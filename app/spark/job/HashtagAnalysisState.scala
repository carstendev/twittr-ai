package spark.job

import akka.actor.{Actor, Props}
import play.api.libs.json.{JsNull, JsValue}
import spark.job.HashtagAnalysisState.{Get, Put}

/**
  * Handles state of hashtag analysis
  */
class HashtagAnalysisState extends Actor {

  var currentState: JsValue = JsNull

  override def receive: Receive = {

    case Get =>
      sender ! currentState

    case Put(state) =>
      currentState = state
  }
}

object HashtagAnalysisState {

  final object Get

  final case class Put(state: JsValue)

  def props: Props = Props(classOf[HashtagAnalysisState])
}
