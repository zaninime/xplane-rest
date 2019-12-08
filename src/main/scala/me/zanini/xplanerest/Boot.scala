package me.zanini.xplanerest

import java.net.InetSocketAddress

import cats.effect.{Blocker, ExitCode}
import cats.implicits._
import fs2.io.udp.SocketGroup
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.{Logger, SelfAwareStructuredLogger}
import me.zanini.xplanerest.backend.UDPDatarefCommandHandler
import me.zanini.xplanerest.http.{DatarefDescription, DatarefService}
import monix.eval.{Task, TaskApp}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

object Boot extends TaskApp {
  private implicit def unsafeTaskLogger: SelfAwareStructuredLogger[Task] =
    Slf4jLogger.getLogger[Task]

  private def datarefs: List[DatarefDescription] = List(
    DatarefDescription("sim/cockpit/radios/com1_freq_hz", "float"),
    DatarefDescription("sim/cockpit/radios/com2_freq_hz", "float"),
    DatarefDescription("sim/cockpit/radios/com3_freq_hz", "string")
  )

  override def run(args: List[String]): Task[ExitCode] = {
    val r = for {
      blocker <- Blocker[Task]
      group <- SocketGroup(blocker)
      socket <- group.open()
    } yield socket

    r.use(socket => {
      val datarefCommandHandler = new UDPDatarefCommandHandler[Task](
        socket,
        new InetSocketAddress("127.0.0.1", 49000))

      val services = datarefs
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
        svc <- services
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
