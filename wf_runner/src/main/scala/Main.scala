import java.sql.{Blob, Clob, Timestamp}

import cats.data.{Kleisli, ReaderT}
import cats.effect.{ExitCode, IO, IOApp}
import doobie.imports._
import fs2.Stream

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    //start to read from DB


    IO(ExitCode.Success)
  }

  type X[A] = Stream[IO, A]

  def fetchRunnableWorkflows: ReaderT[X, (String, String, String, String), WorkflowStoreEntry] = Kleisli {
    case (driver, connectionString, user, pass) =>

      val xa = Transactor.fromDriverManager[IO](driver, connectionString, user, pass)
      val finder: Stream[doobie.ConnectionIO, WorkflowStoreEntry] = find(0).stream
      xa.transP.apply(finder)
  }

  def find(offset: Int): doobie.Query0[WorkflowStoreEntry] = {

//    sql"select * from WORKFLOW_STORE_ENTRY order by SUBMISSION_TIME limit 500 offset $offset".
//      query[WorkflowStoreEntry]
    ???
  }
}

object WorkflowStoreState extends Enumeration {
  type WorkflowStoreState = Value
  val Submitted = Value("Submitted")
  val Running = Value("Running")
  val Aborting = Value("Aborting")
  val OnHold = Value("On Hold")
}
import WorkflowStoreState._

case class WorkflowStoreEntry
(
  workflowExecutionUuid: String,
  workflowDefinition: Option[Clob],
  workflowUrl: Option[String],
  workflowRoot: Option[String],
  workflowType: Option[String],
  workflowTypeVersion: Option[String],
  workflowInputs: Option[Clob],
  workflowOptions: Option[Clob],
  workflowState: WorkflowStoreState,
  submissionTime: Timestamp,
  importsZip: Option[Blob],
  customLabels: Clob,
  cromwellId: Option[String],
  heartbeatTimestamp: Option[Timestamp],
  workflowStoreEntryId: Option[Int] = None
)
