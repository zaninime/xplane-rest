package me.zanini.xplanerest.backend

import scodec.Codec

trait UDPCodec[A] {
  def getCodec: Codec[A]
}
