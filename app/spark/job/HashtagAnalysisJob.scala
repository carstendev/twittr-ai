package spark.job

import actors.HttpActor.PostMessage
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import configuration.JobConfiguration
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.twitter.TwitterUtils
import play.api.libs.json.{Json, _}
import spark.job.HashtagAnalysisJob._
import twitter4j.Status
import org.apache.spark.sql.functions._


/**
  * Spark job that figures out the the most hot/trending hashtags and writes them to kafka using topic "TrendingHashtagAnalysis".
  *
  * @param batchDuration the time interval at which streaming data will be divided into batches
  * @param window        the time window to consider tweet's for
  */
case class HashtagAnalysisJob
(
  config: JobConfiguration,
  httpActor: ActorRef,
  batchDuration: Duration = Seconds(5),
  window: Duration = Minutes(60)
) extends Actor with ActorLogging {

  // Wrap the context in a streaming one, passing along the batch duration
  private val streamingContext = new StreamingContext(config.sparkContext, batchDuration)

 // Creating a stream from Twitter (see the README to learn how to
  // provide a configuration to make this work - you'll basically
  // need a set of Twitter API keys)
  private val tweets: DStream[Tweet] = TwitterUtils.createStream(streamingContext, None)

  // split tweet into words, discard retweets and filter by lang
  private val words = tweets
    .filter(!_.isRetweeted)
    .filter(tweet => LanguageSet.contains(tweet.getLang))
    .flatMap(_.getText.split(" "))

  // filter the words to get only hashtags, then map each hashtag to be a tuple of (hashtag,1)
  private val hashtags = words
    .filter(_.startsWith("#"))
    .map(hashtag => (hashtag, 1L))

  // sum hashtag counts by key and keep result over a sliding window
  private val hashtagTotals = hashtags.reduceByKeyAndWindow(_ + _, window)

  hashtagTotals.foreachRDD { rdd =>

    val sparkSession = SparkSession.builder().getOrCreate()

    val rowRdd = rdd.map(Row.fromTuple(_))

    val df = sparkSession.createDataFrame(rowRdd, rowSchema)

    df.createOrReplaceTempView("hashtags")

    val topHashtagsDf = selectTopTenHashtags(sparkSession)

    val hashtagsAndCounts = topHashtagsDf
      .select("hashtag", "count")
      .collect()
      .map(hashtagAndCount => (hashtagAndCount.getString(0), hashtagAndCount.getLong(1)))

    val asJson = convertToJson(hashtagsAndCounts)

    log.info(asJson.toString)

    val postMsg = PostMessage(asJson, "http://localhost:9001/hashtags")

    httpActor ! postMsg

    topHashtagsDf
      .select(struct("hashtag", "count").as("hashtag_and_count"))
      .agg(collect_set("hashtag_and_count")).as("value")
      .toJSON
      .write
      .format("kafka")
      .option("kafka.bootstrap.servers", config.kafkaConfig.bootstrapServers)
      .option("topic", KafkaOutputTopic)
      .save
  }

  override def receive: Receive = {

    case Start =>
      // Now that the streaming is defined, start it
      streamingContext.start()
      // Let's await the stream to end - forever
      streamingContext.awaitTermination()

    case Stop(gracefully) =>
      log.info(s"HashtagAnalysisJob was stopped, gracefully: $gracefully")
      // Stop the streaming but keep the spark context running since it could be used by other jobs
      streamingContext.stop(stopSparkContext = false, stopGracefully = gracefully)

  }

}

object HashtagAnalysisJob {

  /**
    * Message to start the job
    */
  final case object Start

  /**
    * Message to stop the job
    */
  final case class Stop(gracefully: Boolean = true)

  private type Tweet = Status
  private type HashtagAndCount = (String, Long)
  private type HashtagsAndCounts = Array[HashtagAndCount]

  // English, German, French, Spanish, Portuguese and Dutch
  private val LanguageSet = Set("en", "de", "fr", "es", "pt", "nl")

  private val KafkaOutputTopic = "TrendingHashtagAnalysis"

  private implicit object JsonWriteFormat extends Writes[HashtagAndCount] {

    override def writes(hashtagAndCount: HashtagAndCount): JsValue = {
      JsObject(Seq(
        "hashtag" -> JsString(hashtagAndCount._1),
        "count" -> JsNumber(hashtagAndCount._2)
      ))
    }
  }

  private val rowSchema = StructType(Array(
    StructField("hashtag", DataTypes.StringType, nullable = false),
    StructField("count", DataTypes.LongType, nullable = false)
  ))

  private[job] def selectTopTenHashtags(sparkSession: SparkSession) = {
    sparkSession.sql("select hashtag, count from hashtags order by count desc limit 10")
  }

  private[job] def convertToJson(hashtagsAndCounts: HashtagsAndCounts): JsValue = Json.toJson(hashtagsAndCounts)

  def props(config: JobConfiguration,
            httpActor: ActorRef,
            batchDuration: Duration = Seconds(5),
            window: Duration = Minutes(60)
           ): Props = {

    Props(classOf[HashtagAnalysisJob], config, httpActor, batchDuration, window)
  }

}
