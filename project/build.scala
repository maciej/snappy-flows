import bintray.BintrayKeys._
import com.typesafe.sbt.pgp.PgpKeys
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin.autoImport._

object Versions {
  val akka = "2.4.2-RC3"
}

object Dependencies {
  val scalaTest =
    "org.scalatest" %% "scalatest" % "2.2.4" % "test"

  val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % Versions.akka,
    "com.typesafe.akka" %% "akka-testkit" % Versions.akka % "test",
    "com.typesafe.akka" %% "akka-stream" % Versions.akka
  )

  val snappy = "org.xerial.snappy" % "snappy-java" % "1.1.2"
}

object Settings {
  val common = Seq(
    scalaVersion := "2.11.7",
    organization := "me.maciejb.snappyflows",
    description := "Snappy compression Akka Streams flows",
    homepage := Some(url("https://github.com/maciej/snappy-flows")),
    startYear := Some(2015),
    licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
  )

  val release = Seq(
    isSnapshot <<= isSnapshot or version(_ endsWith "-SNAPSHOT"),
    bintrayOrganization := Some("maciej"),
    pomIncludeRepository := { _ => false },
    publishMavenStyle := true,
    publishArtifact in Test := false,
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
      ),
    releasePublishArtifactsAction := PgpKeys.publishSigned.value
  )
}
