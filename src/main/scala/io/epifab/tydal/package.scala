package io.epifab

import cats.effect.IO

package object tydal {
  type Tag = String with Singleton
  type AS[+T, A <: String with Singleton] = T with Tagging[A]
  type IOEither[+ERR, +OUT] = IO[Either[ERR, OUT]]
}
