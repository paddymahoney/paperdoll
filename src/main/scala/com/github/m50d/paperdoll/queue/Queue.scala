package com.github.m50d.paperdoll.queue

import com.github.m50d.paperdoll.queue.pair.{ Pair, Pair_ }
import scalaz.Forall

/**
 * Function1 for types of kind [_, _]
 */
trait FunctionKK[C[_, _], D[_, _]] {
  def apply[X, Y](c: C[X, Y]): D[X, Y]
}

/**
 * One or two element mini-queue
 * TODO: better documentation, separation, fold instead of pattern match
 */
sealed trait B[C[_, _], A, B]
/**
 * One element
 */
final case class B1[C[_, _], A, B0](a: C[A, B0]) extends B[C, A, B0]
/**
 * Two elements
 */
final case class B2[C[_, _], A, B0, W0](v: Pair[C, A, B0]) extends B[C, A, B0] {
  type W = W0
}

/**
 * A type-aligned queue C[A, X] :: C[X, Y] :: ... :: C[Z, B]
 * Implemented as a kind of lazy binary tree: a Queue is either empty,
 * one element, or two ends and a queue of queues in the middle.
 * Theoretically this provides amortised O(1) append and take operations:
 * we only have to increase/decrease the depth to N once every 2^N
 * operations.
 * TODO: Benchmark and/or automated test to confirm this implementation is actually O(1)
 * and not accidentally much slower due to implementation errors.
 * The code was ported from Haskell (a lazy language) at a time when I didn't
 * really understand it, and so may have issues with eager evaluation in Scala
 * Also untested usage patterns might result in stack overflows for the same reason.
 */
sealed trait Queue[C[_, _], A, B] {
  /**
   * Append a single element e onto the end of this queue
   */
  def |>[Z](e: C[B, Z]): Queue[C, A, Z]
  /**
   * Either empty, or head and tail. This is a basic operation that would be really useful
   * to have on ordinary List, but I don't know the name for it
   */
  def tviewl: TAViewL[Queue, C, A, B]
  /**
   * Prepend a single element l onto the beginning of this queue
   */
  def <|:[X](l: C[X, A]): Queue[C, X, B] = Q1(l) >< this
  /**
   * Append a queue R onto the end of this queue
   */
  def ><[X](r: Queue[C, B, X]): Queue[C, A, X] = tviewl.fold(
    { witness => witness.subst[({ type L[V] = Queue[C, V, X] })#L](r) },
    new Forall[({ type L[W] = (C[A, W], Queue[C, W, B]) => Queue[C, A, X] })#L] {
      override def apply[W] = {
        (head, tail) =>
          head <|: (tail >< r)
      }
    })
}
/**
 * An empty queue - note that this implies A === B at the type level
 */
final case class Q0[C[_, _], A]() extends Queue[C, A, A] {
  override def |>[Z](e: C[A, Z]) = Q1(e)
  override def tviewl = TAViewL.nil
  override def <|:[X](l: C[X, A]): Queue[C, X, A] =
    Q1(l)
}
/**
 * A 1-element queue
 */
final case class Q1[C[_, _], A, B](a: C[A, B]) extends Queue[C, A, B] {
  override def |>[Z](e: C[B, Z]) =
    QN[C, A, Z, B, B](B1(a), Q0[Pair_[C]#O, B](), B1(e))
  override def tviewl = TAViewL.cons(a, Q0())
}
final case class QN[C[_, _], A, B0, X, Y](
  l: B[C, A, X], m: Queue[Pair_[C]#O, X, Y], r: B[C, Y, B0]) extends Queue[C, A, B0] {
  override def |>[Z](e: C[B0, Z]) =
    r match {
      case B1(a) ⇒ QN(l, m, B2(Pair(a, e)))
      case B2(r) ⇒ QN(l, m |> r, B1(e))
    }
  override def tviewl = l match {
    case B2(p) ⇒
      p.fold(new Forall[({ type L[W] = (C[A, W], C[W, X]) => TAViewL[Queue, C, A, B0] })#L] {
        override def apply[W] = {
          (a, b) =>
            TAViewL.cons(a, QN(B1[C, W, X](b), m, r))
        }
      })

    case B1(a) ⇒ {
      def buf2queue[Z, W](b: B[C, Z, W]): Queue[C, Z, W] = b match {
        case B1(a) ⇒ Q1(a)
        case B2(cs) ⇒
          cs.fold(new Forall[({ type L[V] = (C[Z, V], C[V, W]) => Queue[C, Z, W] })#L] {
            override def apply[W] = {
              (a, b) =>
                QN(B1(a), Q0[Pair_[C]#O, W](), B1(b))
            }
          })
      }
      def shiftLeft[A, B3, W](q: Queue[Pair_[C]#O, A, W], r: B[C, W, B3]): Queue[C, A, B3] =
        q.tviewl.fold({
          witness => buf2queue(witness.subst[({ type L[V] = B[C, V, B3] })#L](r))
        }, new Forall[({ type L[V] = (Pair[C, A, V], Queue[Pair_[C]#O, V, W]) => Queue[C, A, B3] })#L] {
          override def apply[V] = {
            (head, tail) =>
              QN(B2(head), tail, r)
          }
        })
      TAViewL.cons(a, shiftLeft(m, r))
    }
  }
}
object Queue {
  def tmapp[C[_, _], D[_, _]](f: FunctionKK[C, D]): FunctionKK[Pair_[C]#O, Pair_[D]#O] =
    new FunctionKK[Pair_[C]#O, Pair_[D]#O] {
      def apply[X, Y](phi: Pair[C, X, Y]): Pair[D, X, Y] =
        phi.fold(new Forall[({ type L[W] = (C[X, W], C[W, Y]) => Pair[D, X, Y] })#L] {
          override def apply[W] = {
            (a, b) => Pair(f(a), f(b))
          }
        })
    }
  def tmapb[C[_, _], D[_, _]](f: FunctionKK[C, D]): FunctionKK[({ type L[X, Y] = (B[C, X, Y]) })#L, ({ type L[X, Y] = (B[D, X, Y]) })#L] =
    new FunctionKK[({ type L[X, Y] = (B[C, X, Y]) })#L, ({ type L[X, Y] = (B[D, X, Y]) })#L] {
      def apply[X, Y](phi: B[C, X, Y]) =
        phi match {
          case B1(v) ⇒ B1(f.apply(v))
          case B2(v) ⇒ B2(tmapp(f)(v))
        }
    }
}