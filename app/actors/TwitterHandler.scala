package actors

import actors.TwitterHandler.{Fetch, FetchResponse}
import akka.actor.{Actor, Props}
import akka.event.LoggingReceive
import configuration.OAuthKeys
import helpers.TwitterHelper
import org.apache.spark.SparkContext
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global


trait TwitterHandlerProxy extends Actor

class TwitterHandler(sparkContext: SparkContext) extends TwitterHandlerProxy {

  val log = Logger(this.getClass)

  override def receive = LoggingReceive {

    case Fetch(keyword, oAuthKeys) =>
      log.debug(s"Received Fetch message with keyword=$keyword from $sender")
      val tweets = TwitterHelper.fetch(keyword, oAuthKeys)
      sender ! FetchResponse(keyword, tweets)

    case undefined => log.warn(s"Unexpected message $undefined")

  }

}

object TwitterHandler {

  def props(sparkContext: SparkContext) = Props(new TwitterHandler(sparkContext))

  case class Fetch(keyword: String, keys: OAuthKeys)

  case class FetchResponse(keyword: String, tweets: Seq[String])

}

