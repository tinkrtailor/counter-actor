lazy val akkaHttpVersion = "10.2.1"
lazy val akkaVersion = "2.6.9"
lazy val slickVersion = "3.3.2"
lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "com.counter",
      scalaVersion := "2.13.3"
    )),
  scalacOptions in Compile ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
  javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
  name := "Counter example",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3",

    "com.lightbend.akka" %% "akka-persistence-jdbc" % "4.0.0",
    "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
    "org.postgresql" % "postgresql" % "42.2.16",
    "com.typesafe.slick" %% "slick" % slickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
    "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,

    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
    "org.scalatest" %% "scalatest" % "3.1.4" % Test
  )
)
