package spark

import javax.inject.{Inject, Singleton}

import actors.HttpActor
import akka.actor.ActorSystem
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.WSClient
import spark.job.HashtagAnalysisJob

@Singleton
class JobHandler @Inject()(appContext: SparkAppContext, wSClient: WSClient, lifecycle: ApplicationLifecycle) {


  private val Log: Logger = Logger(this.getClass)
  private val system = ActorSystem("ActorSystem")


  HashtagAnalysisJob(
    appContext.sparkContext,
    system.actorOf(HttpActor.props(wSClient), "HashtagAnalysisHttpActor")
  )

}
