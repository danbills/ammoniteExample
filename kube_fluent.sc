interp.configureCompiler(_.settings.YpartialUnification.value = true)

import $ivy.`io.kubernetes:client-java:5.0.0`
import io.kubernetes.client.proto.V1Apps
import $ivy.`org.typelevel::cats-effect:1.3.1`
import cats.implicits._
import cats.instances._
import io.kubernetes.client.ApiCallback


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
import io.kubernetes.client.custom.Quantity
import io.kubernetes.client.models.{V1ConfigMapBuilder, V1Deployment, V1DeploymentBuilder, V1PersistentVolumeClaimBuilder, V1Service, V1ServiceBuilder}
import collection.JavaConverters._
import cats.data.IndexedReaderWriterStateT
import cats.data.Chain
import cats.data.ReaderT
import cats.data.Reader
import cats.effect.IO

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.apis.AppsV1Api
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
val service: V1Service =
  new V1ServiceBuilder().
    withNewMetadata().withName("cromwell-service").endMetadata().
    withNewSpec().addNewPort().withPort(8000).endPort().addToSelector("app", "cromwell").endSpec().
    build()

val worker: V1Deployment =
  new V1DeploymentBuilder().
    withNewMetadata().withNewName("cromwell").endMetadata().
    withNewSpec.
    withReplicas(1).
    withNewSelector().addToMatchLabels("app", "cromwell").endSelector().
    withNewTemplate.
    withNewMetadata().addToLabels("app", "cromwell").endMetadata().
    withNewSpec().
    addNewVolume().withName("config-volume").withNewConfigMap.withName("cromwell-conf").endConfigMap().endVolume().
    addNewContainer().
    withImage("broadinstitute/cromwell:44").
    withName("cromwell").
    addNewEnv().withName("CROMWELL_ARGS").withValue("server").endEnv().
    addNewEnv().withName("JAVA_OPTS").withValue("-Dconfig.file=/conf/cromwell.conf").endEnv().
    addNewVolumeMount().withMountPath("/conf").withName("config-volume").endVolumeMount().
    endContainer().
    endSpec().
    endTemplate.
    endSpec.
    build()


val mysqlDeployment: V1Deployment =
  new V1DeploymentBuilder().
    withNewMetadata().withNewName("mysql").
    endMetadata().
    withNewSpec.
    withReplicas(1).
    withNewSelector().addToMatchLabels("app", "mysql").endSelector().

    withNewTemplate.
    withNewMetadata().addToLabels("app", "mysql").endMetadata().
    withNewSpec().
    addNewContainer().
    withImage("mysql:5.5").
    withName("mysql").
    addNewEnv().withName("MYSQL_ROOT_PASSWORD").withValue("cromwell").endEnv().
    addNewEnv().withName("MYSQL_USER").withValue("cromwell").endEnv().
    addNewEnv().withName("MYSQL_PASSWORD").withValue("cromwell").endEnv().
    addNewEnv().withName("MYSQL_DATABASE").withValue("cromwell").endEnv().
    addNewPort().withContainerPort(3306).endPort().
    addNewVolumeMount().withMountPath("/var/lib/mysql").withName("mysql-persistent-storage").endVolumeMount().
    endContainer().
    addNewVolume().withName("mysql-persistent-storage").withNewPersistentVolumeClaim().withClaimName("mysql-pv-claim2").endPersistentVolumeClaim().endVolume().
    endSpec().
    endTemplate.
    endSpec.
    build()

val mysqlService = new V1ServiceBuilder().
  withNewMetadata().withName("mysql-service").endMetadata().
  withNewSpec().addNewPort().withPort(3306).endPort().addToSelector("app", "mysql").endSpec().
  build()

val pvc = new V1PersistentVolumeClaimBuilder().withNewMetadata().withName("mysql-pv-claim2").endMetadata().
  withNewSpec().withAccessModes("ReadWriteOnce").withNewResources().addToRequests("storage", new Quantity("20Gi")).endResources().endSpec().build()

val cfg = new V1ConfigMapBuilder().
  withNewMetadata().withName("cromwell-conf").endMetadata().
  withData(Map("cromwell.conf" -> conf).asJava).build()

def conf =
  """
    |database {
    |  profile = "slick.jdbc.MySQLProfile$"
    |  db {
    |    driver = "com.mysql.cj.jdbc.Driver"
    |    url = "jdbc:mysql://mysql/cromwell?rewriteBatchedStatements=true&useSSL=false"
    |    user = "cromwell"
    |    password = "cromwell"
    |    connectionTimeout = 5000
    |  }
    |}
    |
    """.stripMargin


