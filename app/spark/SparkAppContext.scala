package spark

import javax.inject.Singleton

import org.apache.spark.{SparkConf, SparkContext}

/**
  * SparkConf that is used by the application.
  */
@Singleton
final class SparkAppContext {

  private val sparkConfiguration = new SparkConf()
    .setAppName("twittr-ai")
    .setMaster(sys.env.getOrElse("spark.master", "local[*]"))

  // Create the Spark Context using the configuration we just created
  val sparkContext = new SparkContext(sparkConfiguration)
}
