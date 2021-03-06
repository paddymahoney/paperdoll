package paperdoll.core.layer

import org.junit.Test
import shapeless.{:+:, CNil}
import paperdoll.scalaz.Disjunction_

class LayersTest {
	@Test def disjunction(): Unit = {
	  val _ = Layers[Disjunction_[String] :+: CNil]
	}
	def parameterized[A](): Unit = {
	  val _ = Layers[Disjunction_[A] :+: CNil]
	}
}