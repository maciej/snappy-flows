import _root_.pl.project13.scala.sbt.JmhPlugin
import sbt.Keys._

lazy val core = project.in(file("core"))
  .settings(name := "snappy-flows")
  .settings(Settings.common ++ Settings.release)
  .settings {
    import Dependencies._
    libraryDependencies ++= akka ++ Seq(snappy, scalaTest)
  }

lazy val benchmarks = project.in(file("benchmarks"))
  .settings(Settings.common :+ (publish := {}))
  .enablePlugins(JmhPlugin)
  .dependsOn(core)

lazy val root = project.in(file("."))
  .settings(name := "snappy-flows-root")
  .settings(Settings.common)
  .settings(publish := {})
  .enablePlugins(ReleasePlugin)
  .aggregate(core, benchmarks)
