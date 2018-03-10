package spark.job

import actors.HttpActor.PostMessage
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.apache.spark.SparkContext
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Row, SQLContext}
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.twitter.TwitterUtils
import play.api.libs.json.{Json, _}
import spark.job.HashtagAnalysisJob._
import twitter4j.Status

/**
  * Spark job that figures out the the most hot/trending hashtags.
  *
  * @param batchDuration the time interval at which streaming data will be divided into batches
  * @param window        the time window to consider tweet's for
  */
case class HashtagAnalysisJob
(
  sparkContext: SparkContext,
  httpActor: ActorRef,
  batchDuration: Duration = Seconds(5),
  window: Duration = Minutes(10)
) extends Actor with ActorLogging {

  // Wrap the context in a streaming one, passing along the window size
  private val streamingContext = new StreamingContext(sparkContext, batchDuration)

  // Creating a stream from Twitter (see the README to learn how to
  // provide a configuration to make this work - you'll basically
  // need a set of Twitter API keys)
  private val tweets: DStream[Tweet] = TwitterUtils.createStream(streamingContext, None)

  // split tweet into words
  private val words = tweets.flatMap(_.getText.split(" "))

  // filter the words to get only hashtags, then map each hashtag to be a pair of (hashtag,1)
  private val hashtags = words.filter(_.startsWith("#")).map(hashtag => (hashtag, 1L))

  // sum hashtag counts by key and keep result over a sliding window
  private val hashtagTotals = hashtags.reduceByKeyAndWindow(_ + _, window)

  hashtagTotals.foreachRDD { rdd =>

    val sqlContext = new SQLContext(rdd.context) //TODO: use builder

    val rowRdd = rdd.map(Row.fromTuple(_))

    val df = sqlContext.createDataFrame(rowRdd, rowSchema)

    df.createOrReplaceTempView("hashtags")

    val topHashtagsDf = selectTopTenHashtags(sqlContext)

    log.info(topHashtagsDf.toString())

    val hashtagsAndCounts = topHashtagsDf
      .select("hashtag", "count")
      .collect()
      .map(e => (e.getString(0), e.getLong(1)))

    hashtagsAndCounts.foreach(e => log.info(s"hashtag: ${e._1} count: ${e._2}"))

    val postMsg = PostMessage(convertToJson(hashtagsAndCounts), "http://localhost:5001/updateData")

    httpActor ! postMsg
  }

  override def receive: Receive = {

    case Start =>
      // Now that the streaming is defined, start it
      streamingContext.start()
      // Let's await the stream to end - forever
      streamingContext.awaitTermination()

  }

}

object HashtagAnalysisJob {

  final case object Start

  private type Tweet = Status
  private type HashtagAndCount = (String, Long)
  private type HashtagsAndCounts = Array[HashtagAndCount]

  private implicit object JsonWriteFormat extends Writes[HashtagsAndCounts] {

    override def writes(hashtagsAndCounts: HashtagsAndCounts): JsValue = {
      JsObject(
        Seq(
          "label" -> JsArray(hashtagsAndCounts.map(e => JsString(e._1))),
          "count" -> JsArray(hashtagsAndCounts.map(e => JsNumber(e._2)))
        )
      )
    }
  }

  private val rowSchema = StructType(Array(
    StructField("hashtag", DataTypes.StringType, nullable = false),
    StructField("count", DataTypes.LongType, nullable = false)
  ))

  private[job] def selectTopTenHashtags(sqlContext: SQLContext) = {
    sqlContext.sql("select hashtag, count from hashtags order by count desc limit 10")
  }

  private[job] def convertToJson(hashtagsAndCounts: HashtagsAndCounts): JsValue = Json.toJson(hashtagsAndCounts)

  def props(sparkContext: SparkContext,
            httpActor: ActorRef,
            batchDuration: Duration = Seconds(5),
            window: Duration = Minutes(10)
           ): Props = {

    Props(classOf[HashtagAnalysisJob], sparkContext, httpActor, batchDuration, window)
  }

}
