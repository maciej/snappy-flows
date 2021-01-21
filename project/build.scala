import bintray.BintrayKeys._
import com.jsuereth.sbtpgp.PgpKeys
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin.autoImport._

object Versions {
  val akka = "2.6.10"
}

//noinspection TypeAnnotation
object Dependencies {
  val scalaTest =
    "org.scalatest" %% "scalatest" % "3.2.3" % "test"

  val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % Versions.akka,
    "com.typesafe.akka" %% "akka-testkit" % Versions.akka % "test",
    "com.typesafe.akka" %% "akka-stream" % Versions.akka
  )

  val snappy = "org.xerial.snappy" % "snappy-java" % "1.1.8.2"
}

object Settings {
  val common = Seq(
    scalaVersion := "2.13.4",
    crossScalaVersions := Seq(scalaVersion.value, "2.12.13"),
    organization := "me.maciejb.snappyflows",
    description := "Snappy compression Akka Streams flows",
    homepage := Some(url("https://github.com/maciej/snappy-flows")),
    startYear := Some(2015),
    licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
  )

  val release = Seq(
    isSnapshot := (isSnapshot or version(_ endsWith "-SNAPSHOT")).value,
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
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    releaseCrossBuild := true
  )
}
