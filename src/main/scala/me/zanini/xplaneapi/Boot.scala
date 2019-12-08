package me.zanini.xplaneapi

import java.net.InetSocketAddress

import cats.effect.{Blocker, ExitCode}
import cats.syntax.functor._
import fs2.io.udp.SocketGroup
import me.zanini.xplaneapi.http.{DatarefDescription, DatarefService}
import monix.eval.{Task, TaskApp}
import org.http4s.EntityDecoder
import org.http4s.server.Router
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import scodec.Codec
import scodec.codecs.floatL

object Boot extends TaskApp {
  implicit def floatDecoder: EntityDecoder[Task, Float] =
    EntityDecoder[Task, String].map(_.toFloat)

  implicit def floatUdpCodec: UDPCodec[Float] = new UDPCodec[Float] {
    override def getCodec: Codec[Float] = floatL
  }

  private def datarefs = Seq(
    DatarefDescription("sim/cockpit/radios/com1_freq_hz", "float"),
    DatarefDescription("sim/cockpit/radios/com2_freq_hz", "float")
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

      val services = datarefs.map(description => {
        def makeService[V: UDPCodec](
            implicit entityDecoder: EntityDecoder[Task, V]) =
          new DatarefService[Task, V](description, datarefCommandHandler)

        val service = description.`type` match {
          case "float" => makeService[Float]
          case t       => throw new Exception(s"No mapping defined for type $t")
        }

        s"/${description.name}" -> service.routes
      })

      val datarefRouter = Router(services: _*)

      val httpApp = Router("/dref" -> datarefRouter).orNotFound
      val serverBuilder = BlazeServerBuilder[Task]
        .bindHttp(8080, "localhost")
        .withHttpApp(httpApp)

      serverBuilder.serve.compile.drain.as(ExitCode.Success)
    })
  }
}
