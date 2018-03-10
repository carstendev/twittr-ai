package spark

import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext.Implicits.global
import actors.HttpActor
import akka.actor.{ActorSystem, PoisonPill}
import configuration.JobConfiguration
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.WSClient
import spark.job.HashtagAnalysisJob
import spark.job.HashtagAnalysisJob.Start

import scala.concurrent.Future

@Singleton
class JobHandler @Inject()(configuration: JobConfiguration, wSClient: WSClient, lifecycle: ApplicationLifecycle) {

  private val Log: Logger = Logger(this.getClass)
  private val system = ActorSystem("ActorSystem")

  private val hashtagAnalysisJobActor = system.actorOf(HashtagAnalysisJob.props(
    configuration.sparkContext,
    system.actorOf(HttpActor.props(wSClient), "HashtagAnalysisHttpActor")
  ), "HashtagAnalysisJobActor")

  hashtagAnalysisJobActor ! Start

  lifecycle.addStopHook { () =>

    Future {
      Log.info("Stopping HashtagAnalysisJobActor")
      hashtagAnalysisJobActor ! PoisonPill
    }
  }

}
