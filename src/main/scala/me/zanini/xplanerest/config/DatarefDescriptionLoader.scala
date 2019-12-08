package me.zanini.xplanerest.config

import java.nio.file.Path

import cats.effect.{Blocker, ContextShift, Sync}
import fs2.io.file
import fs2.{Stream, text}
import io.circe.generic.auto._
import io.circe.yaml
import me.zanini.xplanerest.http.DatarefDescription

trait DatarefDescriptionLoader[F[_]] {
  def load: F[List[DatarefDescription]]
}

case class YamlRoot(datarefs: List[DatarefDescription])

class YamlFileDatarefDescriptionLoader[F[_]: Sync: ContextShift](
    path: Path,
    blocker: Blocker)
    extends DatarefDescriptionLoader[F] {
  override def load: F[List[DatarefDescription]] =
    file
      .readAll(path, blocker, 4096)
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
      .map(_.datarefs)
      .compile
      .lastOrError
}
