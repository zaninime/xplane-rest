package me.zanini.xplanerest.model

import cats.effect.Sync
import me.zanini.xplanerest.backend.{Codecs, UDPCodec}
import org.http4s.EntityDecoder
import org.http4s.circe.CirceEntityDecoder

sealed trait DatarefDescription[F[_]] {
  def name: String
  type Value

  implicit def entityDecoder: EntityDecoder[F, Value]
  implicit def udpCodec: UDPCodec[Value]
}

case class FloatDatarefDescription[F[_]: Sync](name: String)
    extends DatarefDescription[F] {
  override type Value = Float

  override implicit def udpCodec: UDPCodec[Value] =
    Codecs.floatCodec

  override implicit def entityDecoder: EntityDecoder[F, Value] =
    CirceEntityDecoder.circeEntityDecoder
}

case class IntDatarefDescription[F[_]: Sync](name: String)
    extends DatarefDescription[F] {
  override type Value = Int

  override implicit def udpCodec: UDPCodec[Value] =
    Codecs.intCodec

  override implicit def entityDecoder: EntityDecoder[F, Value] =
    CirceEntityDecoder.circeEntityDecoder
}
