val CROMWELL_JAR_PATH="/home/dan/cromwell/server/target/scala-2.12/cromwell-33-cea07d6-SNAP.jar"

val INPUTS_JSON_PATH: Option[String]= Some("/home/dan/cromwell/server/target/scala-2.12/cromwell-33-cea07d6-SNAP.jar")

val workflowOptions: Option[String] = None

object WorkflowType extends Enumeration {
  type WorkflowType = Value

  val Cwl = Value("cwl")
  val Wdl = Value("wdl")
}

import WorkflowType._

case class Arguments(workflowType: WorkflowType, inputsJsonPath: Option[String] = None, workflowOptions: Option[String] = None)
/*
interp.repositories() ++= Seq(coursier.maven.MavenRepository(
  "https://broadinstitute.jfrog.io/broadinstitute/libs-release/"))

@
//import $ivy.`org.broadinstitute::cromwell:33-cea07d6-SNAP`
interp.load.ivy(coursier.Dependency(module = coursier.Module("org.broadinstitute", "cromwell_2.12", Map.empty), version = "33-cea07d6-SNAP", exclusions = Set(
  ("org.webjars", "swagger-ui"),
  ("org.mongodb", "casbah_2.12.0-RC1"),

)))

 *   not found: https://repo1.maven.org/maven2/org/webjars/swagger-ui/3.2.2/swagger-ui-3.2.2-sources.jar
  not found: https://repo1.maven.org/maven2/org/mongodb/casbah_2.12.0-RC1/3.1.1/casbah_2.12.0-RC1-3.1.1-sources.jar
  */

//submit?
