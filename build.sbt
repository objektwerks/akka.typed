name := "akka.typed"
organization := "objektwerks"
version := "0.1-SNAPSHOT"
scalaVersion := "2.13.11"
libraryDependencies ++= {
  val akkaVersion = "2.6.20" // Don't upgrade due to BUSL 1.1!
  val akkaHttpVersion = "10.2.10" // Don't upgrade due to BUSL 1.1!
  val json4sVersion = "4.0.6"
  Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "org.json4s" %% "json4s-jackson" % json4sVersion,
    "org.json4s" %% "json4s-native" % json4sVersion,
    "org.iq80.leveldb" % "leveldb" % "0.12",
    "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
    "ch.qos.logback" % "logback-classic" % "1.4.7",
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test,
    "org.scalatest" %% "scalatest" % "3.2.15" % Test
  )
}
