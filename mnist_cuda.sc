//> using dep "org.bytedeco:cuda-platform:12.6-9.5-1.5.11"
//> using dep "org.bytedeco:cuda:12.6-9.5-1.5.11"
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer

import org.bytedeco.javacpp.{BytePointer, FloatPointer, Pointer, SizeTPointer}
import org.bytedeco.cuda.cublas._
import org.bytedeco.cuda.cudart._
import org.bytedeco.cuda.cudnn._
import org.bytedeco.cuda.global.cublas._
import org.bytedeco.cuda.global.cudart._
import org.bytedeco.cuda.global.cudnn._

val IMAGE_H = 28
val IMAGE_W = 28

val first_image  = "one_28x28.pgm"
val second_image = "three_28x28.pgm"
val third_image  = "five_28x28.pgm"

val conv1_bin      = "conv1.bin"
val conv1_bias_bin = "conv1.bias.bin"
val conv2_bin      = "conv2.bin"
val conv2_bias_bin = "conv2.bias.bin"
val ip1_bin        = "ip1.bin"
val ip1_bias_bin   = "ip1.bias.bin"
val ip2_bin        = "ip2.bin"
val ip2_bias_bin   = "ip2.bias.bin"

val EXIT_FAILURE = 1
val EXIT_SUCCESS = 0
val EXIT_WAIVED  = 0

def FatalError(s: String): Unit = {
  System.err.println(s)
  Thread.dumpStack()
  System.err.println("Aborting...")
  cudaDeviceReset()
  System.exit(EXIT_FAILURE)
}

def checkCUDNN(status: Int): Unit = {
  if (status != CUDNN_STATUS_SUCCESS) {
    FatalError("CUDNN failure: " + status)
  }
}

def checkCudaErrors(status: Int): Unit = {
  if (status != 0) {
    FatalError("Cuda failure: " + status)
  }
}

def get_path(fname: String, pname: String): String = {
  "data/" + fname
}

class Layer_t(
    _inputs: Int,
    _outputs: Int,
    _kernel_dim: Int,
    fname_weights: String,
    fname_bias: String,
    pname: String
) {
  val inputs: Int                 = _inputs
  val outputs: Int                = _outputs
  val kernel_dim: Int             = _kernel_dim
  val data_h: Array[FloatPointer] = Array(new FloatPointer())
  val data_d: Array[FloatPointer] = Array(new FloatPointer())
  val bias_h: Array[FloatPointer] = Array(new FloatPointer())
  val bias_d: Array[FloatPointer] = Array(new FloatPointer())

  val weights_path: String =
    if (pname != null) get_path(fname_weights, pname) else fname_weights
  val bias_path: String =
    if (pname != null) get_path(fname_bias, pname) else fname_bias

  readBinaryFile(
    weights_path,
    inputs * outputs * kernel_dim * kernel_dim,
    data_h,
    data_d
  )
  readBinaryFile(bias_path, outputs, bias_h, bias_d)

  def release(): Unit = {
    checkCudaErrors(cudaFree(data_d(0)))
  }

  private def readBinaryFile(
      fname: String,
      size: Int,
      data_h: Array[FloatPointer],
      data_d: Array[FloatPointer]
  ): Unit = {
    try {
      val stream = new FileInputStream(fname)
      val size_b = size * java.lang.Float.BYTES
      val data   = Array.ofDim[Byte](size_b)
      if (stream.read(data) < size_b) {
        FatalError("Error reading file " + fname)
      }
      stream.close()
      data_h(0) = new FloatPointer(new BytePointer(ByteBuffer.wrap(data)))
      data_d(0) = new FloatPointer()

      checkCudaErrors(cudaMalloc(data_d(0), size_b))
      checkCudaErrors(
        cudaMemcpy(data_d(0), data_h(0), size_b, cudaMemcpyHostToDevice)
      )
    } catch {
      case e: IOException =>
        FatalError("Error opening file " + fname)
    }
  }
}

