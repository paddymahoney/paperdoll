package paperdoll.core.queue

import scalaz.Forall

/**
 * A type-aligned queue C[A, X] :: C[X, Y] :: ... :: C[Z, B]
 * Implemented as a kind of lazy binary tree: a Queue is either empty,
 * one element, or two MiniQueue ends and a queue of queues in the middle.
 * Theoretically this provides amortised O(1) :+ and take operations:
 * we only have to increase/decrease the depth to N once every 2^N
 * operations.
 * TODO: Benchmark and/or automated test to confirm this implementation is actually O(1)
 * and not accidentally much slower due to implementation errors.
 * The code was ported from Haskell (a lazy language) at a time when I didn't
 * really understand it, and so may have issues with eager evaluation in Scala
 * Also untested usage patterns might result in stack overflows for the same reason.
 */
sealed trait Queue[C[_, _], A, B] {
  def :+[Z](element: C[B, Z]): Queue[C, A, Z]
  /**
   * Examine the front of the queue, destructuring it as either the queue being empty,
   * or a head and tail. Usually followed by a .fold which acts as a shallow fold
   * (i.e. no recursion). Most callers end up recursing in one way or another,
   * but I've found it too difficult to abstract over the type differences enough to factor
   * out this common recursion.
   * This is a basic operation that would be really useful
   * to have on ordinary List, but I'm not aware of it having a standard name
   */
  def destructureHead: DestructuredHead[Queue, C, A, B]
}
object Queue {
  def empty[C[_, _], A]: Queue[C, A, A] = Empty()
  def one[C[_, _], A, B](value: C[A, B]): Queue[C, A, B] = One(value)
}
/**
 * Empty queue - note that this implies A === B at the type level
 */
private[queue] final case class Empty[C[_, _], A]() extends Queue[C, A, A] {
  override def :+[Z](e: C[A, Z]) = One(e)
  override def destructureHead = DestructuredHead.nil
}
private[queue] final case class One[C[_, _], A, B](value: C[A, B]) extends Queue[C, A, B] {
  override def :+[Z](e: C[B, Z]) =
    Node[C, A, Z, B, B](MiniQueue.one(value), Empty[Pair_[C]#O, B](), MiniQueue.one(e))
  override def destructureHead = DestructuredHead.cons(value, Empty())
}
private[queue] final case class Node[C[_, _], A, B, X, Y](
  head: MiniQueue[C, A, X], middle: Queue[Pair_[C]#O, X, Y], last: MiniQueue[C, Y, B]) extends Queue[C, A, B] {
  override def :+[Z](newLast: C[B, Z]) =
    last.fold(lastOne ⇒ Node(head, middle, MiniQueue.pair(Pair(lastOne, newLast))),
      lastPair ⇒ Node(head, middle :+ lastPair, MiniQueue.one(newLast)))

  override def destructureHead = head.fold({
    headOne ⇒
      DestructuredHead.cons(headOne, middle.destructureHead.fold({
        witness ⇒ witness.subst[({ type L[V] = MiniQueue[C, V, B] })#L](last).asQueue
      }, new Forall[({ type L[V] = (Pair[C, X, V], Queue[Pair_[C]#O, V, Y]) ⇒ Queue[C, X, B] })#L] {
        override def apply[V] = (head, tail) ⇒ Node(MiniQueue.pair(head), tail, last)
      }))
  },
    {
      _.fold(new Forall[({ type L[W] = (C[A, W], C[W, X]) ⇒ DestructuredHead[Queue, C, A, B] })#L] {
        override def apply[W] = (first, second) ⇒ DestructuredHead.cons(first, Node(MiniQueue.one[C, W, X](second), middle, last))
      })
    })
}