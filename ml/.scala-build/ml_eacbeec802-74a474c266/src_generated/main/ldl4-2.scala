

final class ldl4$minus2$_ {
def args = ldl4$minus2_sc.args$
def scriptPath = """ldl4-2.sc"""
/*<script>*/
//> using lib "io.github.dieproht::matr-bundle:0.0.3"

// Bring API interfaces in scope
import matr.{Matrix, MatrixFactory}
import matr.MatrBundle.given 

// Bring bundled implementations in scope
val a: Matrix[3, 4, Int] =
      MatrixFactory[3, 4, Int].fromTuple(
         (0, 8, 15, 0),
         (4, 7, 1, 1),
         (1, 2, 3, 4)
      )
// Create a Matrix of ones
val b: Matrix[3, 4, Int] = MatrixFactory[3, 4, Int].ones
// Add two matrices
val c: Matrix[3, 4, Int] = a + b
// Create a Matrix of random numbers
val d: Matrix[4, 2, Int] =
    MatrixFactory[4, 2, Int].tabulate((_, _) => scala.util.Random.nextInt(20))
// Calculate the dot product of two Matrices
val e: Matrix[3, 2, Int] = c dot d
// Transpose a Matrix
val f: Matrix[2, 3, Int] = e.transpose
// Pretty print a Matrix
println(f.mkString)


   /*
val a: Matrix[3, 4, Int] = MatrixFactory.fromTuple( 
  (0, 8, 15, 0), 
  (4, 7, 1, 1), 
  (1, 2, 3, 4) 
)

val d: Matrix[4, 2, Int] = MatrixFactory.tabulate((_, _) => scala.util.Random.nextInt(20))

val e: Matrix[3, 2, Int] = a dot d 
*/
/*</script>*/ /*<generated>*//*</generated>*/
}

object ldl4$minus2_sc {
  private var args$opt0 = Option.empty[Array[String]]
  def args$set(args: Array[String]): Unit = {
    args$opt0 = Some(args)
  }
  def args$opt: Option[Array[String]] = args$opt0
  def args$: Array[String] = args$opt.getOrElse {
    sys.error("No arguments passed to this script")
  }

  lazy val script = new ldl4$minus2$_

  def main(args: Array[String]): Unit = {
    args$set(args)
    val _ = script.hashCode() // hashCode to clear scalac warning about pure expression in statement position
  }
}

export ldl4$minus2_sc.script as `ldl4-2`

