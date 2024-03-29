package controllers

import java.net.URL
import dependencymanagement.loading.{DependencyResolverClassLoader, DependencyResolverClassLoaderCache}
import forms.DependencySearchForm
import javax.inject.Inject
import play.api.mvc._
import models.Dependency
import play.api.Configuration

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
class DependencyResolverController @Inject()(playConfiguration:Configuration, cc: ControllerComponents) extends AbstractController(cc) with play.api.i18n.I18nSupport {
  private val logger = play.api.Logger(this.getClass)

  /**
    * Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index(DependencySearchForm.dependencySearchForm,playConfiguration))
  }

  /**
    * Called by Javascript Router when Form is Submitted.
    * @return
    */
  def handleFormSubmit() = Action { implicit request: Request[AnyContent] =>
    DependencySearchForm.dependencySearchForm.bindFromRequest().fold(
      formWithErrors =>  {
        // binding failure, you retrieve the form containing errors:
        BadRequest(views.html.searchForm(formWithErrors))
      },
      formData => {
        // binding success, you get the actual value.
        val dependency:Dependency=new Dependency(formData.groupId, formData.artifactId,formData.version)
        val classLoader:DependencyResolverClassLoader = DependencyResolverClassLoaderCache.getInstance().getClassLoader(dependency.toString)
        val dependencyClassPathUrls:Array[URL]=classLoader.getURLs

        val html=views.html.resultsTable.render(dependency,dependencyClassPathUrls)
        Ok(html)
      }
    )
  }

}