def printDeviceVector(size: Int, vec_d: FloatPointer): Unit = {
  val vec = new FloatPointer(size)
  cudaDeviceSynchronize()
  cudaMemcpy(vec, vec_d, size * java.lang.Float.BYTES, cudaMemcpyDeviceToHost)
  for (i <- 0 until size) {
    print(vec.get(i) + " ")
  }
  println()
}

class network_t {
  val dataType: Int                         = CUDNN_DATA_FLOAT
  val tensorFormat: Int                     = CUDNN_TENSOR_NCHW
  val cudnnHandle: cudnnContext             = new cudnnContext()
  val srcTensorDesc: cudnnTensorStruct      = new cudnnTensorStruct()
  val dstTensorDesc: cudnnTensorStruct      = new cudnnTensorStruct()
  val biasTensorDesc: cudnnTensorStruct     = new cudnnTensorStruct()
  val filterDesc: cudnnFilterStruct         = new cudnnFilterStruct()
  val convDesc: cudnnConvolutionStruct      = new cudnnConvolutionStruct()
  val activationDesc: cudnnActivationStruct = new cudnnActivationStruct()
  val poolingDesc: cudnnPoolingStruct       = new cudnnPoolingStruct()
  val cublasHandle: cublasContext           = new cublasContext()

  createHandles()

  def createHandles(): Unit = {
    checkCUDNN(cudnnCreate(cudnnHandle))
    checkCUDNN(cudnnCreateTensorDescriptor(srcTensorDesc))
    checkCUDNN(cudnnCreateTensorDescriptor(dstTensorDesc))
    checkCUDNN(cudnnCreateTensorDescriptor(biasTensorDesc))
    checkCUDNN(cudnnCreateFilterDescriptor(filterDesc))
    checkCUDNN(cudnnCreateConvolutionDescriptor(convDesc))
    checkCUDNN(cudnnCreateActivationDescriptor(activationDesc))
    checkCUDNN(cudnnCreatePoolingDescriptor(poolingDesc))

    checkCudaErrors(cublasCreate_v2(cublasHandle))
  }

  def destroyHandles(): Unit = {
    checkCUDNN(cudnnDestroyPoolingDescriptor(poolingDesc))
    checkCUDNN(cudnnDestroyActivationDescriptor(activationDesc))
    checkCUDNN(cudnnDestroyConvolutionDescriptor(convDesc))
    checkCUDNN(cudnnDestroyFilterDescriptor(filterDesc))
    checkCUDNN(cudnnDestroyTensorDescriptor(srcTensorDesc))
    checkCUDNN(cudnnDestroyTensorDescriptor(dstTensorDesc))
    checkCUDNN(cudnnDestroyTensorDescriptor(biasTensorDesc))
    checkCUDNN(cudnnDestroy(cudnnHandle))

    checkCudaErrors(cublasDestroy_v2(cublasHandle))
  }

  def release(): Unit = {
    destroyHandles()
  }

  def resize(size: Int, data: FloatPointer): Unit = {
    if (!data.isNull) {
      checkCudaErrors(cudaFree(data))
    }
    checkCudaErrors(cudaMalloc(data, size * java.lang.Float.BYTES))
  }

  def addBias(
      dstTensorDesc: cudnnTensorStruct,
      layer: Layer_t,
      c: Int,
      data: FloatPointer
  ): Unit = {
    checkCUDNN(
      cudnnSetTensor4dDescriptor(
        biasTensorDesc,
        tensorFormat,
        dataType,
        1,
        c,
        1,
        1
      )
    )
    val alpha = new FloatPointer(1.0f)
    val beta  = new FloatPointer(1.0f)
    checkCUDNN(
      cudnnAddTensor(
        cudnnHandle,
        alpha,
        biasTensorDesc,
        layer.bias_d(0),
        beta,
        dstTensorDesc,
        data
      )
    )
  }

