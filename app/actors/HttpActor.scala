package actors


import actors.HttpActor.PostMessage
import akka.actor.{Actor, ActorLogging, Props}
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient

/**
  * Actor to do basic http requests.
  */
case class HttpActor(wsClient: WSClient) extends Actor with ActorLogging {


  override def receive: Receive = {

    //TODO: Logging and retry?
    case PostMessage(msg, targetUrl) =>
      wsClient.url(targetUrl).post(msg)


  }
}

object HttpActor {

  final case class PostMessage(msg: JsValue, targetUrl: String)

  def props(wsClient: WSClient): Props = {
    Props(classOf[HttpActor], wsClient)
  }
}
