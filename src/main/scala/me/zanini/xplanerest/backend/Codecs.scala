package me.zanini.xplanerest.backend

import scodec.Codec
import scodec.codecs._

object Codecs {
  implicit def floatCodec: UDPCodec[Float] = new UDPCodec[Float] {
    override def getCodec: Codec[Float] = floatL
  }

  implicit def intCodec: UDPCodec[Int] = new UDPCodec[Int] {
    override def getCodec: Codec[Int] = int32L
  }
}
