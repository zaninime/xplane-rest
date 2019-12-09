package me.zanini.xplanerest.resources

import cats.effect.{Blocker, ContextShift, Sync}

class ResourceLoader[F[_]:  ContextShift](implicit F: Sync[F]) {
  def load(path: String,
           chunkSize: Int,
           blocker: Blocker): fs2.Stream[F, Byte] = {
    val inputStream = F.delay(getClass.getResourceAsStream(path))

    fs2.io.readInputStream(inputStream, chunkSize, blocker)
  }
}
