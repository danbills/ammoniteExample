import $ivy.`com.chuusai::shapeless:2.3.2`

import shapeless._

type X = String :+: Int :+: CNil

val r = Coproduct[X](4)

object FoldMe extends Poly1 {
  implicit def s = at[String] { println _ }
  implicit def i = at[Int] { println _ }
}

object FoldMe2 extends Poly2 {
  implicit def s = at[String, String] { 
    case(x,y) => println(x + y) 
  }
  implicit def i = at[String, Int] { 
    case(x,y) => println(x + y) 
  }
}

