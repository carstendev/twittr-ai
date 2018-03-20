package helpers

import configuration.OAuthKeys
import play.api.Logger
import twitter4j.conf.ConfigurationBuilder
import twitter4j.{Query, Twitter, TwitterFactory}

import scala.collection.JavaConverters._

object TwitterHelper {

  private val Log = Logger(this.getClass)

  def fetch(keyword: String, oAuthKeys: OAuthKeys, count: Int = 100): Seq[String] = {
    Log.info(s"Start fetching tweets filtered by keyword=$keyword")
    val query = new Query(s"$keyword -filter:retweets").lang("en")
    val result = twitter(oAuthKeys).search(query)
    result.getTweets.asScala.take(count).map(_.getText)
  }

  def twitter(oAuthKeys: OAuthKeys): Twitter = {
    val config = new ConfigurationBuilder()
      .setDebugEnabled(true)
      .setOAuthConsumerKey(oAuthKeys.consumerKey.key)
      .setOAuthConsumerSecret(oAuthKeys.consumerKey.secret)
      .setOAuthAccessToken(oAuthKeys.requestToken.token)
      .setOAuthAccessTokenSecret(oAuthKeys.requestToken.secret)
      .build()
    new TwitterFactory(config).getInstance()
  }

}
