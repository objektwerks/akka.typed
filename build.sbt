name := "akka.typed"
organization := "objektwerks"
version := "0.1-SNAPSHOT"
scalaVersion := "2.13.7"
libraryDependencies ++= {
  val akkaVersion = "2.6.17"
  val akkaHttpVersion = "10.2.7"
  val json4sVersion = "4.0.3"
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
    "ch.qos.logback" % "logback-classic" % "1.2.6",
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test,
    "org.scalatest" %% "scalatest" % "3.2.10" % Test
  )
}
