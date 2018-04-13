package controllers

import javax.inject._
import play.api.mvc._
import streaming.JobHandler

import scala.concurrent.{ExecutionContext, Future}

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HashtagAnalysisController @Inject()(cc: ControllerComponents, jobHandler: JobHandler, implicit val ec: ExecutionContext) extends AbstractController(cc) {

  /**
    * Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def start() = Action.async {
    Future {
      jobHandler.startHashtagAnalysis()
      Ok("Started")
    }
  }

  def stop() = Action.async {
    Future {
      jobHandler.stopHashtagAnalysis()
      Ok("Stopped")
    }
  }
}
