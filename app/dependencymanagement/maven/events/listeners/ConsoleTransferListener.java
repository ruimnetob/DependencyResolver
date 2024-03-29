package dependencymanagement.maven.events.listeners;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 *
 * Console transfer listener which logs upload / download events into the console.
 * @since 1.0.0
 *
 */
public class ConsoleTransferListener extends AbstractTransferListener {
    private final Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    private Map<TransferResource, Long> downloads = new ConcurrentHashMap<>();

    private int lastLength;

    @Override
    public void transferInitiated( TransferEvent event ) {
        String message = event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading";

        logger.debug( "Transfer initiated. " + message + " " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName() );
    }

    @Override
    public void transferProgressed( TransferEvent event )
    {
        TransferResource resource = event.getResource();
        downloads.put( resource, event.getTransferredBytes() );

        StringBuilder buffer = new StringBuilder( 64 );

        for ( Map.Entry<TransferResource, Long> entry : downloads.entrySet() )
        {
            long total = entry.getKey().getContentLength();
            long complete = entry.getValue();

            buffer.append( getStatus( complete, total ) ).append( "  " );
        }

        int pad = lastLength - buffer.length();
        lastLength = buffer.length();
        pad( buffer, pad );
        buffer.append( '\r' );

        logger.debug( "Transfer progressed: "+ buffer.toString() );
    }

    private String getStatus( long complete, long total )
    {
        if ( total >= 1024 )
        {
            return toKB( complete ) + "/" + toKB( total ) + " KB ";
        }
        else if ( total >= 0 )
        {
            return complete + "/" + total + " B ";
        }
        else if ( complete >= 1024 )
        {
            return toKB( complete ) + " KB ";
        }
        else
        {
            return complete + " B ";
        }
    }

    private void pad( StringBuilder buffer, int spaces )
    {
        String block = "                                        ";
        while ( spaces > 0 )
        {
            int n = Math.min( spaces, block.length() );
            buffer.append( block, 0, n );
            spaces -= n;
        }
    }

    @Override
    public void transferSucceeded( TransferEvent event )
    {
        transferCompleted( event );

        TransferResource resource = event.getResource();
        long contentLength = event.getTransferredBytes();
        if ( contentLength >= 0 )
        {
            String type = ( event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploaded" : "Downloaded" );
            String len = contentLength >= 1024 ? toKB( contentLength ) + " KB" : contentLength + " B";

            String throughput = "";
            long duration = System.currentTimeMillis() - resource.getTransferStartTime();
            if ( duration > 0 )
            {
                DecimalFormat format = new DecimalFormat( "0.0", new DecimalFormatSymbols( Locale.ENGLISH ) );
                double kbPerSec = ( contentLength / 1024.0 ) / ( duration / 1000.0 );
                throughput = " at " + format.format( kbPerSec ) + " KB/sec";
            }
            logger.debug( "Transfer Succeeded: " + type + " of " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName() + "( "+ len + throughput + " )");
        }
    }

    @Override
    public void transferFailed( TransferEvent event )
    {
        transferCompleted( event );

        logger.debug( "Transfer Failed with exception: "+ event.getException().getClass() + " and message: " + event.getException().getMessage()  );
    }

    private void transferCompleted( TransferEvent event )
    {
        downloads.remove( event.getResource() );

        StringBuilder buffer = new StringBuilder( 64 );
        pad( buffer, lastLength );
        buffer.append( '\r' );
        logger.debug(buffer.toString());
    }

    @Override
    public void transferCorrupted( TransferEvent event )
    {
        logger.debug( "Transfer Corrupted. Exception: " + event.getException().getClass() + " with message: " + event.getException().getMessage() );
    }

    protected long toKB( long bytes )
    {
        return ( bytes + 1023 ) / 1024;
    }
}
