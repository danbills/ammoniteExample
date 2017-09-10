import $ivy.`io.circe::circe-core:0.8.0`
import $ivy.`io.circe::circe-generic:0.8.0`
import $ivy.`io.circe::circe-parser:0.8.0`

import io.circe.Printer
import ammonite.ops._

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
write(wd/"file1.txt", "I am cow")


val workflowInputs = Array(
  "name" -> "world",
  "other" -> "other"
)

val workflowOptions = Array.empty[(String, String)]


import scalaj.http._

val CROMWELL_URL="http://localhost:8000"

val CROMWELL_METADATA_PARAMETERS="excludeKey=submittedFiles"
val CROMWELL_LAST_WORKFLOW_FILE="$HOME/.cromwell.last.workfow.id"

def submit = {
  //val response=$(curl -s -F wdlSource=@${1} -F workflowInputs=@${2} -F workflowOptions=@${3} ${CROMWELL_URL}/api/workflows/v1)
  //-s is silent mode
  //-F is form for posting multipart post dat
  //$(curl -s -F wdlSource=@${1} -F workflowInputs=@${2} -F workflowOptions=@${3} ${CROMWELL_URL}/api/workflows/v1)
  val json =
     Http(CROMWELL_URL + "/api/workflows/v1")
      .postMulti(
        Seq(
          "wdlSource"  -> p.pretty(wdlSource.asJson),
          "workflowInputs"   -> p.pretty(workflowInputs.asJson),
          "workflowOptions" -> p.pretty(workflowOptions.asJson)))
      .asString
      .body

  println(json)

  /*
  echo $response
  id=$(echo $response | cut -d"," -f1 | cut -d":" -f2 | sed s/\"//g | sed s/\ //g)
  echo $id > $CROMWELL_LAST_WORKFLOW_FILE
  */
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
