package me.zanini.xplanerest

import java.net.InetSocketAddress
import java.nio.file.Paths

import cats.effect.{Blocker, ExitCode}
import cats.implicits._
import fs2.io.udp.SocketGroup
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.{Logger, SelfAwareStructuredLogger}
import me.zanini.xplanerest.backend.UDPDatarefCommandHandler
import me.zanini.xplanerest.config.YamlFileDatarefDescriptionLoader
import me.zanini.xplanerest.http.{DatarefDescription, DatarefService}
import monix.eval.{Task, TaskApp}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

object Boot extends TaskApp {
  private implicit def unsafeTaskLogger: SelfAwareStructuredLogger[Task] =
    Slf4jLogger.getLogger[Task]

  override def run(args: List[String]): Task[ExitCode] = {
    val r = for {
      blocker <- Blocker[Task]
      group <- SocketGroup(blocker)
      socket <- group.open()
    } yield (socket, blocker)

    r.use({
      case (socket, blocker) =>
        val datarefsDescriptionLoader =
          new YamlFileDatarefDescriptionLoader[Task](Paths.get("datarefs.yaml"),
                                                     blocker)

        val datarefCommandHandler = new UDPDatarefCommandHandler[Task](
          socket,
          new InetSocketAddress("127.0.0.1", 49000))

        val makeDatarefServices = (datarefs: List[DatarefDescription]) =>
          datarefs
            .map(description => {
              val service = DatarefService(description, datarefCommandHandler)

              service match {
                case Left(error) => {
                  Logger[Task].error(
                    s"Cannot initialize ${description.name}: $error. Will not be available") *> Task
                    .delay(List())
                }
                case Right(svc) =>
                  Task.delay(List(s"/${description.name}" -> svc.routes))
              }
            })
            .sequence
            .map(_.flatten)

        for {
          descriptions <- datarefsDescriptionLoader.load
          svc <- makeDatarefServices(descriptions)
          datarefRouter = Router(svc: _*)
          httpApp = Router("/v0/dref" -> datarefRouter).orNotFound
          serverBuilder = BlazeServerBuilder[Task]
            .bindHttp(8080, "localhost")
            .withHttpApp(httpApp)
          _ <- serverBuilder.serve.compile.drain
        } yield ExitCode.Success
    })
  }
}
