import $ivy.`com.google.cloud:google-cloud-logging:1.66.0`

import com.google.cloud.MonitoredResource;
import scala.collection.JavaConverters._
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.cloud.logging.Severity;
import java.util.Collections;


// Instantiates a client
val logging = LoggingOptions.getDefaultInstance().getService();

// The name of the log to write to
val logName = "my-log";

// The data to write to the log
val text = "Hello, world!";

val entry = LogEntry.newBuilder(JsonPayload.of(Map("entry" -> text).asJava))
    .setSeverity(Severity.ERROR)
    .setLogName(logName)
    //.setResource(MonitoredResource.newBuilder("global").build())
    .setResource(MonitoredResource.of("generic_task",
      Map(
        "project_id" -> "broad-dsde-cromwell-dev",
        "location" -> "us-east4-c",
        "namespace" -> "danb-test-namespace",
        "job" -> "danb-test-logging",
        "task-id" -> "2"
        ).asJava
      ))
    .build();

// Writes the log entry asynchronously
logging.write(Collections.singleton(entry));

System.out.printf("Logged: %s%n", text);
