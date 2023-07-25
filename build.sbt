version := "0.1.0-SNAPSHOT"
scalaVersion := "2.12.8"
name := "fraud-detector-service"

scalaVersion := "2.13.11"
val akkaVersion = "2.8.2"
val akkaHttpVersion = "10.5.2"

libraryDependencies ++= Seq(
  // akka streams
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  // akka http
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,




)
