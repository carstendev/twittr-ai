package features

import models.Tweet
import org.apache.spark.mllib.feature.{HashingTF, IDF}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.rdd.RDD
import features.Transformers.default._

case class TfIdf (corpus: RDD[Tweet]) extends Serializable {

  val tf = new HashingTF(Features.coefficients)

  val idf = new IDF().fit(tf.transform(corpus.map(_.tokens)))

  def tf(text: Seq[String]): Vector = tf.transform(text)

  def tfIdf(text: Seq[String]): Vector = idf.transform(tf.transform(text))

}