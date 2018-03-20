package actors

import actors.OnlineTrainer.OnlineTrainerModel
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import classifiers.Predictor
import configuration.AiConfiguration

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Director(aiConfiguration: AiConfiguration, eventServer: ActorRef, statisticsServer: ActorRef) extends Actor with ActorLogging {

  import Director._

  val twitterHandler = context.actorOf(TwitterHandler.props(aiConfiguration.sparkContext), "twitter-handler")

  val onlineTrainer = context.actorOf(OnlineTrainer.props(aiConfiguration, self), "online-trainer")

  val predictor = new Predictor(aiConfiguration.sparkContext)

  val classifier = context.actorOf(Classifier.props(aiConfiguration.sparkContext, twitterHandler, onlineTrainer, predictor), "classifier")

  context.actorOf(CorpusInitializer.props(aiConfiguration, onlineTrainer, eventServer, statisticsServer), "corpus-initializer")

  override def receive = LoggingReceive {

    case GetClassifier => sender ! classifier

    case OnlineTrainingFinished =>
      context.system.scheduler.schedule(0.seconds, 5.seconds) {
        onlineTrainer ! GetLatestModel
      }

    case m: OnlineTrainerModel => statisticsServer ! m

    case undefined => log.info(s"Unexpected message $undefined")
  }

}

object Director {

  def props(aiConfiguration: AiConfiguration, eventServer: ActorRef, statisticsServer: ActorRef) = Props(new Director(aiConfiguration, eventServer, statisticsServer))

  case object GetClassifier

  case object OnlineTrainingFinished

  case object BatchTrainingFinished

}
