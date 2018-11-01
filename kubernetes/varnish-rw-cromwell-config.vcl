vcl 4.0;

backend worker {
  .host = "cromwell-worker-service.default";
  .port = "8000";
}

backend reader {
  .host = "cromwell-reader-service.default";
  .port = "8000";
}

sub vcl_recv {
    if (req.url ~ "abort/$") {
        set req.backend_hint = worker;
    } else {
        set req.backend_hint = reader;
    }
}
