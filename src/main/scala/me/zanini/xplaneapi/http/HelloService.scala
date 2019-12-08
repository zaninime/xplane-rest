package me.zanini.xplaneapi.http

import cats.effect.Sync
import cats.syntax.semigroupk._
import org.http4s._
import org.http4s.dsl.Http4sDsl

class HelloService[F[_]: Sync] extends Http4sDsl[F] {
  val helloWorldService = HttpRoutes.of[F] {
    case GET -> Root / "hello" / name =>
      Ok(s"Hello, $name.")
  }

  val helloWorldService2 = HttpRoutes.of[F] {
    case GET -> Root / "hello2" / name =>
      Ok(s"Hello2, $name.")
  }

  val x: HttpRoutes[F] =
    Seq(helloWorldService, helloWorldService2).reduce((a, b) => a.combineK(b))
}
