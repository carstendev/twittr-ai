package models

import features.Transformer
import org.apache.spark.mllib.feature.HashingTF
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.regression.LabeledPoint
import play.api.libs.json.Json
import twitter4j.Status
import helpers.SentimentIdentifier._

case class LabeledTweet(tweet: String, sentiment: String)

object LabeledTweet {

  implicit val formats = Json.format[LabeledTweet]

}

case class Tweet(text: String, sentiment: Double) extends Serializable {

  def tokens(implicit transformer: Transformer): Seq[String] = transformer.transform(text)

  def toLabeledPoint(implicit hashingTF: HashingTF, transformer: Transformer): LabeledPoint = LabeledPoint(sentiment, hashingTF.transform(tokens))

  def toLabeledPoint(f: String => Vector): LabeledPoint = LabeledPoint(sentiment, f(text))

}


object Tweet {

  val Positive = 1.0
  val Negative = 0.0

  def apply(status: Status): Tweet = Tweet(
    status.getText,
    if (isPositive(status.getText)) Positive else Negative
  )

  def apply(tweetText: String): Tweet = Tweet(
    tweetText,
    if (isPositive(tweetText)) Positive else Negative
  )

}