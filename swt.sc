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

//This ampersand is VERY IMPORTANT to re-process script!
@

import org.eclipse.swt.graphics._
import org.eclipse.swt.widgets._
import org.eclipse.swt.dnd._
import org.eclipse.swt.events._
import org.eclipse.swt.layout._
import org.eclipse.swt.SWT
import java.lang.reflect.Field
import java.lang.ClassLoader

/*
Set the lib path setting for sigar native library
*/
val pathSetting = "java.library.path"
val libPath = System.getProperty(pathSetting)
val setTo =
  os match {
    case Posix => (pwd/'lib).toString
    case Mac => (pwd/'maclib).toString
  }

if (libPath !=  setTo) {
  System.setProperty(pathSetting, setTo )
  val fieldSysPath = classOf[ClassLoader].getDeclaredField( "sys_paths" );
  fieldSysPath.setAccessible( true );
  fieldSysPath.set( null, null );
}

//use this in debug REPL
val sigar = new org.hyperic.sigar.Sigar


val display = new Display ();
val shell = new Shell(display)
shell.setText("Demo")

val resultsComposite = new Composite(shell, SWT.NONE)
val rl = new RowLayout(SWT.HORIZONTAL)
resultsComposite.setLayout(rl)

val l = new Label(resultsComposite, SWT.NONE)
l.setText("address of cromwell: ")
l.setSize(50, 15)

val t = new Text(resultsComposite, SWT.SINGLE)

shell.setLayout(rl)
shell.setSize(800, 800)
shell.open ();
while (!shell.isDisposed ()) {
        if (!display.readAndDispatch ()) display.sleep ();
}

display.dispose ();
