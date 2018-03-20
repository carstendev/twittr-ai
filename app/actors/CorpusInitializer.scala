package actors

import actors.StatisticsServer.Corpus
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import configuration.AiConfiguration
import helpers.SentimentIdentifier
import models.Tweet
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

class CorpusInitializer(aiConfiguration: AiConfiguration, onlineTrainer: ActorRef, eventServer: ActorRef, statisticsServer: ActorRef) extends Actor with ActorLogging {

  import CorpusInitializer._

  val ssc = new StreamingContext(aiConfiguration.sparkContext, Seconds(1))

  val twitterAuth = Some(aiConfiguration.twitter.getAuthorization)

  val csvFilePath = "data/testdata.manual.2009.06.14.csv"

  var posTweets: RDD[Tweet] = aiConfiguration.sparkContext.emptyRDD[Tweet]

  var negTweets: RDD[Tweet] = aiConfiguration.sparkContext.emptyRDD[Tweet]

  val totalStreamedTweetSize = streamedTweetsSize

  var stop = false


  override def postStop() = {
    ssc.stop(false)
  }

  override def preStart() = {
    self ! InitFromStream
  }

  override def receive = LoggingReceive {

    case Finish =>
      log.debug("Received Finish message")
      log.info("Terminating streaming context...")
      ssc.stop(stopSparkContext = false, stopGracefully = true)
      val msg = s"Send ${posTweets.count} positive and ${negTweets.count} negative tweets toonline trainer"
      log.info(msg)
      eventServer ! msg
      val tweets: RDD[Tweet] = posTweets ++ negTweets
      tweets.cache()
      val trainMessage = Train(tweets)
      onlineTrainer ! trainMessage
      context.stop(self)
      statisticsServer ! Corpus(tweets)
      eventServer ! "Corpus initialization finished"

    case InitFromStream =>
      log.debug("Received InitFromStream message")
      val msg = "Initialize tweets corpus from twitter stream..."
      log.info(msg)
      eventServer ! msg

      val stream = TwitterUtils.createStream(ssc, twitterAuth, filters = SentimentIdentifier.sentimentEmoticons)
        .filter(t => t.getUser.getLang == "en" && !t.isRetweet)
        .map(Tweet(_))

      stream.foreachRDD { rdd =>

        posTweets = add(posTweets, rdd.filter(t => t.sentiment == 1).collect())
        negTweets = add(negTweets, rdd.filter(t => t.sentiment == 0).collect())

        if (stopCollection) {
          stop = true
          self ! Finish
        } else {
          val msg = s"Collected ${posTweets.count} positive tweets and ${negTweets.count} negative tweets of total $totalStreamedTweetSize"
          println(s"*** $msg")
          eventServer ! msg
        }
      }

      ssc.start()

      def add(rdd: RDD[Tweet], tweets: Array[Tweet]) = if (!reachedMax(rdd)) rdd ++ aiConfiguration.sparkContext.makeRDD(tweets) else rdd

      def reachedMax(s: RDD[Tweet]) = s.count >= totalStreamedTweetSize / 2

      def stopCollection = reachedMax(posTweets) && reachedMax(negTweets) && !stop
  }

}

object CorpusInitializer {

  def props(aiConfiguration: AiConfiguration, onlineTrainer: ActorRef, eventServer: ActorRef, statisticsServer: ActorRef) =
    Props(new CorpusInitializer(aiConfiguration, onlineTrainer, eventServer, statisticsServer))

  case object InitFromStream

  case object LoadFromFs

  case object Finish

  val streamedTweetsSize = 500
}
