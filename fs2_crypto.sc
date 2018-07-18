import $ivy.`com.spinoco::fs2-crypto:0.2.0`

import javax.net.ssl._
import java.net.InetSocketAddress

import fs2.io.tcp.client

import spinoco.fs2.crypto._
import spinoco.fs2.crypto.io.tcp.TLSSocket

val ctx = SSLContext.getInstance("TLS")
ctx.init(null, null, null)

val engine = sslCtx.createSSLEngine()
engine.setUseClientMode(true)

val address = new InetSocketAddress("127.0.0.1", 6060)

client(address) flatMap { socket => TLSSocket(socket, engine) } flatMap { tlsSocket =>

  // perform any operations with tlsSocket as you would with normal Socket.

  ???
}
