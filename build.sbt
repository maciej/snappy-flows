organization := "me.maciejb.snappyflows"
name := "snappy-flows"
description := "Snappy compression Akka Streams flows"
homepage := Some(url("https://github.com/maciej/snappy-flows"))
startYear := Some(2015)
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

isSnapshot <<= isSnapshot or version(_ endsWith "-SNAPSHOT")
bintrayOrganization := Some("maciej")

pomIncludeRepository := { _ => false }
publishMavenStyle := true
publishArtifact in Test := false
//noinspection ScalaUnnecessaryParentheses
pomExtra := (
  <scm>
    <url>git@github.com:maciej/snappy-flows.git</url>
    <connection>scm:git:git@github.com:maciej/snappy-flows.git</connection>
  </scm>
    <developers>
      <developer>
        <id>maciej</id>
        <name>Maciej Bilas</name>
        <url>http://maciejb.me</url>
      </developer>
    </developers>
  )
releasePublishArtifactsAction := PgpKeys.publishSigned.value


scalaVersion := "2.11.7"

val akkaVersion = "2.4.1"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "com.typesafe.akka" %% "akka-stream-experimental" % "2.0-M2",
  "org.xerial.snappy" % "snappy-java" % "1.1.2"
)
