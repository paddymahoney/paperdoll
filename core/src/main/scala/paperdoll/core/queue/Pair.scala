package paperdoll.core.queue

import scalaz.Forall

/**
 * A type-aligned pair of C[A, W] and C[W, B]
 * for some unknown type W.
 * Type W is deliberately inaccessible from outside
 */
private[queue] sealed trait Pair[C[_, _], A, B] {
  def fold[Z](f: Forall[({ type L[W] = (C[A, W], C[W, B]) => Z })#L]): Z
}
private[queue] sealed trait Pair_[C[_, _]] {
  final type O[X, Y] = Pair[C, X, Y]
}
private[queue] object Pair {
  def apply[C[_, _], A, B, X](a: C[A, X], b: C[X, B]): Pair[C, A, B] = new Pair[C, A, B] {
    override def fold[Z](f: Forall[({ type L[W] = (C[A, W], C[W, B]) => Z })#L]) =
      f[X](a, b)
    override def toString = f"P($a%s,$b%s)"
  }
}