vcl 4.0;
/*
cromwell-reader-service   ClusterIP      10.11.254.191   <none>          8000/TCP       7s
cromwell-service          ClusterIP      None            <none>          8000/TCP       6d
cromwell-worker-service   ClusterIP      10.11.251.106   <none>          8000/TCP       11s

*/
backend worker {
  .host = "10.11.251.106";
  .port = "8000";
}

backend reader {
  .host = "10.11.254.191";
  .port = "8000";
}

sub vcl_recv {
    if (req.url ~ "abort/$") {
        set req.backend_hint = worker;
    } else {
        set req.backend_hint = reader;
    }
}