def papiConf =
  """
    |database {
    |  profile = "slick.jdbc.MySQLProfile$"
    |  db {
    |    driver = "com.mysql.cj.jdbc.Driver"
    |    url = "jdbc:mysql://mysql/cromwell?rewriteBatchedStatements=true&useSSL=false"
    |    user = "cromwell"
    |    # this is not a secret and is hardcoded.
    |    password = "cromwell"
    |    connectionTimeout = 5000
    |  }
    |}
    |
    """.stripMargin
//val serviceAccountFile: String = "/home/dan/.kube/config"
val serviceAccountFile: String = "/home/BROAD.MIT.EDU/danb/.kube/config"

val kubeThings: IO[(CoreV1Api, AppsV1Api)] = IO {
  import io.kubernetes.client.util.ClientBuilder
  import io.kubernetes.client.util.KubeConfig
  import java.io.FileReader
  val client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(serviceAccountFile))).build
  client.setDebugging(true)
  Configuration.setDefaultApiClient(client);

  (new CoreV1Api(), new AppsV1Api())
}

val envVar = (new V1EnvVar)
envVar.setName("CROMWELL_ARGS")
envVar.setValue("server")

import io.kubernetes.client.custom.Quantity
import io.kubernetes.client.models.{V1ConfigMapBuilder, V1Deployment, V1DeploymentBuilder, V1PersistentVolumeClaimBuilder, V1Service, V1ServiceBuilder}
import collection.JavaConverters._

object CromwellComponents {

  val service: V1Service =
    new V1ServiceBuilder().
      withNewMetadata().withName("cromwell-service").endMetadata().
      withNewSpec().addNewPort().withPort(8000).endPort().addToSelector("app", "cromwell").endSpec().
      build()

  val worker: V1Deployment =
    new V1DeploymentBuilder().
      withNewMetadata().withNewName("cromwell").endMetadata().
      withNewSpec.
      withReplicas(1).
      withNewSelector().addToMatchLabels("app", "cromwell").endSelector().
      withNewTemplate.
      withNewMetadata().addToLabels("app", "cromwell").endMetadata().
      withNewSpec().
      addNewVolume().withName("config-volume").withNewConfigMap.withName("cromwell-conf").endConfigMap().endVolume().
      addNewContainer().
      withImage("broadinstitute/cromwell:44").
      withName("cromwell").
      addNewEnv().withName("CROMWELL_ARGS").withValue("server").endEnv().
      addNewEnv().withName("JAVA_OPTS").withValue("-Dconfig.file=/conf/cromwell.conf").endEnv().
      addNewVolumeMount().withMountPath("/conf").withName("config-volume").endVolumeMount().
      endContainer().
      endSpec().
      endTemplate.
      endSpec.
      build()


  val mysqlDeployment: V1Deployment =
    new V1DeploymentBuilder().
      withNewMetadata().withNewName("mysql").
      endMetadata().
      withNewSpec.
      withReplicas(1).
      withNewSelector().addToMatchLabels("app", "mysql").endSelector().

      withNewTemplate.
      withNewMetadata().addToLabels("app", "mysql").endMetadata().
      withNewSpec().
      addNewContainer().
      withImage("mysql:5.5").
      withName("mysql").
      addNewEnv().withName("MYSQL_ROOT_PASSWORD").withValue("cromwell").endEnv().
      addNewEnv().withName("MYSQL_USER").withValue("cromwell").endEnv().
      addNewEnv().withName("MYSQL_PASSWORD").withValue("cromwell").endEnv().
      addNewEnv().withName("MYSQL_DATABASE").withValue("cromwell").endEnv().
      addNewPort().withContainerPort(3306).endPort().
      addNewVolumeMount().withMountPath("/var/lib/mysql").withName("mysql-persistent-storage").endVolumeMount().
      endContainer().
      addNewVolume().withName("mysql-persistent-storage").withNewPersistentVolumeClaim().withClaimName("mysql-pv-claim2").endPersistentVolumeClaim().endVolume().
      endSpec().
      endTemplate.
      endSpec.
      build()

  val mysqlService = new V1ServiceBuilder().
    withNewMetadata().withName("mysql-service").endMetadata().
    withNewSpec().addNewPort().withPort(3306).endPort().addToSelector("app", "mysql").endSpec().
    build()

  val pvc = new V1PersistentVolumeClaimBuilder().withNewMetadata().withName("mysql-pv-claim2").endMetadata().
    withNewSpec().withAccessModes("ReadWriteOnce").withNewResources().addToRequests("storage", new Quantity("20Gi")).endResources().endSpec().build()

