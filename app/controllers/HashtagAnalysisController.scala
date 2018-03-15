package controllers

import javax.inject._

import play.api.mvc._
import spark.JobHandler

import scala.concurrent.ExecutionContext.Implicits.global
/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HashtagAnalysisController @Inject()(cc: ControllerComponents, jobHandler: JobHandler) extends AbstractController(cc) {

  def getTrendingHashtags = Action.async {

    jobHandler.getTrendingHashtags().map(Ok(_))
  }
}
