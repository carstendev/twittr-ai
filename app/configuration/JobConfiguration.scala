package configuration

import javax.inject.{Inject, Singleton}

import configuration.JobConfiguration._
import org.apache.spark.{SparkConf, SparkContext}
import play.api.Configuration
import twitter4j.Twitter
import helpers.TwitterHelper


@Singleton
case class JobConfiguration(sparkContext: SparkContext, kafkaConfig: KafkaConfig, twitter: Twitter) {

  @Inject
  def this(configuration: Configuration) {
    this(
      resolveSparkContext(configuration),
      resolveKafkaConfig(configuration),
      resolveTwitterImpl(configuration)
    )
  }
}

object JobConfiguration {

  def resolveSparkContext(configuration: Configuration): SparkContext = {
    val appName = configuration.get[String]("spark.app.name")
    val master = configuration.get[String]("spark.master")
    val logLevel = configuration.get[String]("spark.loglevel")
    val sparkContext = new SparkContext(new SparkConf().setAppName(appName).setMaster(sys.env.getOrElse(master, "local[*]")))
    sparkContext.setLogLevel(logLevel)
    sparkContext
  }

  def resolveKafkaConfig(configuration: Configuration): KafkaConfig = {
    KafkaConfig(configuration.get[String]("kafka.bootstrap.servers"))
  }

  def resolveTwitterImpl(configuration: Configuration): Twitter = {
    TwitterHelper.twitter(OAuthKeys(configuration))
  }

}
