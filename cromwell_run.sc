val CROMWELL_JAR_PATH="/home/dan/cromwell/server/target/scala-2.12/cromwell-33-cea07d6-SNAP.jar"

val INPUTS_JSON_PATH: Option[String]= Some("/home/dan/cromwell/server/target/scala-2.12/cromwell-33-cea07d6-SNAP.jar")

val workflowOptions: Option[String] = None

object WorkflowType extends Enumeration {
  type WorkflowType = Value

  val Cwl = Value("cwl")
  val Wdl = Value("wdl")
}

import WorkflowType._

case class Arguments(workflowType: WorkflowType, inputsJson: Option[String] = None, workflowOptions: Option[String] = None)