  def fullyConnectedForward(
      ip: Layer_t,
      n: Array[Int],
      c: Array[Int],
      h: Array[Int],
      w: Array[Int],
      srcData: FloatPointer,
      dstData: FloatPointer
  ): Unit = {
    if (n(0) != 1) {
      FatalError("Not Implemented")
    }
    val dim_x = c(0) * h(0) * w(0)
    val dim_y = ip.outputs
    resize(dim_y, dstData)

    val alpha = new FloatPointer(1.0f)
    val beta  = new FloatPointer(1.0f)
// place bias into dstData
    checkCudaErrors(
      cudaMemcpy(
        dstData,
        ip.bias_d(0),
        dim_y * java.lang.Float.BYTES,
        cudaMemcpyDeviceToDevice
      )
    )

    checkCudaErrors(
      cublasSgemv_v2(
        cublasHandle,
        CUBLAS_OP_T,
        dim_x,
        dim_y,
        alpha,
        ip.data_d(0),
        dim_x,
        srcData,
        1,
        beta,
        dstData,
        1
      )
    )

    h(0) = 1
    w(0) = 1
    c(0) = dim_y
  }

  def convoluteForward(
      conv: Layer_t,
      n: Array[Int],
      c: Array[Int],
      h: Array[Int],
      w: Array[Int],
      srcData: FloatPointer,
      dstData: FloatPointer
  ): Unit = {
    val algo: Array[Int] = Array(0)
    val algoPerf: cudnnConvolutionFwdAlgoPerf_t =
      new cudnnConvolutionFwdAlgoPerf_t(1)

    checkCUDNN(
      cudnnSetTensor4dDescriptor(
        srcTensorDesc,
        tensorFormat,
        dataType,
        n(0),
        c(0),
        h(0),
        w(0)
      )
    )

    checkCUDNN(
      cudnnSetFilter4dDescriptor(
        filterDesc,
        dataType,
        tensorFormat,
        conv.outputs,
        conv.inputs,
        conv.kernel_dim,
        conv.kernel_dim
      )
    )

    checkCUDNN(
      cudnnSetConvolution2dDescriptor(
        convDesc,
        0,
        0, // padding
        1,
        1, // stride
        1,
        1, // upscale
        CUDNN_CROSS_CORRELATION,
        dataType
      )
    )
// find dimension of convolution output
    checkCUDNN(
      cudnnGetConvolution2dForwardOutputDim(
        convDesc,
        srcTensorDesc,
        filterDesc,
        n,
        c,
        h,
        w
      )
    )

    checkCUDNN(
      cudnnSetTensor4dDescriptor(
        dstTensorDesc,
        tensorFormat,
        dataType,
        n(0),
        c(0),
        h(0),
        w(0)
      )
    )
    checkCUDNN(
      cudnnGetConvolutionForwardAlgorithm_v7(
        cudnnHandle,
        srcTensorDesc,
        filterDesc,
        convDesc,
        dstTensorDesc,
        1,
        algo,
        algoPerf
      )
    )
    resize(n(0) * c(0) * h(0) * w(0), dstData)
    val sizeInBytes: SizeTPointer = new SizeTPointer(1)
    val workSpace: Pointer        = new Pointer()
    checkCUDNN(
      cudnnGetConvolutionForwardWorkspaceSize(
        cudnnHandle,
        srcTensorDesc,
        filterDesc,
        convDesc,
        dstTensorDesc,
        algoPerf.algo(),
        sizeInBytes
      )
    )
    if (sizeInBytes.get(0) != 0) {
      checkCudaErrors(cudaMalloc(workSpace, sizeInBytes.get(0)))
    }
    val alpha = new FloatPointer(1.0f)
    val beta  = new FloatPointer(0.0f)
    checkCUDNN(
      cudnnConvolutionForward(
        cudnnHandle,
        alpha,
        srcTensorDesc,
        srcData,
        filterDesc,
        conv.data_d(0),
        convDesc,
        algo(0),
        workSpace,
        sizeInBytes.get(0),
        beta,
        dstTensorDesc,
        dstData
      )
    )
    addBias(dstTensorDesc, conv, c(0), dstData)
    if (sizeInBytes.get(0) != 0) {
      checkCudaErrors(cudaFree(workSpace))
    }
  }

