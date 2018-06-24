import java.net.{Inet4Address, InetAddress, InetSocketAddress}
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.spi.AsynchronousChannelProvider
import java.util.concurrent.{Executors, ThreadFactory}

import $ivy.`org.typelevel::mouse:0.17`
import mouse.all._

import $ivy.`eu.timepit::refined:0.9.0`
import $ivy.`eu.timepit::refined-scodec:0.9.0`
import $ivy.`co.fs2::fs2-core:0.10.4`
import $ivy.`co.fs2::fs2-io:0.10.4`
import $ivy.`com.chuusai::shapeless:2.3.2`
import $ivy.`org.typelevel::cats-effect:1.0.0-RC2`
import $ivy.`org.scodec::scodec-core:1.10.3`
import $ivy.`org.scodec::scodec-bits:1.1.5`
import $ivy.`org.scodec::scodec-stream:1.1.0`
import $ivy.`org.scodec::scodec-cats:0.6.0`
import $ivy.`org.typelevel::cats-core:1.0.1`
import scodec.{Attempt, DecodeResult}
//import $ivy.`org.scodec::scodec-codecs:1.10.3`
import cats.~>

import cats.syntax.apply._

import scodec.codecs._
import scodec.Attempt.{Successful, Failure}
import scodec.bits.BitVector
import scodec.stream.decode.Cursor

import eu.timepit.refined._
import eu.timepit.refined.scodec._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._
import eu.timepit.refined.types.net.UserPortNumber
import eu.timepit.refined.types.net._
import eu.timepit.refined.W
import shapeless._
import cats.Apply

import fs2._
import scala.{Stream => _ }
import fs2.Stream
import fs2.Stream._
import cats.effect.IO
import cats.effect.IO._

import scala.concurrent.ExecutionContext

import _root_.scodec.{codecs => C}
import _root_.scodec._
import _root_.scodec.codecs.implicits._
import _root_.scodec.stream.{decode => D, StreamDecoder}
import _root_.scodec.bits.ByteVector

val CROMWELL_JAR_PATH="/home/dan/cromwell/server/target/scala-2.12/cromwell-33-cea07d6-SNAP.jar"

object WorkflowType extends Enumeration {
  type WorkflowType = Value

  val Cwl = Value("cwl")
  val Wdl = Value("wdl")
}

import WorkflowType._

case class Arguments(workflowType: WorkflowType, inputsJson: Option[String] = None, workflowOptions: Option[String] = None)

case class ServerConfig(port: UserPortNumber)
case class ServerConfig2(port: Int)

implicit val c = Codec[ServerConfig]

type Config = ServerConfig :: HNil

case class CromwellStarted(port: UserPortNumber)

val configStarter = D.once[ServerConfig]

import fs2.io.tcp._
implicit val tcpACG: AsynchronousChannelGroup = AsynchronousChannelProvider
  .provider()
  //I have no idea what I'm doing here
  .openAsynchronousChannelGroup(Executors.newCachedThreadPool(), 8)

implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

@main
def mainServer(args: String*) {
  args.toList match {
    case List(argPort) =>
        refineV[Interval.Closed[W.`1024`.T, W.`49151`.T]](argPort.toInt).foreach{ port =>

          val s: Stream[IO, ServerConfig] = server[IO](new InetSocketAddress(InetAddress.getByName(null), port.value)).flatMap {
            connection: Stream[IO, Socket[IO]] =>
              val connection1: Stream[IO, ServerConfig] = connection.flatMap {
                socket: Socket[IO] =>
                  println(s"bound $port")

                  val x: Stream[IO, Chunk[Byte]] = Stream.eval(socket.readN(2).map(_.get))

                  val z: Stream[IO, Int] =
                    x.map(_.toVector).map(BitVector.apply).map(C.uint16.decode).map(_.fold({err => throw new RuntimeException(err.messageWithContext)}, {dr => dr.value}))

                  val m:Stream[IO, Chunk[Byte]] = z.flatMap{numBytes => println(s"numbytes was $numBytes"); Stream.eval(socket.readN(numBytes).map(_.get)) }

                  val y: Stream[IO, ServerConfig] = m.map(_.toVector).map(BitVector.apply).map(c.decodeValue).flatMap {
                    _.fold({
                      err => Stream.eval(IO.raiseError(throw new RuntimeException(err.messageWithContext)))
                    }, {
                      i =>
                      println(s"thing was $i")
                      Stream.emit(i).observe1(_ => socket.close)
                    })
                  }
                  y
              }


              //do something here
              Stream.eval(connection1.compile.last.map(_.get))
          }

          s.compile.last.flatMap(sc => IO{ println(s"server config was $sc")}).unsafeRunSync()
        }
    case _ => println("usage: amm cromwell_server.sc [port]")
  }
}

@main
def mainClient(args: String*): Unit = {
  args.toList match {
    case List(argPort, connectionPort) =>
      refineV[Interval.Closed[W.`1024`.T, W.`49151`.T]](argPort.toInt).
        foreach { port =>
          val connection = client[IO](new InetSocketAddress(InetAddress.getByName(null), connectionPort.toInt))
          val x: Stream[IO, Unit] = connection.flatMap{
            connection =>
              println("bound")

              val writer = c.encode(ServerConfig(port)) match {
                case Successful(s: BitVector) =>
                  C.uint16.encode(s.size.toInt) match {
                    case Successful(bytes) =>
                      Stream.eval(connection.write(Chunk.array(bytes.toByteArray))) ++
                        Stream.eval(connection.write(Chunk.array(s.toByteArray))) ++
                        Stream.eval(connection.close)
                  }
                case x => Stream.eval{IO{println(s"something happened $x")}} ++ Stream.eval(connection.close)
              }
              writer
          }
          x.compile.drain.unsafeRunSync()
        }
    case _ => println("usage: amm cromwell_server.sc [port]")
  }
}
