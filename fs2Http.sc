import $ivy.`com.spinoco::fs2-http:0.4.0`

import fs2._
//import fs2.util.syntax._
import cats.effect._
import cats.syntax.all._
import spinoco.fs2.http
import http._
import http.websocket._
import spinoco.protocol.http.header._
import spinoco.protocol.http._
import spinoco.protocol.http.header.value._

// import resources (Executor, Strategy, Asynchronous Channel Group, ...)
//import spinoco.fs2.http.Resources._

 import java.net.InetSocketAddress
 import java.util.concurrent.Executors
 import java.nio.channels.AsynchronousChannelGroup

 val ES = Executors.newCachedThreadPool(Strategy.daemonThreadFactory("ACG"))
 implicit val ACG = AsynchronousChannelGroup.withThreadPool(ES) // http.server requires a group
 implicit val S = Strategy.fromExecutor(ES) // Async (Task) requires a strategy

 def service(request: HttpRequestHeader, body: Stream[IO,Byte]): Stream[IO,HttpResponse[IO]] = {
    if (request.path != Uri.Path / "echo") Stream.emit(HttpResponse(HttpStatusCode.Ok).withUtf8Body("Hello World"))
    else {
      val ct =  request.headers.collectFirst { case `Content-Type`(ct) => ct }.getOrElse(ContentType(MediaType.`application/octet-stream`, None, None))
      val size = request.headers.collectFirst { case `Content-Length`(sz) => sz }.getOrElse(0l)
      val ok = HttpResponse(HttpStatusCode.Ok).chunkedEncoding.withContentType(ct).withBodySize(size)

      Stream.emit(ok.copy(body = body.take(size)))
    }
  }

  http.server(new InetSocketAddress("127.0.0.1", 9090))(service).run.unsafeRun()
