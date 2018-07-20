import $ivy.`com.spinoco::fs2-crypto:0.2.0`

import javax.net.ssl._
import java.net.InetSocketAddress

import fs2.io.tcp.client

import spinoco.fs2.crypto._
import spinoco.fs2.crypto.io.tcp.TLSSocket

import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.spi.AsynchronousChannelProvider
import java.util.concurrent.{Executors, ThreadFactory}
import cats.effect.IO._
import scala.concurrent.ExecutionContext
import scala.{Stream => _ }
import fs2.Stream

implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

val sslCtx = SSLContext.getInstance("TLS")
sslCtx.init(null, null, null)

implicit val tcpACG: AsynchronousChannelGroup = AsynchronousChannelProvider
  .provider()
  //I have no idea what I'm doing here
  .openAsynchronousChannelGroup(Executors.newCachedThreadPool(), 8)
val ctx = SSLContext.getInstance("TLS")
ctx.init(null, null, null)

val engine = sslCtx.createSSLEngine()
engine.setUseClientMode(true)

val address = new InetSocketAddress("127.0.0.1", 6060)

client(address) flatMap { socket => Stream.eval(TLSSocket(socket, engine, executionContext)) } flatMap { tlsSocket =>

  // perform any operations with tlsSocket as you would with normal Socket.

  ???
}
