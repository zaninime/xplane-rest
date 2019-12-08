import Dependencies._

ThisBuild / scalaVersion     := "2.13.1"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "me.zanini"
ThisBuild / organizationName := "Zanini [dot] me"
ThisBuild / scalacOptions ++= Seq(
    "-language:implicitConversions",
    "-language:higherKinds",
)

lazy val root = (project in file("."))
  .settings(
    name := "xplane-rest",
    libraryDependencies ++= appDeps,
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
