import Dependencies._

enablePlugins(GatlingPlugin)

lazy val root = (project in file("."))
  .settings(
    inThisBuild(List(
      organization := "org.broad",
      scalaVersion := "2.12.8",
      version := "0.1.0-SNAPSHOT"
    )),
    name := "perf",
    libraryDependencies ++= gatling
  )
