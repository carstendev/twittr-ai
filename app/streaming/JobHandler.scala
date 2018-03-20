package streaming

import javax.inject.{Inject, Singleton}

import actors.HttpActor
import akka.actor.ActorSystem
import akka.util.Timeout
import configuration.AiConfiguration
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.WSClient
import streaming.jobs.HashtagAnalysisJob
import streaming.jobs.HashtagAnalysisJob.{Start, Stop}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class JobHandler @Inject()(configuration: AiConfiguration, wSClient: WSClient, lifecycle: ApplicationLifecycle) {

  private val Log: Logger = Logger(this.getClass)
  private val system = ActorSystem("ActorSystem")
  private implicit val timeout = Timeout(5.seconds)

  private val hashtagAnalysisJobActor = system.actorOf(HashtagAnalysisJob.props(
    configuration,
    system.actorOf(HttpActor.props(wSClient), "HashtagAnalysisHttpActor")
  ), "HashtagAnalysisJobActor")


  def stopHashtagAnalysis(): Unit = {
    hashtagAnalysisJobActor ! Stop
  }

  def startHashtagAnalysis(): Unit = {
    hashtagAnalysisJobActor ! Start
  }

  lifecycle.addStopHook { () =>

    Future {
      Log.info("Stopping HashtagAnalysisJobActor")
      hashtagAnalysisJobActor ! Stop
    }
  }

}
