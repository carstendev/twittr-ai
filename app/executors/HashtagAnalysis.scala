package executors

import executors.HashtagAnalysis.{processRDD, sumHashtagCount}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{LongType, StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row, SQLContext}
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import twitter4j.Status
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.ws.WSRequest

case class HashtagAnalysis(sparkContext: SparkContext) {

  type Tweet = Status
  type TweetText = String
  type Sentence = Seq[String]

  // Now let's wrap the context in a streaming one, passing along the window size
  private val streamingContext = new StreamingContext(sparkContext, Seconds(5))

  // Creating a stream from Twitter (see the README to learn how to
  // provide a configuration to make this work - you'll basically
  // need a set of Twitter API keys)
  val tweets: DStream[Tweet] = TwitterUtils.createStream(streamingContext, None)

  // split tweet into words
  val words = tweets.flatMap(_.getText.split(" "))

  // filter the words to get only hashtags, then map each hashtag to be a pair of (hashtag,1)
  val hashtags = words.filter(_.startsWith("#")).map(hashtag => (hashtag, 1L))

  val hashtagTotals = hashtags.updateStateByKey(sumHashtagCount)

  hashtagTotals.foreachRDD(processRDD(_))

  // Now that the streaming is defined, start it
  streamingContext.start()

  // Let's await the stream to end - forever
  streamingContext.awaitTermination()

}

object HashtagAnalysis {

  private type HastagsAndCounts = Array[(String, Long)]

  private implicit object JsonWriteFormat extends Writes[HastagsAndCounts] {

    override def writes(hastagsAndCounts: HastagsAndCounts): JsValue = {
      JsObject(
        Seq(
          "label" -> JsArray(hastagsAndCounts.map(e => JsString(e._1))),
          "count" -> JsArray(hastagsAndCounts.map(e => JsNumber(e._2)))
        )
      )
    }
  }

  private val rowSchema = StructType(Array(
    StructField("hashtag", StringType, nullable = false),
    StructField("count", LongType, nullable = false)
  ))

  private def sumHashtagCount(newHashtags: Seq[Long], totalSum: Option[Long]) = {
    Option(newHashtags.sum + totalSum.getOrElse(0L))
  }

  //TODO: this method does too many things
  private def processRDD(rdd: RDD[(String, Long)]) = {
    val sqlContext = new SQLContext(rdd.context)

    val rowRdd = rdd.map(e => Row(Array(e._1, e._2)))

    val hashtagDf = sqlContext.createDataFrame(rowRdd, rowSchema)

    hashtagDf.createOrReplaceTempView("hashtags")

    val topHashtagsDf = sqlContext.sql("select hashtag, count from hashtags order by count desc limit 10")

    topHashtagsDf.show(10)

    // TODO: set target through configuration
    postDataFrame(topHashtagsDf, "http://localhost:5001/updateData")

  }

  private def postDataFrame(df: DataFrame, wSRequest: WSRequest): Unit = {

    val hashtagsAndCounts = df
      .select("hashtag", "count")
      .collect()
      .map(e => (e.getString(0), e.getLong(1)))

    val asJson = convertToJson(hashtagsAndCounts)

    // TODO: maybe use an akka actor to do the ws calls?

  }

  private def convertToJson(hashtagsAndCounts: HastagsAndCounts): JsValue = Json.toJson(hashtagsAndCounts)



}
