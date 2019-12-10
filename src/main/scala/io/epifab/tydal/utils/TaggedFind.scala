package io.epifab.tydal.utils

import io.epifab.tydal._
import shapeless.{::, Generic, HList}

import scala.annotation.implicitNotFound

@implicitNotFound("Field or relation ${T} could not be found")
trait TaggedFind[T <: String with Singleton, +X, Haystack] {
  def apply(u: Haystack): X As T
}

object TaggedFind {
  implicit def tuple2Find1[T <: String with Singleton, X, X1, X2](
    implicit
    t1Find: TaggedFind[T, X, X1]
  ): TaggedFind[T, X, (X1, X2)] =
    (u: (X1, X2)) => t1Find(u._1)

  implicit def tuple2Find2[T <: String with Singleton, X, X1, X2](
    implicit
    t2Find: TaggedFind[T, X, X2]
  ): TaggedFind[T, X, (X1, X2)] =
    (u: (X1, X2)) => t2Find(u._2)

  implicit def headFind[T <: String with Singleton, X, Tail <: HList]: TaggedFind[T, X, (X As T) :: Tail] =
    (u: (X As T) :: Tail) => u.head

  implicit def tailFind[T <: String with Singleton, X, H, Tail <: HList](
    implicit
    find: TaggedFind[T, X, Tail]
  ): TaggedFind[T, X, H :: Tail] =
    (u: H :: Tail) => find(u.tail)

  implicit def caseClassFind[T <: String with Singleton, X, CC, Repr](
    implicit
    generic: Generic.Aux[CC, Repr],
    find: TaggedFind[T, X, Repr]
  ): TaggedFind[T, X, CC] =
    (cc: CC) => find(generic.to(cc))
}
