package me.zanini.xplanerest.http

import cats.effect.Sync
import cats.implicits._
import me.zanini.xplanerest.model.DatarefDescription
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Allow
import org.http4s.server.Router
import org.http4s.{HttpRoutes, Request, Response}

class DatarefDirectoryService[F[_]: Sync](
    datarefs: List[(DatarefDescription[F], DatarefService[F])])
    extends Http4sDsl[F] {
  def router = Router("/" -> routes)

  private def routes = HttpRoutes.of[F] {
    case GET -> Root =>
      import org.http4s.circe.CirceEntityEncoder._
      Ok(serviceMap.keySet.toList.sorted)
    case _ -> Root => MethodNotAllowed(Allow(GET))
    case request @ _ -> path =>
      val pathWithoutSlash = path.toString.substring(1)

      val serviceName = pathWithoutSlash match {
        case x if x.endsWith("/value") => x.dropRight("/value".length)
        case x                         => x
      }

      val newPath = request.pathInfo.drop(serviceName.length + 1)
      passToService(request.withPathInfo(newPath), serviceName)
  }

  private def passToService(request: Request[F],
                            serviceName: String): F[Response[F]] = {
    val endpoint: HttpRoutes[F] =
      serviceMap.getOrElse(serviceName, notFoundRoute)

    endpoint
      .run(request)
      .value
      .map(_.getOrElse(Response(NotFound)))
  }

  private def notFoundRoute =
    HttpRoutes.of[F]({
      case _ => NotFound()
    })

  private def serviceMap: Map[String, HttpRoutes[F]] =
    datarefs.map {
      case (description, service) => description.name -> service.routes
    }.toMap
}
