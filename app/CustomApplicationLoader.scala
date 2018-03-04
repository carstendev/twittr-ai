import com.typesafe.config.ConfigFactory
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceApplicationLoader}
import play.api.{ApplicationLoader, Configuration, Logger}

class CustomApplicationLoader extends GuiceApplicationLoader() {

  private val Log: Logger = Logger(this.getClass)

  override def builder(context: ApplicationLoader.Context): GuiceApplicationBuilder = {

    Log debug "builder(...)"

    val envOption1 = Option(System getProperty("ENV", "")) filter(_.nonEmpty)
    val envOption2 = Option(System getProperty("env", "")) filter(_.nonEmpty)

    val envConfigFileName = envOption1 orElse envOption2 match {
      case Some(envName) =>
        s"application.${envName.toLowerCase}.conf"
      case None =>
        "application.dev.conf"
    }

    Log info s"Using environment config file: $envConfigFileName"

    val config = Configuration(ConfigFactory load envConfigFileName)

    initialBuilder
      .in(context.environment)
      .loadConfig(config)
      .overrides(overrides(context): _*)
  }
}
