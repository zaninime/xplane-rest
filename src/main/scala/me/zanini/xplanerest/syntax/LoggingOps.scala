package me.zanini.xplanerest.syntax

import io.chrisdavenport.log4cats.Logger

object LoggingOps {
  def info[F[_]: Logger](message: => String): F[Unit] = Logger[F].info(message)
  def warn[F[_]: Logger](message: => String): F[Unit] = Logger[F].warn(message)
  def error[F[_]: Logger](message: => String): F[Unit] =
    Logger[F].error(message)
}
