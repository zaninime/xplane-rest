import sbt._

object Dependencies {
  private lazy val cats = Seq("core", "kernel", "macros", "effect").map(lib =>
    "org.typelevel" %% s"cats-$lib" % "2.0.0")

  private lazy val fs2 =
    Seq("core", "io").map(lib => "co.fs2" %% s"fs2-$lib" % "2.1.0")

  private lazy val monix = Seq("io.monix" %% "monix" % "3.1.0")

  private lazy val scodec = Seq("org.scodec" %% "scodec-core" % "1.11.4",
                                "org.scodec" %% "scodec-bits" % "1.1.12")
  private lazy val http4s =
    Seq("blaze-server", "blaze-client", "circe", "dsl").map(lib =>
      "org.http4s" %% s"http4s-$lib" % "0.21.0-M6")

  private lazy val circe =
    Seq("core", "generic", "parser", "generic-extras").map(lib =>
      "io.circe" %% s"circe-$lib" % "0.12.1")

  private lazy val circeYaml = Seq("io.circe" %% "circe-yaml" % "0.12.0")

  private lazy val slf4j = Seq("org.slf4j" % "slf4j-simple" % "1.7.29")

  private lazy val log4cats = Seq("core", "slf4j").map(lib =>
    "io.chrisdavenport" %% s"log4cats-$lib" % "1.0.1")

  private lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"

  lazy val appDeps
    : Seq[ModuleID] = monix ++ cats ++ fs2 ++ scodec ++ http4s ++ circe ++ circeYaml ++ slf4j ++ log4cats ++ testDeps
  lazy val testDeps: Seq[ModuleID] = Seq(scalaTest).map(_ % Test)
}
