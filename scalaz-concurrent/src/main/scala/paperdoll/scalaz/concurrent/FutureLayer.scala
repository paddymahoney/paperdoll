package paperdoll.scalaz.concurrent

import paperdoll.core.effect.Effects.{sendU, unsafeRun}
import scalaz.concurrent.Future
import paperdoll.core.effect.Effects

object FutureLayer {
  def sendFuture[A](fa: Future[A]): Effects.One[Future_, A] = sendU(fa)
  /** We can't generally "move future past" other effects -
   *  we could use a callback, but there is no way to guarantee
   *  that that callback is finally run (since other effects
   *  in the stack might include e.g. Option).
   *  So we only allow running Future as the final effect.
   */
  def unsafeRunFuture[A](effects: Effects.One[Future_, A]): Future[A] =
    unsafeRun(effects)
}