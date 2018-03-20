package configuration

import play.api.Configuration
import play.api.libs.oauth.{ConsumerKey, RequestToken}

case class OAuthKeys(consumerKey: ConsumerKey, requestToken: RequestToken)

object OAuthKeys {

  def apply(configuration: Configuration): OAuthKeys = {
    OAuthKeys(
      ConsumerKey(
        configuration.get[String]("twitter4j.oauth.consumerKey"),
        configuration.get[String]("twitter4j.oauth.consumerSecret")
      ),
      RequestToken(
        configuration.get[String]("twitter4j.oauth.accessToken"),
        configuration.get[String]("twitter4j.oauth.accessTokenSecret")
      )
    )
  }
}
