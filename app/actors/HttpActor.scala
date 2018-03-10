package actors

import scala.concurrent.ExecutionContext.Implicits.global
import actors.HttpActor.PostMessage
import akka.actor.{Actor, ActorLogging, Props}
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient

/**
  * Actor to do basic http requests.
  */
case class HttpActor(wsClient: WSClient) extends Actor with ActorLogging {


  override def receive: Receive = {

    case PostMessage(msg, targetUrl) =>
      log.debug(s"Sending msg $msg to target $targetUrl")
      wsClient.url(targetUrl).post(msg).onFailure {
        case error => log.error(error.getMessage, error)
      }

  }
}

object HttpActor {

  final case class PostMessage(msg: JsValue, targetUrl: String)

  def props(wsClient: WSClient): Props = {
    Props(classOf[HttpActor], wsClient)
  }
}
