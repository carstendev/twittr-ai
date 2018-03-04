
name := """twittr-ai"""
organization := "com.twittr-ai"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.11"


libraryDependencies += "org.apache.spark" %% "spark-streaming" % "2.3.0" % "provided"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.3.0"
libraryDependencies += "org.apache.bahir" %% "spark-streaming-twitter" % "2.2.0"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.11"


libraryDependencies += guice
libraryDependencies += ws
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.twittr-ai.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.twittr-ai.binders._"
