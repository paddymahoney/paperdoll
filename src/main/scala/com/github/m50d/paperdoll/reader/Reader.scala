package com.github.m50d.paperdoll.reader

import shapeless.{ Coproduct, CNil, :+: }
import shapeless.ops.coproduct.Inject
import scalaz.syntax.monad._
import scalaz.{ Forall, Leibniz }
import scalaz.Leibniz.===
import com.github.m50d.paperdoll.layer.Layers
import com.github.m50d.paperdoll.effect.{ Eff, Eff_, Arr }
import com.github.m50d.paperdoll.layer.Member
import com.github.m50d.paperdoll.effect.Bind

/**
 * The type representing an effectful value of type X
 * that reads from input of type I
 * Implementation is encapsulated (hopefully).
 */
sealed trait Reader[I, X] {
  def fold[A](get: (I === X) => A): A
}
private[reader] final case class Get[I, X](val witness: Leibniz.===[I, X]) extends Reader[I, X] {
  override def fold[A](get: (I === X) => A) = get(witness)
}

object Reader {
  /**
   * Effect that reads an input I and returns it.
   */
  def ask[I, R <: Coproduct, F[_] <: Coproduct](implicit l: Layers.Aux[R, F], inj: Inject[F[I], Reader_[I]#F[I]]): Eff[R, Layers.Aux[R, F], I] =
    Eff.send[Reader_[I]#F, R, F, I](Get[I, I](Leibniz.refl))
  /**
   * Specialisation of ask to the case where Reader_[I] is the *only* effect.
   */
  def askReaderOnly[I]: Eff[Reader_[I] :+: CNil, Layers[Reader_[I] :+: CNil] { type O[X] = Reader[I, X] :+: CNil }, I] =
    ask[I, Reader_[I] :+: CNil, ({ type L[X] = Reader[I, X] :+: CNil })#L]
  /**
   * Slightly arbitrary effectful operator: reads an Int value and adds x to it.
   * Could be implemented "manually" with for/yield.
   */
  def addGet[R <: Coproduct, F[_] <: Coproduct](x: Int)(
    implicit l: Layers.Aux[R, F], inj: Inject[F[Int], Reader_[Int]#F[Int]]): Eff[R, Layers.Aux[R, F], Int] =
    Eff.monadEff.bind(ask[Int, R, F](l, inj)) { i => (i + x).point[Eff_[R, Layers.Aux[R, F]]#O] }
  /**
   * Run the reader effect in the stack R by passing the input i
   * (i.e. giving the value i to any reads in the "lazy effectful value" e),
   * removing Reader_[I] from the stack of effects in the result.
   */
  def runReader[I, R <: Coproduct, L1 <: Layers[R], A, L2 <: Layers[R]](i: I, e: Eff[R, L1, A])(
    implicit me: Member[R, Reader_[I]] {
      type L = L2
    }, le: Leibniz[Nothing, Layers[R], L1, L2]): Eff[me.RestR, me.RestL, A] =
    Eff.handleRelay[Reader_[I], R, me.RestR, me.RestL, A](
      new Bind[Reader_[I]] {
        override def apply[V, RR <: Coproduct, RL <: Layers[RR], A](reader: Reader[I, V], arr: Arr[RR, RL, V, A]) =
          reader.fold(witness => arr(witness(i)))
      })(me).apply(le.subst[({ type L[X <: Layers[R]] = Eff[R, X, A] })#L](e))
}