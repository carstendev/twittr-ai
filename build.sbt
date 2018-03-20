
name := """twittr-ai"""
organization := "com.twittr-ai"

version := "1.0-SNAPSHOT"

val sparkVersion = "2.2.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.11"

//TODO: Important: provided must be used if the jobs are to run on a spark cluster and not in standalone mode
libraryDependencies += "org.apache.spark" %% "spark-streaming" % sparkVersion
libraryDependencies += "org.apache.spark" %% "spark-mllib" % sparkVersion
libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion
libraryDependencies += "org.apache.bahir" %% "spark-streaming-twitter" % sparkVersion
libraryDependencies += "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion
libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.11"
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "0.10.1.1"
libraryDependencies += "org.apache.kafka" % "kafka_2.11" % "0.10.1.1"

libraryDependencies += "org.jblas" % "jblas" % "1.2.4"
libraryDependencies += "org.scalanlp" %% "breeze" % "0.11.2"
libraryDependencies += "org.scalanlp" % "chalk" % "1.3.0" intransitive()
libraryDependencies += "org.scalanlp" % "nak" % "1.2.0" intransitive()

libraryDependencies += guice
libraryDependencies += ws
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test

libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-log4j12")) }

// Must be overridden to avoid conflict
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.5"


// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.twittr-ai.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.twittr-ai.binders._"
