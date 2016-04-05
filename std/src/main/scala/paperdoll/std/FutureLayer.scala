package paperdoll.std

import paperdoll.core.effect.Effects
import paperdoll.core.effect.Bind
import paperdoll.core.effect.Handler
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import paperdoll.core.effect.Arr
import paperdoll.core.layer.Layers
import shapeless.{ :+:, CNil, Coproduct }
import scalaz.Forall
import paperdoll.core.effect.Arrs

object FutureLayer {
  /** We allow running stdlib Future only as the last Layer,
   *  since it's side-effecting and so can't be safely combined with laziness.
   *  For a more pure/composable notion of async,
   *  consider scalaz-concurrent Future instead
   *  TODO this implementation is totally generic and should just be in Eff
   */
  def runFuture[A](effects: Effects.One[Future_, A])(implicit ec: ExecutionContext): Future[A] =
    effects.fold(Future.successful,
      new Forall[({ type K[X] = (Future[X] :+: CNil, Arrs.One[Future_, X, A]) ⇒ Future[A] })#K] {
        override def apply[X] = {
          (eff, cont) ⇒
            eff.eliminate(_.flatMap {
              x ⇒ runFuture(Effects.compose(cont)(x))
            }, _.impossible)
        }
      })
}