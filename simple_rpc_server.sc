/*
Send strongly typed objects over TCP.

This demonstration serializes a nicely typed config over a simple TCP connection.

It further demonstrates an application that can be started using a single TCP port to be bound, upon which
a config is received.

After the config is received, the
 */
import java.net.{Inet4Address, InetAddress, InetSocketAddress}
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.spi.AsynchronousChannelProvider
import java.util.concurrent.{Executors, ThreadFactory}

import $ivy.`org.typelevel::mouse:0.17`
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

import mouse.all._
import scodec.{Attempt, DecodeResult}
import cats.~>
import cats.data.StateT
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

//we expect the size of the arg to be < 65 KiB, so we
val sizeCodec = C.uint32

import fs2.io.tcp.{server => tcpServer, client => tcpClient, _}

implicit val tcpACG: AsynchronousChannelGroup = AsynchronousChannelProvider
  .provider()
  //I have no idea what I'm doing here
  .openAsynchronousChannelGroup(Executors.newCachedThreadPool(), 8)

implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

// Connect once, send a serverConfig
def server[A](port: String)(implicit c: Codec[A]): Stream[IO, A] = {
  refineV[Interval.Closed[W.`1024`.T, W.`49151`.T]](port.toInt) match {
    case Right(port) =>
      tcpServer[IO](new InetSocketAddress(InetAddress.getByName(null), port.value)).flatMap {
        connection: Stream[IO, Socket[IO]] =>
          connection.flatMap {
            socket: Socket[IO] =>
              Stream.
                eval(socket.readN(4).map(_.get)).
                map(_.toVector).
                map(BitVector.apply).
                map(sizeCodec.decode).
                map(_.fold({err => throw new RuntimeException(err.messageWithContext)}, {dr => dr.value})).
                flatMap{numBytes =>
                  Stream.
                    eval(socket.readN(numBytes.toInt).map(_.get))}.
                    map(_.toVector).
                    map(BitVector.apply).map(c.decodeValue).flatMap {
                      _.fold({err => Stream.raiseError(throw new RuntimeException(err.messageWithContext))}, {
                        serverConfig =>
                          Stream.emit(serverConfig)
                      })
                    }
          }
      }
    case Left(bad) => Stream.raiseError(new RuntimeException(bad.toString))
  }
}

// Connect once, send a serverConfig
def client[A](a: A,args: String*)(implicit configCodec: Codec[A]): Unit = {
  args.toList match {
    case List(argPort, connectionPort) =>
      refineV[Interval.Closed[W.`1024`.T, W.`49151`.T]](argPort.toInt).
        foreach { port =>
          val connection = tcpClient[IO](new InetSocketAddress(InetAddress.getByName(null), connectionPort.toInt))
          val x: Stream[IO, Unit] = connection.flatMap{
            connection =>
              val writer = sendConfig(a)
              writer.flatMap(bv => Stream.eval(connection.write(Chunk.array(bv.toByteArray))) ++ Stream.eval(connection.close))
          }
          x.compile.drain.unsafeRunSync()
        }
    case _ => println("usage: amm cromwell_server.sc server [portToBeBound] [portOfServerThatWillDoTheBinding]")
  }
}

//Protocol is 4 bytes
def sendConfig[A](a: A)(implicit configCodec: Codec[A]): Stream[Pure, BitVector] = {
  val sizeCodec = C.uint32
  configCodec.encode(a) match {
    case Successful(objBytes: BitVector) =>
      sizeCodec.encode(objBytes.size.toInt) match {
        case Successful(sizeBytes) =>
          Stream.emit(sizeBytes ++ objBytes)
      }
    case x => Stream.raiseError(new RuntimeException(s"something happened $x"))
  }
}
