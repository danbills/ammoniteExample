name := "xdg-iron-monad"

version := "0.1.0"

scalaVersion := "3.8.0"

libraryDependencies ++= Seq(
  // Iron for refined types
  "io.github.iltotore" %% "iron" % "3.2.0",
  "io.github.iltotore" %% "iron-cats" % "3.2.0",

  // ScalaZ for RWS monad
  "org.scalaz" %% "scalaz-core" % "7.3.8",
  "org.scalaz" %% "scalaz-effect" % "7.3.8",

  // Better Files for filesystem operations
  "com.github.pathikrit" %% "better-files" % "3.9.2"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-language:postfixOps"
)
