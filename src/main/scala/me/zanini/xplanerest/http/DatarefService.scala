package me.zanini.xplanerest.http

import cats.effect.Sync
import cats.syntax.apply._
import me.zanini.xplanerest.backend.DatarefCommandHandler
import me.zanini.xplanerest.model.{
  DatarefDescription,
  DatarefValue,
  FloatDatarefDescription,
  IntDatarefDescription
}
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Response}
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityEncoder._

class DatarefService[F[_]: Sync](description: DatarefDescription[F],
                                 commandHandler: DatarefCommandHandler[F])
    extends Http4sDsl[F] {

  import description._

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => Ok(DatarefDoc(description.name, valueType).asJson)
    case req @ PUT -> Root / "value" =>
      req.decode[description.Value](putEndpoint)
  }

  private def putEndpoint(body: description.Value): F[Response[F]] = {
    val value = DatarefValue(description.name, body)
    commandHandler.store(value) *> NoContent()
  }

  private def valueType: String = description match {
    case FloatDatarefDescription(_) => "float"
    case IntDatarefDescription(_)   => "int"
  }
}

object DatarefService {
  def apply[F[_]: Sync](
      description: DatarefDescription[F],
      commandHandler: DatarefCommandHandler[F]): DatarefService[F] =
    new DatarefService(description, commandHandler)
}
