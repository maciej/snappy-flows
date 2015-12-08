
lazy val core = Project("core", file("core"))
  .settings(name := "snappy-flows")
  .settings(Settings.common ++ Settings.release)
  .settings {
    import Dependencies._
    libraryDependencies ++= akka ++ Seq(snappy, scalaTest)
  }

lazy val root = Project("root", file("."))
  .settings(name := "snappy-flows-root")
  .settings(Settings.common)
  .settings(publish := {})
  .enablePlugins(ReleasePlugin)
  .aggregate(core)
