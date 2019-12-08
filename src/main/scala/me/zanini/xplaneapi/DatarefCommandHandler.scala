package me.zanini.xplaneapi

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

import cats.data.EitherT
import cats.effect.Sync
import fs2.Chunk
import fs2.io.udp.{Packet, Socket}
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs.{constant, paddedFixedSizeBytes, string}
import scodec.{Codec, Err, GenCodec}

trait DatarefCommandHandler[F[_]] {
  def store[A: UDPCodec](value: DatarefValue[A]): F[Either[Err, Unit]]
}

trait UDPCodec[A] {
  def getCodec: Codec[A]
}

class UDPDatarefCommandHandler[F[_]: Sync](socket: Socket[F],
                                           remoteAddr: InetSocketAddress)
    extends DatarefCommandHandler[F] {
  override def store[A](value: DatarefValue[A])(
      implicit codec: UDPCodec[A]): F[Either[Err, Unit]] = {
    val packetCodec = getPacketCodec(codec.getCodec)

    def encode: EitherT[F, Err, BitVector] =
      EitherT(
        Sync[F].delay(packetCodec.encode((value.name, value.value)).toEither))

    def send(content: BitVector): EitherT[F, Err, Unit] =
      EitherT.liftF(
        socket.write(
          Packet(remoteAddr, Chunk.byteBuffer(content.toByteBuffer))))

    (for {
      content <- encode
      result <- send(content)
    } yield result).value
  }

  private def getPacketCodec[A](
      valueCodec: Codec[A]): GenCodec[(String, A), (String, A)] =
    paddedFixedSizeBytes(
      509,
      constant(ByteVector.encodeAscii("DREF").toOption.get) ~ constant(BitVector
        .low(8)) ~ valueCodec ~ string(StandardCharsets.US_ASCII) ~ constant(
        BitVector.low(8)),
      constant(ByteVector.encodeAscii(" ").toOption.get)
    ).contramap((v: (String, A)) => (((((), ()), v._2), v._1), ()))
      .map(v => (v._1._2, v._1._1._2))
}