  def poolForward(
      n: Array[Int],
      c: Array[Int],
      h: Array[Int],
      w: Array[Int],
      srcData: FloatPointer,
      dstData: FloatPointer
  ): Unit = {
    checkCUDNN(
      cudnnSetPooling2dDescriptor(
        poolingDesc,
        CUDNN_POOLING_MAX,
        CUDNN_PROPAGATE_NAN,
        2,
        2, // window
        0,
        0, // padding
        2,
        2 // stride
      )
    )
    checkCUDNN(
      cudnnSetTensor4dDescriptor(
        srcTensorDesc,
        tensorFormat,
        dataType,
        n(0),
        c(0),
        h(0),
        w(0)
      )
    )
    h(0) = h(0) / 2
    w(0) = w(0) / 2
    checkCUDNN(
      cudnnSetTensor4dDescriptor(
        dstTensorDesc,
        tensorFormat,
        dataType,
        n(0),
        c(0),
        h(0),
        w(0)
      )
    )
    resize(n(0) * c(0) * h(0) * w(0), dstData)
    val alpha = new FloatPointer(1.0f)
    val beta  = new FloatPointer(0.0f)
    checkCUDNN(
      cudnnPoolingForward(
        cudnnHandle,
        poolingDesc,
        alpha,
        srcTensorDesc,
        srcData,
        beta,
        dstTensorDesc,
        dstData
      )
    )
  }

  def softmaxForward(
      n: Int,
      c: Int,
      h: Int,
      w: Int,
      srcData: FloatPointer,
      dstData: FloatPointer
  ): Unit = {
    resize(n * c * h * w, dstData)

    checkCUDNN(
      cudnnSetTensor4dDescriptor(
        srcTensorDesc,
        tensorFormat,
        dataType,
        n,
        c,
        h,
        w
      )
    )
    checkCUDNN(
      cudnnSetTensor4dDescriptor(
        dstTensorDesc,
        tensorFormat,
        dataType,
        n,
        c,
        h,
        w
      )
    )
    val alpha = new FloatPointer(1.0f)
    val beta  = new FloatPointer(0.0f)
    checkCUDNN(
      cudnnSoftmaxForward(
        cudnnHandle,
        CUDNN_SOFTMAX_ACCURATE,
        CUDNN_SOFTMAX_MODE_CHANNEL,
        alpha,
        srcTensorDesc,
        srcData,
        beta,
        dstTensorDesc,
        dstData
      )
    )
  }

  def activationForward(
      n: Int,
      c: Int,
      h: Int,
      w: Int,
      srcData: FloatPointer,
      dstData: FloatPointer
  ): Unit = {
    resize(n * c * h * w, dstData)
    checkCUDNN(
      cudnnSetTensor4dDescriptor(
        srcTensorDesc,
        tensorFormat,
        dataType,
        n,
        c,
        h,
        w
      )
    )
    checkCUDNN(
      cudnnSetTensor4dDescriptor(
        dstTensorDesc,
        tensorFormat,
        dataType,
        n,
        c,
        h,
        w
      )
    )
    checkCUDNN(
      cudnnSetActivationDescriptor(
        activationDesc,
        CUDNN_ACTIVATION_RELU,
        CUDNN_PROPAGATE_NAN,
        0
      )
    )
    val alpha = new FloatPointer(1.0f)
    val beta  = new FloatPointer(0.0f)
    checkCUDNN(
      cudnnActivationForward(
        cudnnHandle,
        activationDesc,
        alpha,
        srcTensorDesc,
        srcData,
        beta,
        dstTensorDesc,
        dstData
      )
    )
  }

