package paperdoll.scalaz

import scalaz.EitherT
import shapeless.{ :+:, CNil }
import paperdoll.core.effect.Eff
import paperdoll.core.layer.Layer
import paperdoll.core.layer.Layers
import scalaz.Disjunction
import scalaz.syntax.monad._
import paperdoll.core.layer.Subset
import paperdoll.core.layer.Member

object EitherTLayer {
  def send[F[_], A, B](et: EitherT[F, A, B]): Eff[Layer.Aux[F] :+: Disjunction_[A] :+: CNil, Layers[Layer.Aux[F] :+: Disjunction_[A] :+: CNil] {
    type O[X] = F[X] :+: Disjunction[A, X] :+: CNil
  }, B] = Eff.send[Layer.Aux[F], Disjunction[A, B]](et.run).extend[Layer.Aux[F] :+: Disjunction_[A] :+: CNil]().flatMap {
    eab =>
      Eff.send[Disjunction_[A], B](eab)
        .extend[Layer.Aux[F] :+: Disjunction_[A] :+: CNil]
        .apply[Layers.One[Disjunction_[A]]]()(
          (Predef.??? : Subset[Layer.Aux[F] :+: Disjunction_[A] :+: CNil, Disjunction_[A] :+: CNil] {
            type LT = Layers.One[Disjunction_[A]]
            type LS = Layers[Layer.Aux[F] :+: Disjunction_[A] :+: CNil] {
              type O[X] = F[X] :+: Disjunction[A, X] :+: CNil
            }
          }),
          //        Subset.consSubset[Layer.Aux[F] :+: Disjunction_[A] :+: CNil, Disjunction_[A], Layers.One[Disjunction_[A]], CNil, Layers.One[Disjunction_[A]]](
          //            /*Member.cons[Layer.Aux[F], Disjunction_[A] :+: CNil, Disjunction_[A]], Subset.nilSubset[Layer.Aux[F] :+: Disjunction_[A] :+: CNil]*/ ???,
          //            implicitly, implicitly
          //        )
          implicitly)
  }
}