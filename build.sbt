name := "akka.typed"
organization := "objektwerks"
version := "0.1-SNAPSHOT"
scalaVersion := "2.12.9"
libraryDependencies ++= {
  val akkaVersion = "2.5.24"
  Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
    "org.scalatest" %% "scalatest" % "3.0.8" % Test
  )
}