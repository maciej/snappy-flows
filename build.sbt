organization := "me.maciejb.snappyflows"
name := "snappy-flows"
version := "1.0"
scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "com.typesafe.akka" %% "akka-actor" % "2.4.1",
  "com.typesafe.akka" %% "akka-stream-experimental" % "2.0-M2",
  "org.xerial.snappy" % "snappy-java" % "1.1.2"
)
