package configuration

import javax.inject.{Inject, Singleton}

import configuration.JobConfiguration._
import org.apache.spark.{SparkConf, SparkContext}
import play.api.Configuration


@Singleton
case class JobConfiguration(sparkContext: SparkContext, kafkaConfig: KafkaConfig) {

  @Inject
  def this(configuration: Configuration) {
    this(resolveSparkContext(configuration), resolveKafkaConfig(configuration))
    loadTwitter4jOauth(configuration)
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

  def loadTwitter4jOauth(configuration: Configuration): Unit = {
    System.setProperty("twitter4j.oauth.consumerKey", configuration.get[String]("twitter4j.oauth.consumerKey"))
    System.setProperty("twitter4j.oauth.consumerSecret", configuration.get[String]("twitter4j.oauth.consumerSecret"))
    System.setProperty("twitter4j.oauth.accessToken", configuration.get[String]("twitter4j.oauth.accessToken"))
    System.setProperty("twitter4j.oauth.accessTokenSecret", configuration.get[String]("twitter4j.oauth.accessTokenSecret"))
  }

}
