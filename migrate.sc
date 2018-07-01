import $ivy.`org.tpolecat::doobie-core:0.5.3`
import $ivy.`org.tpolecat::doobie-hikari:0.5.3`
import $ivy.`com.google.cloud:google-cloud-datastore:1.31.0`
import $ivy.`mysql:mysql-connector-java:8.0.11`
import $file.simple_rpc_server
import simple_rpc_server._
import doobie._
import doobie.implicits._
import cats.effect.IO
import java.time.LocalDate
import java.math.BigInteger

import scodec._
import scodec.codecs._
import _root_.scodec.codecs.implicits._
import fs2.Pure


def find(offset: Int) =
  sql"select * from METADATA_ENTRY order by METADATA_JOURNAL_ID limit 500 offset $offset".
      query[(Int, String, String, Option[String], Option[Int], Option[Int], Option[String], LocalDate, Option[String])]

case class DoobieConfig(driver: String, connectionString: String, user: String, pass: String)

implicit val c = Codec[DoobieConfig]

val counter: fs2.Stream[Pure, Int] = fs2.Stream.unfold(0){s =>
  val n = s + 500
  Some((n,n))
}

@main
def server(args: String*) =
  simple_rpc_server.server[DoobieConfig](args.head).
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
    }.unsafeRunSync()

@main def client(args: String*) = simple_rpc_server.client(DoobieConfig("com.mysql.cj.jdbc.Driver","jdbc:mysql://localhost/DatabaseName?useSSL=false","ChooseAName","YourOtherPassword"), args:_*)

//Things to do w/ server
//.observe1(_ => socket.close)
