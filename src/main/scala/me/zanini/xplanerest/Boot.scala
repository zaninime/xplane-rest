package me.zanini.xplanerest

import java.net.InetSocketAddress
import java.nio.file.Paths

import cats.effect.{Blocker, ExitCode}
import fs2.io.udp.SocketGroup
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import me.zanini.xplanerest.backend.UDPDatarefCommandHandler
import me.zanini.xplanerest.config.YamlFileDatarefDescriptionLoader
import me.zanini.xplanerest.http.{DatarefDirectoryService, DatarefService}
import me.zanini.xplanerest.model.DatarefDescription
import monix.eval.{Task, TaskApp}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import me.zanini.xplanerest.syntax.LoggingOps._

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

        val makeDatarefServices = (datarefs: List[DatarefDescription[Task]]) =>
          datarefs.map(d => (d, DatarefService(d, datarefCommandHandler)))

        for {
          _ <- info("Loading dataref descriptions")
          descriptions <- datarefsDescriptionLoader.load
          _ <- info("Instantiating services")
          svc = makeDatarefServices(descriptions)
          datarefDirectory = new DatarefDirectoryService[Task](svc)
          httpApp = Router("/v0/dref" -> datarefDirectory.router).orNotFound
          serverBuilder = BlazeServerBuilder[Task]
            .bindHttp(8080, "localhost")
            .withHttpApp(httpApp)
          _ <- info("Starting server")
          _ <- serverBuilder.serve.compile.drain
        } yield ExitCode.Success
    })
  }
}