  val cfg = new V1ConfigMapBuilder().
    withNewMetadata().withName("cromwell-conf").endMetadata().
    withData(Map("cromwell.conf" -> conf).asJava).build()

  def conf =
    """
      |database {
      |  profile = "slick.jdbc.MySQLProfile$"
      |  db {
      |    driver = "com.mysql.cj.jdbc.Driver"
      |    url = "jdbc:mysql://mysql/cromwell?rewriteBatchedStatements=true&useSSL=false"
      |    user = "cromwell"
      |    password = "cromwell"
      |    connectionTimeout = 5000
      |  }
      |}
      |
    """.stripMargin


  def papiConf =
    """
      |database {
      |  profile = "slick.jdbc.MySQLProfile$"
      |  db {
      |    driver = "com.mysql.cj.jdbc.Driver"
      |    url = "jdbc:mysql://mysql/cromwell?rewriteBatchedStatements=true&useSSL=false"
      |    user = "cromwell"
      |    password = "cromwell"
      |    connectionTimeout = 5000
      |  }
      |}
      |
    """.stripMargin
}

import CromwellComponents._

/***** MySQL ****/

// Service
//api.createNamespacedService("default", mysqlService, null, null, null)


/***** Cromwell ****/
// ConfigMap
//api.createNamespacedConfigMap("default", cfg, null, null, null)

// Deployment
//apps.createNamespacedDeployment("default", worker, null, null, null)

// Service
//api.createNamespacedService("default", service, null, null, null)

object Init
object VolumeClaimed
object MysqlDeployment
object ServiceCreated
object CromwellConfigMap
object CromwellDeployment
object CromwellService

import java.util

case class Config(namespace: String = "default", coreApi: CoreV1Api, appsV1Api: AppsV1Api)

import cats.data.OptionT
import cats.syntax.either._


def cromwellDeploymentExists: cats.data.ReaderT[IO, Config, V1DeploymentList] = ReaderT[IO,Config,V1DeploymentList] { config =>
    IO.async{reporter => config.
      appsV1Api.
      listNamespacedDeploymentAsync("default", null,null,null, null, null, null, null, null,null, callback(reporter))}
}

def persistentVolumeClaim(deleteExisting: Boolean): IndexedReaderWriterStateT[IO, Config, Chain[String], Init.type,  VolumeClaimed.type, V1PersistentVolumeClaim] = IndexedReaderWriterStateT.apply {
  (env, sa) => {
    // Persistent Volume Claim for MySQL persistence
    IO.async[V1PersistentVolumeClaim]{reporter =>
      env.coreApi.createNamespacedPersistentVolumeClaimAsync(env.namespace, pvc, null, null, null, callback(reporter))
    }.map(claim => (Chain(s"created persistent volume claim $claim"), VolumeClaimed, claim))
  }
}

def cromwellDeployment: IndexedReaderWriterStateT[IO, Config, Chain[String], VolumeClaimed.type, MysqlDeployment.type, V1Deployment] = IndexedReaderWriterStateT.apply {
  (env, sa) => {
    IO.async[V1Deployment] { reporter =>
      // Deployment
      env.appsV1Api.createNamespacedDeploymentAsync(env.namespace, mysqlDeployment, null, null, null, callback(reporter))
    }.map(mysqlDeployment => (Chain(s"deployed mysql $mysqlDeployment"), MysqlDeployment, mysqlDeployment))
  }
}

def callback[A](reporter: (Either[Exception, A]) => Unit): ApiCallback[A] = {
  new ApiCallback[A] {
    override def onFailure(e: ApiException, statusCode: Int, responseHeaders: util.Map[String, util.List[String]]): Unit = reporter(Left(e))
    override def onSuccess(result: A, statusCode: Int, responseHeaders: util.Map[String, util.List[String]]): Unit = reporter(Right(result))
    override def onUploadProgress(bytesWritten: Long, contentLength: Long, done: Boolean): Unit = ()
    override def onDownloadProgress(bytesRead: Long, contentLength: Long, done: Boolean): Unit = ()
  }
}
import cats.syntax.apply._
import cats.syntax.functor._
import cats.instances.tuple._

//script assumes none of these things exist.  If they do, should either write a new script or comment these out
for {
  claim <- persistentVolumeClaim(false)
  deployment <- cromwellDeployment
} yield ()

val x: IO[Config] = kubeThings.map{ case (api, apps) => Config(coreApi = api, appsV1Api = apps) }

x.flatMap{
    cromwellDeploymentExists.run
}.map(option => println(s"\n\n\nthing was $option")).unsafeRunSync()

