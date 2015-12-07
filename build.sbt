
lazy val core = Project("snappy-flows", file("core"))
  .settings(name := "snappy-flows")
  .settings(Settings.common ++ Settings.release)
  .settings {
    import Dependencies._
    libraryDependencies ++= akka ++ Seq(snappy, scalaTest)
  }
  .enablePlugins(ReleasePlugin)

lazy val root = Project("root", file("."))
  .settings(name := "snappy-flows-root")
  .settings(Settings.common)
  .disablePlugins(ReleasePlugin)
  .aggregate(core)
