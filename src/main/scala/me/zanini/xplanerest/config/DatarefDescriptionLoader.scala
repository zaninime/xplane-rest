package me.zanini.xplanerest.config

import cats.effect.{ContextShift, Sync}
import cats.syntax.functor._
import fs2.{Stream, text}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.generic.auto._
import io.circe.yaml
import me.zanini.xplanerest.model.{
  DatarefDescription,
  FloatDatarefDescription,
  IntDatarefDescription
}
import me.zanini.xplanerest.syntax.LoggingOps._

trait DatarefDescriptionLoader[F[_]] {
  def load(stream: Stream[F, Byte]): F[List[DatarefDescription[F]]]
}

case class YamlRoot(datarefs: List[DatarefTextDescription])
case class DatarefTextDescription(name: String, `type`: String)

class YamlFileDatarefDescriptionLoader[F[_]:  ContextShift](implicit F: Sync[F])
    extends DatarefDescriptionLoader[F] {

  implicit def unsafeLogger: Logger[F] = Slf4jLogger.getLogger[F]

  override def load(stream: Stream[F, Byte]): F[List[DatarefDescription[F]]] =
    stream
      .through(text.utf8Decode)
      .reduce(_ + _)
      .flatMap(
        document =>
          yaml.parser
            .parse(document)
            .flatMap(_.as[YamlRoot]) match {
            case Left(error)  => Stream.raiseError(error)
            case Right(value) => Stream(value)
        })
      .flatMap(root => Stream.emits(root.datarefs))
      .map[Either[String, DatarefDescription[F]]] { description =>
        description.`type` match {
          case "float" => Right(FloatDatarefDescription(description.name))
          case "int"   => Right(IntDatarefDescription(description.name))
          case _ =>
            Left(
              s"${description.name}: no mapping defined for type ${description.`type`}")
        }
      }
      .evalMap[F, List[DatarefDescription[F]]] {
        case Left(error)  => warn(error).as(List())
        case Right(value) => F.delay(List(value))
      }
      .flatMap(list => Stream.emits(list))
      .compile
      .toList
}
