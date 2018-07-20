import $ivy.`com.google.apis:google-api-services-drive:v3-rev124-1.23.0`
import $ivy.`com.google.oauth-client:google-oauth-client-jetty:1.23.0`
import $ivy.`com.google.http-client:google-http-client:1.23.0`
import $ivy.`com.google.http-client:google-http-client-jackson2:1.23.0`


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Preconditions;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collections;

val JSON_FACTORY = JacksonFactory.getDefaultInstance();

val httpTransport = GoogleNetHttpTransport.newTrustedTransport();
val DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".store/drive_sample");
val dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);



  def authorize(): Credential = {
    // load client secrets
    val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
        new InputStreamReader(new java.io.FileInputStream("/home/dan/key.json")));

    println(clientSecrets)
    /*
    if (clientSecrets.getDetails().getClientId().startsWith("Enter")
        || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
      System.out.println(
          "Enter Client ID and Secret from https://code.google.com/apis/console/?api=drive "
          + "into drive-cmdline-sample/src/main/resources/client_secrets.json");
      System.exit(1);
    }
    */
    // set up authorization code flow
    val flow = new GoogleAuthorizationCodeFlow.Builder(
        httpTransport, JSON_FACTORY, clientSecrets,
        Collections.singleton(DriveScopes.DRIVE)).setDataStoreFactory(dataStoreFactory)
        .build();
    // authorize
    new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
  }

//authorize()
//
     val credential = authorize();
      // set up the global Drive instance
     val drive = new Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(
          "driveSizer").build();
val files = drive.files.list().setQ("mimeType='application/vnd.google-apps.folder'").execute()


