

final class ldl4$minus2$_ {
def args = ldl4$minus2_sc.args$
def scriptPath = """ldl4-2.sc"""
/*<script>*/
//> using lib "io.github.dieproht::matr-bundle:0.0.3"
//> using lib "org.scalanlp::breeze:2.1.0"
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

import java.io.{DataInputStream, FileInputStream}
import breeze.linalg.DenseMatrix

// IDX file format constants
val IDX_TYPE_BYTE = 0x08
val IDX_TYPE_INT = 0x0b
val MAGIC_NUMBER_IMAGES = 0x00000803
val MAGIC_NUMBER_LABELS = 0x00000801

def readImages(filename: String): DenseMatrix[Double] = {
  val stream = new DataInputStream(new FileInputStream(filename))

  // Read and validate header
  val magicNumber = stream.readInt()
  if (magicNumber != MAGIC_NUMBER_IMAGES) {
    throw new RuntimeException("Invalid IDX images file format")
  }

  val numImages = stream.readInt()
  val numRows = stream.readInt()
  val numCols = stream.readInt()

  // Create data matrix
  val data = new DenseMatrix[Double](numRows * numCols, numImages)

  // Load image data (normalize to 0-1 range)
  for {
    imageIndex <- 0 until numImages
    pixelIndex <- 0 until numRows * numCols
  } {
    data(pixelIndex, imageIndex) = stream.readUnsignedByte() / 255.0
  }

  stream.close()
  data
}

def readLabels(filename: String): Array[Int] = {
  val stream = new DataInputStream(new FileInputStream(filename))

  // Read and validate header
  val magicNumber = stream.readInt()
  if (magicNumber != MAGIC_NUMBER_LABELS) {
    throw new RuntimeException("Invalid IDX labels file format")
  }

  val numLabels = stream.readInt()

  // Create array to hold labels
  val labels = new Array[Int](numLabels)

  // Load label data
  for (i <- 0 until numLabels) {
    labels(i) = stream.readUnsignedByte()
  }

  stream.close()
  labels
}

val TRAIN_IMAGE_FILENAME =
  "/Users/dan/Downloads/MNIST_ORG/train-images.idx3-ubyte"
val TRAIN_LABEL_FILENAME =
  "/Users/dan/Downloads/MNIST_ORG/train-labels.idx1-ubyte"
val TEST_IMAGE_FILENAME =
  "/Users/dan/Downloads/MNIST_ORG/t10k-images.idx3-ubyte"
val TEST_LABEL_FILENAME =
  "/Users/dan/Downloads/MNIST_ORG/t10k-labels.idx1-ubyte"

val imageData: DenseMatrix[Double] = readImages(TRAIN_IMAGE_FILENAME)
val labels: Array[Int] = readLabels(TRAIN_IMAGE_FILENAME)

// Now you have the images in 'imageData' and labels in 'labels'
println("first label is " + labels(0))
println("second label is " + labels(1))
println("third label is " + labels(1))
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

