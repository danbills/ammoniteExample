
curl -X POST http://127.0.0.1:8001/api/v1/namespaces/default/services/$1-cromwell:http/proxy/api/workflows/v1 -H "accept: application/json" -H "Content-Type: multipart/form-data" -F "workflowSource=@hello.wdl" -F "workflowInputs=@hello.inputs;type=application/json"
