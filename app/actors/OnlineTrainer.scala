package actors

import actors.Director.OnlineTrainingFinished
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import configuration.AiConfiguration
import features.Transformers.default._
import features.{Features, TfIdf}
import helpers.SentimentIdentifier
import models.Tweet
import org.apache.spark.mllib.classification.{LogisticRegressionModel, StreamingLogisticRegressionWithSGD}
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import twitter4j.auth.Authorization

trait OnlineTrainerProxy extends Actor

class OnlineTrainer(aiConfiguration: AiConfiguration, director: ActorRef) extends Actor with ActorLogging with OnlineTrainerProxy {

  import OnlineTrainer._

  val ssc = new StreamingContext(aiConfiguration.sparkContext, Seconds(1))

  val twitterAuth: Option[Authorization] = Some(aiConfiguration.twitter.getAuthorization)

  val sqlContext = new SQLContext(aiConfiguration.sparkContext)

  var logisticRegression: Option[StreamingLogisticRegressionWithSGD] = None

  var maybeTfIdf: Option[TfIdf] = None

  override def postStop() = ssc.stop(false)

  override def receive = LoggingReceive {

    case Train(corpus) =>
      log.debug("Received Train message with tweets corpus")
      val tfIdf = TfIdf(corpus)
      maybeTfIdf = Some(tfIdf)

      logisticRegression = Some(new StreamingLogisticRegressionWithSGD()
        .setNumIterations(200)
        .setInitialWeights(Vectors.zeros(Features.coefficients))
        .setStepSize(1.0))

      log.info("Start twitter stream for online training")
      val stream = TwitterUtils.createStream(ssc, twitterAuth, filters = SentimentIdentifier.sentimentEmoticons)
        .filter(t => t.getUser.getLang == "en" && !t.isRetweet)
        .map(Tweet(_))
        .map(tweet => tweet.toLabeledPoint { _ => tfIdf.tf(tweet.tokens) })

      logisticRegression.map(lr => lr.trainOn(stream))
      ssc.start()
      director ! OnlineTrainingFinished

    case GetFeatures(fetchResponse) =>
      log.debug("Received GetFeatures message")
      val features = maybeTfIdf map { tfIdf =>
        val rdd: RDD[String] = aiConfiguration.sparkContext.parallelize(fetchResponse.tweets)
        rdd.cache()
        rdd map { t => (t, tfIdf.tfIdf(Tweet(t).tokens)) }
      }
      sender ! OnlineFeatures(features)

    case GetLatestModel =>
      log.debug("Received GetLatestModel message")
      val maybeModel = logisticRegression.map(_.latestModel())
      sender ! OnlineTrainerModel(maybeModel)

  }

}

object OnlineTrainer {

  def props(aiConfiguration: AiConfiguration, director: ActorRef) = Props(new OnlineTrainer(aiConfiguration, director: ActorRef))

  case class OnlineTrainerModel(model: Option[LogisticRegressionModel])

  case class OnlineFeatures(features: Option[RDD[(String, Vector)]])

}
