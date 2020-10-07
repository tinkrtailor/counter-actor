lazy val akkaHttpVersion = "10.2.1"
lazy val akkaVersion = "2.6.9"

lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "com.counter",
      scalaVersion := "2.13.3"
    )),
  name := "Counter example",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3",

    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
    "org.scalatest" %% "scalatest" % "3.1.4" % Test
  )
)
