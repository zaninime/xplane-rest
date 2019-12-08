package me.zanini.xplanerest.http

import cats.effect.Sync
import cats.syntax.apply._
import io.circe.generic.auto._
import io.circe.syntax._
import me.zanini.xplanerest.{DatarefCommandHandler, DatarefValue, UDPCodec}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes, Response}

case class DatarefDescription(name: String, `type`: String)

class DatarefService[F[_]: Sync, V: UDPCodec](
    description: DatarefDescription,
    commandHandler: DatarefCommandHandler[F])(
    implicit entityDecoder: EntityDecoder[F, V])
    extends Http4sDsl[F] {
  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root                 => Ok(description.asJson)
    case req @ PUT -> Root / "value" => req.decode[V](putEndpoint)
  }

  private def putEndpoint(body: V): F[Response[F]] = {
    val value = DatarefValue[V](description.name, body)
    commandHandler.store(value) *> NoContent()
  }
}
