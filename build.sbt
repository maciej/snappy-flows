import _root_.pl.project13.scala.sbt.JmhPlugin
import sbt.Keys._

lazy val snappyFlows = project.in(file("."))
  .settings(name := "snappy-flows")
  .settings(Settings.common ++  Settings.release)
  .settings {
    import Dependencies._
    libraryDependencies ++= akka ++ Seq(snappy, scalaTest)
  }
  .enablePlugins(ReleasePlugin)

lazy val benchmarks = project.in(file("benchmarks"))
  .settings(Settings.common :+ (publish := {}))
  .enablePlugins(JmhPlugin)
  .dependsOn(snappyFlows)
