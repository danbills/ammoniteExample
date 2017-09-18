import $ivy.`io.circe::circe-core:0.8.0`
import $ivy.`io.circe::circe-generic:0.8.0`
import $ivy.`io.circe::circe-parser:0.8.0`
import $ivy.`com.chuusai::shapeless:2.3.2`

import io.circe.Printer
import ammonite.ops._

import shapeless._
import syntax.std.tuple._

import io.circe.syntax._
import io.circe._
import io.circe.parser._
import io.circe.generic.auto._


import java.nio.file.Files

val p = Printer.noSpaces

/*

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
val workflowInputs = Map(
  "test.hello.name" -> "world",
  "other" -> "other"
)

val workflowOptions = Map.empty[String, String]


import scalaj.http._

val CROMWELL_URL="http://localhost:8000"

val CROMWELL_METADATA_PARAMETERS="excludeKey=submittedFiles"
val CROMWELL_LAST_WORKFLOW_FILE="$HOME/.cromwell.last.workfow.id"

def submit: String = {
     Http(CROMWELL_URL + "/api/workflows/v2")
      .postMulti(
        MultiPart("workflowSource", "nomatter", "", wdlSource),
        MultiPart("workflowInputs", "nomatter2", "", p.pretty(workflowInputs.asJson)),
        MultiPart("workflowOptions", "nomatter3", "", p.pretty(Map.empty[String, String].asJson)))
      .asString
      .body
}

case class Submittal(id: String, status: String)

def readSubmittal(in: String) =
  decode[Submittal](in)

def status(id: String) =
     Http(CROMWELL_URL + s"/api/workflows/v2/$id/status").
       asString.
       body

def metadata(id: String) = {
  Http(CROMWELL_URL + s"/api/workflows/v2/$id/metadata").
    header("Accept", "application/json").
    asString.
    body
}

def timing(id: String) = {
  val exec = s"open $CROMWELL_URL/api/workflows/v2/$id/timing"
  //%(exec)
  //%%(exec)
  //Http().
    //asString.
    //body
}

/** Submit */
val submittal = readSubmittal(submit)
println(submittal)
Thread.sleep(5000)

/** Metadata */
//val data = submittal.map(_.id).map(metadata)

/** Status */
val data = submittal.map(_.id).map(status)

/** Timing */
//val data = submittal.map(_.id).map(timing)

println(data)

/*
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
