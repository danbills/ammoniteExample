//IMPORTANT
//to run on mac use JAVA_OPTS="-XstartOnFirstThread" amm -w swt.sc

interp.repositories() ++= Seq(coursier.maven.MavenRepository(
  "http://maven-eclipse.github.io/maven/"))

import $ivy.`org.clapper::grizzled-scala:4.4.1`
import $ivy.`org.fusesource:sigar:1.6.4`

import grizzled.sys._
import grizzled.sys.OperatingSystem._

import ammonite.ops.{Path => APath, _}


os match {
  case Mac => interp.load.ivy("org.eclipse.swt" % "org.eclipse.swt.cocoa.macosx.x86_64" % "4.6.1")
  case Posix => interp.load.ivy("org.eclipse.swt" % "org.eclipse.swt.gtk.linux.x86_64" % "4.6.1")
  case _ => //do nothing
}

@

import org.eclipse.swt.graphics._
import org.eclipse.swt.widgets._
import org.eclipse.swt.dnd._
import org.eclipse.swt.events._
import java.lang.reflect.Field
import java.lang.ClassLoader

if (!(System.getProperty("java.library.path") ==  "/home/dan/9e551bac3626c8d3f5fd197775409135/lib/")) {
  System.setProperty("java.library.path", "/home/dan/9e551bac3626c8d3f5fd197775409135/lib/" )
  val fieldSysPath = classOf[ClassLoader].getDeclaredField( "sys_paths" );
  fieldSysPath.setAccessible( true );
  fieldSysPath.set( null, null );
}
val sigar = new org.hyperic.sigar.Sigar


/*
val display = new Display ();
val shell = new Shell(display)
shell.setText("Demo")

shell.setSize(800, 800)
shell.open ();
while (!shell.isDisposed ()) {
        if (!display.readAndDispatch ()) display.sleep ();
}

display.dispose ();
*/
