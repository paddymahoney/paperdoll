package paperdoll.core.layer

import org.junit.Test
import shapeless.{ CNil, :+: }
import paperdoll.scalaz.Reader_
import paperdoll.scalaz.Disjunction_

class MemberTest {
  @Test def basicFunctionality(): Unit = {
    val _1 = Member[Reader_[Int] :+: CNil, Reader_[Int]]
    val _2 = Member[Reader_[String] :+: Reader_[Int] :+: CNil, Reader_[String]]
    val _3 = Member[Reader_[String] :+: Reader_[Int] :+: CNil, Reader_[Int]]
  }

  @Test def twoParameterType(): Unit = {
    val _1 = Member[Disjunction_[String] :+: CNil, Disjunction_[String]]
    val _2 = Member[Reader_[Int] :+: Disjunction_[String] :+: CNil, Disjunction_[String]]
    val _3 = Member[Disjunction_[String] :+: Reader_[Int] :+: CNil, Disjunction_[String]]
  }
  
  def parameterized[F[_], A] = {
    val _1 = Member[Layer.Aux[F] :+: Disjunction_[A] :+: CNil, Disjunction_[A]]
    val _2 = Member[Disjunction_[A] :+: Layer.Aux[F] :+: CNil, Disjunction_[A]]
  }
}