  def classify_example(
      fname: String,
      conv1: Layer_t,
      conv2: Layer_t,
      ip1: Layer_t,
      ip2: Layer_t
  ): Int = {
    val n: Array[Int]           = Array(0)
    val c: Array[Int]           = Array(0)
    val h: Array[Int]           = Array(0)
    val w: Array[Int]           = Array(0)
    val srcData: FloatPointer   = new FloatPointer()
    val dstData: FloatPointer   = new FloatPointer()
    val imgData_h: FloatPointer = new FloatPointer(IMAGE_H * IMAGE_W)

// load gray-scale image from disk
    println("Loading image " + fname)
    try {
// declare a host image object for an 8-bit grayscale image
      val oHostSrc = new FileInputStream(fname)
      var lines    = 0
      while (lines < 4) {
        // skip header, comment, width, height, and max value
        if (oHostSrc.read() == '\n') {
          lines += 1
        }
      }

// Plot to console and normalize image to be in range [0,1]
      for (i <- 0 until IMAGE_H) {
        for (j <- 0 until IMAGE_W) {
          val idx = IMAGE_W * i + j
          imgData_h.put(idx, oHostSrc.read() / 255.0f)
        }
      }
      oHostSrc.close()
    } catch {
      case rException: IOException =>
        FatalError(rException.toString)
    }

    println("Performing forward propagation ...")

    checkCudaErrors(
      cudaMalloc(srcData, IMAGE_H * IMAGE_W * java.lang.Float.BYTES)
    )
    checkCudaErrors(
      cudaMemcpy(
        srcData,
        imgData_h,
        IMAGE_H * IMAGE_W * java.lang.Float.BYTES,
        cudaMemcpyHostToDevice
      )
    )

    n(0) = 1
    c(0) = 1
    h(0) = IMAGE_H
    w(0) = IMAGE_W
    convoluteForward(conv1, n, c, h, w, srcData, dstData)
    poolForward(n, c, h, w, dstData, srcData)

    convoluteForward(conv2, n, c, h, w, srcData, dstData)
    poolForward(n, c, h, w, dstData, srcData)

    fullyConnectedForward(ip1, n, c, h, w, srcData, dstData)
    activationForward(n(0), c(0), h(0), w(0), dstData, srcData)

    fullyConnectedForward(ip2, n, c, h, w, srcData, dstData)
    softmaxForward(n(0), c(0), h(0), w(0), dstData, srcData)

    val max_digits           = 10
    val result: FloatPointer = new FloatPointer(max_digits)
    checkCudaErrors(
      cudaMemcpy(
        result,
        srcData,
        max_digits * java.lang.Float.BYTES,
        cudaMemcpyDeviceToHost
      )
    )
    var id = 0
    for (i <- 1 until max_digits) {
      if (result.get(id) < result.get(i)) id = i
    }

    println("Resulting weights from Softmax:")
    printDeviceVector(n(0) * c(0) * h(0) * w(0), srcData)

    checkCudaErrors(cudaFree(srcData))
    checkCudaErrors(cudaFree(dstData))
    id
  }
}

if (args.length > 1) {
  println("Test usage:\njava MNISTCUDNN [image]\nExiting...")
  System.exit(EXIT_FAILURE)
}

val mnist: network_t = new network_t()
val name: String     = "myname"

val conv1: Layer_t = new Layer_t(1, 20, 5, conv1_bin, conv1_bias_bin, name)
val conv2: Layer_t = new Layer_t(20, 50, 5, conv2_bin, conv2_bias_bin, name)
val ip1: Layer_t   = new Layer_t(800, 500, 1, ip1_bin, ip1_bias_bin, name)
val ip2: Layer_t   = new Layer_t(500, 10, 1, ip2_bin, ip2_bias_bin, name)

if (args.length == 0) {
  val image_path1: String = get_path(first_image, name)
  val i1: Int = mnist.classify_example(image_path1, conv1, conv2, ip1, ip2)

  val image_path2: String = get_path(second_image, name)
  val i2: Int = mnist.classify_example(image_path2, conv1, conv2, ip1, ip2)

  val image_path3: String = get_path(third_image, name)
  val i3: Int = mnist.classify_example(image_path3, conv1, conv2, ip1, ip2)

  println("\nResult of classification: " + i1 + " " + i2 + " " + i3)
  if (i1 != 1 || i2 != 3 || i3 != 5) {
    println("\nTest failed!")
    FatalError("Prediction mismatch")
  } else {
    println("\nTest passed!")
  }
} else {
  val i1: Int = mnist.classify_example(args(0), conv1, conv2, ip1, ip2)
  println("\nResult of classification: " + i1)
}
cudaDeviceReset()
System.exit(EXIT_SUCCESS)
