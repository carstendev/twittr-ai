package spark

import javax.inject.{Inject, Singleton}

import actors.HttpActor
import akka.actor.{ActorSystem, PoisonPill}
import akka.pattern.ask
import akka.util.Timeout
import configuration.JobConfiguration
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import spark.job.HashtagAnalysisJob.{Start, Stop}
import spark.job.HashtagAnalysisState.Get
import spark.job.{HashtagAnalysisJob, HashtagAnalysisState}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class JobHandler @Inject()(configuration: JobConfiguration, wSClient: WSClient, lifecycle: ApplicationLifecycle) {

  private val Log: Logger = Logger(this.getClass)
  private val system = ActorSystem("ActorSystem")
  private implicit val timeout = Timeout(5.seconds)

  private val hashtagAnalysisState = system.actorOf(HashtagAnalysisState.props, "HashtagAnalysisState")

  private val hashtagAnalysisJobActor = system.actorOf(HashtagAnalysisJob.props(
    configuration.sparkContext,
    system.actorOf(HttpActor.props(wSClient), "HashtagAnalysisHttpActor"),
    hashtagAnalysisState
  ), "HashtagAnalysisJobActor")

  //TODO: The actor is not reachable afterwards
  hashtagAnalysisJobActor ! Start

  def getTrendingHashtags: Future[JsValue] = {
   (hashtagAnalysisState ? Get).mapTo[JsValue]
  }

  lifecycle.addStopHook { () =>

    Future {
      Log.info("Stopping HashtagAnalysisJobActor")
      hashtagAnalysisJobActor ! Stop
      hashtagAnalysisJobActor ! PoisonPill
    }
  }

}
