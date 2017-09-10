import $ivy.`io.circe::circe-core:0.8.0`
import $ivy.`io.circe::circe-generic:0.8.0`
import $ivy.`io.circe::circe-parser:0.8.0`
import $ivy.`com.chuusai::shapeless:2.3.2`


import io.circe.Printer
import ammonite.ops._

import shapeless._
import syntax.std.tuple._

//import io.circe.api._
import io.circe.syntax._

import java.nio.file.Files

val p = Printer.noSpaces

/*

   submit [wdl-file] [inputs] [options]"
   status [worfklow-id]"
   metadata [worfklow-id]"
   timing [worfklow-id]"
   */

//PASTE WDL HERE
val wdlSource = """
task hello {
  String name

  command {
    echo 'Hello ${name}!'
  }
  output {
    File response = stdout()
  }
}

workflow test {
  call hello
}
"""
val wdlSourceFileName = ".source"



val workflowInputsFileName = ".inputs"
val workflowInputs = Map(
  "test.hello.name" -> "world",
  "other" -> "other"
)

println(Printer.spaces4.pretty(workflowInputs.asJson))

val workflowOptionsFileName = ".options"
val workflowOptions = Map.empty[String, String]


import scalaj.http._

val CROMWELL_URL="http://localhost:8000"

val CROMWELL_METADATA_PARAMETERS="excludeKey=submittedFiles"
val CROMWELL_LAST_WORKFLOW_FILE="$HOME/.cromwell.last.workfow.id"

def submit = {
  val json =
     Http(CROMWELL_URL + "/api/workflows/v1")
      .postMulti(
        MultiPart("workflowSource", "nomatter", "", wdlSource),
        MultiPart("workflowInputs", "nomatter2", "", p.pretty(workflowInputs.asJson)),
        MultiPart("workflowOptions", "nomatter3", "", p.pretty(Map.empty[String, String].asJson)))
      .asString
      .body

  println(json)
}

submit
/*
function status() {
  if [ -n "$1" ]; then id=$1; else id=$(cat ${CROMWELL_LAST_WORKFLOW_FILE} ); fi
  curl -s ${CROMWELL_URL}/api/workflows/v1/${id}/status;
}
function metadata() {
  if [ -n "$1" ]; then id=$1; else id=$(cat ${CROMWELL_LAST_WORKFLOW_FILE} ); fi
  curl --compressed -s ${CROMWELL_URL}/api/workflows/v1/${id}/metadata?${CROMWELL_METADATA_PARAMETERS};
}
function timing() {
  if [ -n "$1" ]; then id=$1; else id=$(cat ${CROMWELL_LAST_WORKFLOW_FILE} ); fi
  open ${CROMWELL_URL}/api/workflows/v1/${id}/timing;
}

if [ "$1" = "submit" ] ; then
   submit $2 $3 $4
elif [ "$1" = "status" ] ; then
   status $2
elif [ "$1" = "metadata" ] ; then
   metadata $2
elif [ "$1" = "timing" ] ; then
   timing $2
else
	instruct exit
fi
*/
