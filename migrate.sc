/*

Migration of (Meta)Data from Cromwell's SQL datastore to Google Datastore

Algorithm:
lastRowPulled <- query datastore by auto Id desc to get last row pushed to the Datastore
newData <- query Cromwell metadata
entities <- convert data into Datastore objects known as "Entities"
push the entites into Datastore

*/
import $ivy.`org.tpolecat::doobie-core:0.5.3`
import $ivy.`org.tpolecat::doobie-hikari:0.5.3`
import $ivy.`com.google.cloud:google-cloud-datastore:1.31.0`
import $ivy.`mysql:mysql-connector-java:8.0.11`
import $file.simple_rpc_server
import simple_rpc_server._
import doobie._
import doobie.implicits._
import cats.effect.IO
import java.time.Instant
import scala.{Stream => _}
import java.math.BigInteger

import scodec._
import scodec.codecs._
import _root_.scodec.codecs.implicits._
import fs2.Pure


import com.google.cloud.datastore._



def find(offset: Int) =
  sql"select * from METADATA_ENTRY order by METADATA_JOURNAL_ID limit 500 offset $offset".
      query[(Int, String, String, Option[String], Option[Int], Option[Int], Option[String], Instant, Option[String])]

case class DoobieConfig(driver: String, connectionString: String, user: String, pass: String)

implicit val c = Codec[DoobieConfig]

val counter: fs2.Stream[Pure, Int] = fs2.Stream.unfold(0){s =>
  val n = s + 500
  Some((n,n))
}

@main
def server(args: String*) = {

  val datastore = DatastoreOptions.getDefaultInstance().getService();



  val tcpServer: fs2.Stream[IO, DoobieConfig] = simple_rpc_server.server[DoobieConfig](args.head)

  tcpServer.
    take(1).
    compile.
    last.
    map(_.get).
    flatMap{
      case DoobieConfig(driver, connectionString, user, pass) =>
        val xa = Transactor.fromDriverManager[IO](driver, connectionString, user, pass)
        find(0).
          stream.
          //take(1).
          compile.toList.
          transact(xa)
    }.map{
      list => list map {
      case tuple@(autoId, wfId, metadataKey, optCallFQN, optJobScatterIndex, optJobRetryAttempt, optMetadataValue, timestamp, optValueType) =>
        val key = tuple.hashCode.toString
        val taskKey = datastore.newKeyFactory().setKind("metadata").newKey(key)
        import com.google.cloud.Timestamp

        val ts = Timestamp.ofTimeSecondsAndNanos(timestamp.getEpochSecond,  timestamp.getNano)

        val builder = Entity.newBuilder(taskKey)
          .set("autoId", autoId)
          .set("wfId", wfId)
          .set("metadataKey", metadataKey)
          .set("timestamp", ts)
        optCallFQN.foreach(builder.set("callFQN", _))
        optJobScatterIndex.foreach(builder.set("jobScatterIndex", _))
        optJobRetryAttempt.foreach(builder.set("jobRetryAttempt", _))
        optMetadataValue.foreach(builder.set("metadataValue", _))
        optValueType.foreach(builder.set("valueType", _))

        builder.build()
    }}
    .flatMap{
      entities =>
        IO {datastore.put(entities:_*) }
    }.unsafeRunSync()
}

@main def client(args: String*) = simple_rpc_server.client(DoobieConfig("com.mysql.cj.jdbc.Driver","jdbc:mysql://localhost/DatabaseName?useSSL=false","ChooseAName","YourOtherPassword"), args:_*)

//Things to do w/ server
//.observe1(_ => socket.close)
