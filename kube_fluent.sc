import $ivy.`io.kubernetes:client-java:4.0.0`

/*
Copyright 2018 The Kubernetes Authors.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
import collection.JavaConverters._

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models._
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.util.Config;
import java.io.IOException;
import java.util.Arrays;

/**
 * A simple example of how to use the Java API
 *
 * <p>Easiest way to run this: mvn exec:java
 * -Dexec.mainClass="io.kubernetes.client.examples.FluentExample"
 *
 * <p>From inside $REPO_DIR/examples
 */
 def run = {
    val client = Config.defaultClient();
    Configuration.setDefaultApiClient(client);

    val api = new CoreV1Api();
    /*
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cromwell
spec:
  selector:
    matchLabels:
      app: cromwell
  strategy:
    type: Recreate
  template:
    metadata:
      labels:
        app: cromwell
    spec:
      containers:
      - image: broadinstitute/cromwell
        name: cromwell
        env:
          # Use secret in real usage
        - name: MYSQL_ROOT_PASSWORD
          value: password
        ports:
        - containerPort: 8000
          name: cromwell
	  */

    val deployment = 
	    new V1DeploymentBuilder().
            withNewMetadata().
		    withName("cromwell").
            endMetadata().
	    withNewSpec.
		    withReplicas(3).
		    withNewTemplate.
		      withNewSpec().
		         
		      endSpec().
		    endTemplate.
	    endSpec.


    val pod =
        new V1PodBuilder()
            .withNewMetadata()
            .withName("apod")
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName("www")
            .withImage("nginx")
            .endContainer()
            .endSpec()
            .build();

    api.createNamespacedPod("default", pod, null, null, null);

    val pod2 =
        new V1Pod()
            .metadata(new V1ObjectMeta().name("anotherpod"))
            .spec(
                new V1PodSpec()
                    .containers(Arrays.asList(new V1Container().name("www").image("nginx"))));

    api.createNamespacedPod("default", pod2, null, null, null);

    val list =
        api.listNamespacedPod("default", null, null, null, null, null, null, null, null, null);
    for (item <- list.getItems().asScala) {
      println(item.getMetadata().getName());
    }
}
