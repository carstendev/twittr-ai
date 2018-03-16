import com.typesafe.config.ConfigFactory
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceApplicationLoader}
import play.api.{ApplicationLoader, Configuration, Logger}

class ApplicationLoader extends GuiceApplicationLoader() {

  private val Log: Logger = Logger(this.getClass)

  override def builder(context: ApplicationLoader.Context): GuiceApplicationBuilder = {

    Log debug "builder(...)"

    val env = Option(System getProperty("ENV", "")) filter(_.nonEmpty)

    val envConfigFileName = env match {
      case Some(envName) =>
        s"application.${envName.toLowerCase}.conf"
      case None =>
        "application.dev.conf"
    }

    Log info s"Using file: $envConfigFileName"

    val config = Configuration(ConfigFactory load envConfigFileName)

    initialBuilder
      .in(context.environment)
      .loadConfig(config)
      .overrides(overrides(context): _*)
  }
}
