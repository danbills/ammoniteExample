interp.configureCompiler(_.settings.YpartialUnification.value = true)
interp.repositories() ++= Seq(coursier.maven.MavenRepository(
  "https://oss.sonatype.org/content/repositories/releases"))

@

import $plugin.$ivy.`org.spire-math::kind-projector:0.9.7`
import scala.concurrent.duration._

/*

Migration of (Meta)Data from Cromwell's SQL datastore to Google Datastore


Algorithm:

every N seconds,
lastRowPulled <- query datastore by auto Id desc to get last row pushed to the Datastore
newData <- query Cromwell metadata from lastRow until end of all metadata
entities <- convert data into Datastore objects known as "Entities"
push the entites into Datastore

possible optimizations to be made:
Only pull data from datastore if we see there is no more data to be pulled from MySQL
pull data from mysql and load to DS in separate threads
*/

import $ivy.`org.tpolecat::doobie-core:0.5.3`
import $ivy.`org.tpolecat::doobie-hikari:0.5.3`
import $ivy.`com.google.cloud:google-cloud-datastore:1.35.0`
import $ivy.`mysql:mysql-connector-java:8.0.11`
import $ivy.`com.chuusai::shapeless:2.3.3`
import $ivy.`org.hsqldb:hsqldb:2.4.1`

import shapeless.syntax.std.tuple._

import $file.simple_rpc_server
import simple_rpc_server._
import doobie._
import doobie.implicits._
import cats.effect.IO
import java.util.concurrent.Executors
import java.time.Instant
import scala.{Stream => _}
import java.math.BigInteger

import scodec._
import scodec.codecs._
import _root_.scodec.codecs.implicits._
import fs2.{Pure, Stream, Sink, Pipe, Scheduler, Segment, Pull}
import fs2.Stream._
import cats.effect.Effect
import cats.instances.long._
import cats.instances.vector._
import cats.data.State


import com.google.cloud.datastore.{Query => DSQuery, _}
//import com.google.cloud.datastore.Query._
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import collection.JavaConverters._
import com.google.cloud.datastore.ReadOption
import cats.Monad
import cats.syntax.monad._
import cats.syntax.traverse._
import cats.instances.list._

def datastore[F[_]: Effect]: Stream[F, Datastore] =
  Stream emit DatastoreOptions.getDefaultInstance().getService()

def queryLatest[F[_]](implicit eff : Effect[F], m: Monad[Stream[F, ?]]): Pipe[F, Datastore, List[Entity]] =
  _.flatMap{
    datastore =>
      val query = DSQuery.newEntityQueryBuilder()
        .setKind("metadata")
        .setOrderBy(OrderBy.desc("autoId"))
        .setLimit(1)
        .build();
      Stream.eval(eff.delay {datastore.run(query, Seq.empty[ReadOption]:_*).asScala.toList})
  }

def emitList[F[_]: Effect, A]: Pipe[F, Seq[A], A] = _.flatMap(Stream.emits(_))

def pullEveryMinute = {
  def fullLatestFromDatastore: Stream[IO, Long] =
    datastore[IO].through(queryLatest).through(emitList).map(_.getLong("autoId"))

  val timer =
    Scheduler.
    fromScheduledExecutorService(Executors.newScheduledThreadPool(1)).
    awakeEvery[IO](5.seconds)

    def onlyEmitIfNew: Pipe[IO, Int, Option[Int]] =
      scanState(0)({
        n =>
          State{old =>
            if (old == n)
              (n, None)
            else
              (n, Some(old))
          }})

  fullLatestFromDatastore.zipWith(timer){ case (ds, _) => ds }
}

type MetadataColumns = (Int, String, String, Option[String], Option[Int], Option[Int], Option[String], Instant, Option[String])

def find(offset: Int): doobie.Query0[MetadataColumns] =
  sql"select * from METADATA_ENTRY order by METADATA_JOURNAL_ID limit 500 offset $offset".
      query[(Int, String, String, Option[String], Option[Int], Option[Int], Option[String], Instant, Option[String])]


case class DoobieConfig(driver: String, connectionString: String, user: String, pass: String)

implicit val c = Codec[DoobieConfig]

val counter: fs2.Stream[Pure, Int] = fs2.Stream.unfold(0){s => val n = s + 500
  Some((n,n))
}

@main
def server(args: String*) = {

  val datastore = DatastoreOptions.getDefaultInstance().getService();

  val tcpServer: fs2.Stream[IO, DoobieConfig] = simple_rpc_server.server[DoobieConfig](args.head)

  tcpServer.
    flatMap{
      case DoobieConfig(driver, connectionString, user, pass) =>
        val xa = Transactor.fromDriverManager[IO](driver, connectionString, user, pass)
        val finder = find(0).stream
        xa.transP.apply(finder)
    }.map{
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
    }
    .chunkLimit(500)
    .flatMap{
      entities =>
        Stream.eval(IO {datastore.put(entities.toList:_*) })
    }.compile.drain.unsafeRunSync()
}

@main def client(args: String*) = simple_rpc_server.client(DoobieConfig("com.mysql.cj.jdbc.Driver","jdbc:mysql://localhost/DatabaseName?useSSL=false","ChooseAName","YourOtherPassword"), args:_*)

  def scanState[F[_], S, A, B](init: S)(f: A => State[S, B]): Pipe[F, A, B] =
      _.pull.scanSegments(init){
        case (previousState, segment: Segment[A, Unit]) =>
          segment.mapAccumulate(previousState)({
            case (state, o1) =>
              val stateFunc = f(o1)
              (stateFunc run state).value
          }).mapResult(_._2)
      }.stream

  /*
    driver = "org.hsqldb.jdbcDriver"
    url = "jdbc:hsqldb:file:metadata;shutdown=false;hsqldb.tx=mvcc"
    connectionTimeout = 20000
    */
def allWorkflows: doobie.Query0[String] =
  sql"select WORKFLOW_EXECUTION_UUID from WORKFLOW_STORE_ENTRY".
      query[String]

val xa = Transactor.fromDriverManager[IO]("org.hsqldb.jdbcDriver", "jdbc:hsqldb:file:metadata", "ChooseAName","YourOtherPassword")

        val finder = allWorkflows.stream
        xa.transP.apply(finder).evalMap(res => IO(println(res))).compile.drain.unsafeRunSync
