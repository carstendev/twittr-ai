import actors.TwitterHandler.FetchResponse
import models.Tweet
import org.apache.spark.rdd.RDD

package object actors {

  case object GetLatestModel

  case class Train(corpus: RDD[Tweet])

  case class GetFeatures(fetchResponse: FetchResponse)

  case object Subscribe

  case object Unsubscribe

}
