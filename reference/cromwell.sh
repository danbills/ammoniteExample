#! /bin/bash

This=`basename $0`
BinDir=`dirname $0`

# FIXME: read env var, or default with this
export CROMWELL_URL="http://localhost:8000"

CROMWELL_METADATA_PARAMETERS="excludeKey=submittedFiles"
CROMWELL_LAST_WORKFLOW_FILE="$HOME/.cromwell.last.workfow.id"

instruct()  # {{{
{
   echo
   echo "Pokes Cromwell REST Endpoints at a server specified by the shell var CROMWELL_URL" 
   echo
   echo "Usage:    $This <subcommand> [options]"
   echo
   echo "NOTE: As a convenience, f you omit a workflow-id from a command, the last used workflow-id "
   echo "will be used as a default.  When submitting a workflow, the returned worfklow-id "
   echo "will become the last used workflow id"
   echo
   echo "Supported Subcommands:"
   echo "   submit [wdl-file] [inputs] [options]"
   echo "   status [worfklow-id]"
   echo "   metadata [worfklow-id]"
   echo "   timing [worfklow-id]"
   echo
   eval $1
}  # }}}

function submit() { 
  response=$(curl -vv -F wdlSource=@${1} -F workflowInputs=@${2} -F workflowOptions=@${3} ${CROMWELL_URL}/api/workflows/v1)
  echo $response  
  id=$(echo $response | cut -d"," -f1 | cut -d":" -f2 | sed s/\"//g | sed s/\ //g)
  echo $id > $CROMWELL_LAST_WORKFLOW_FILE
}
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

