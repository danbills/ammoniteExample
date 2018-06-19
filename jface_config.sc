//IMPORTANT
//to run on mac use JAVA_OPTS="-XstartOnFirstThread" amm -w swt.sc

interp.repositories() ++= Seq(coursier.maven.MavenRepository(
  "http://maven-eclipse.github.io/maven/"))

import $ivy.`org.clapper::grizzled-scala:4.4.1`

import grizzled.sys._
import grizzled.sys.OperatingSystem._

import ammonite.ops.{Path => APath, _}
//import $ivy.`org.eclipse.platform:org.eclipse.jface:3.12.2`
//load jface, which painfully includes a dependency on swt magic sauce jar
interp.load.ivy(coursier.Dependency(module = coursier.Module("org.eclipse.platform", "org.eclipse.jface", Map.empty), version = "3.12.2", exclusions = Set(("org.eclipse.platform", "org.eclipse.swt.${osgi.platform}"))))


os match {
  case Mac => interp.load.ivy("org.eclipse.swt" % "org.eclipse.swt.cocoa.macosx.x86_64" % "4.6.1")
  case Posix => interp.load.ivy("org.eclipse.swt" % "org.eclipse.swt.gtk.linux.x86_64" % "4.6.1")
  case _ => //do nothing
}

//This ampersand is VERY IMPORTANT to re-process script!
@


import java.util
import org.eclipse.jface.viewers.CellEditor
import org.eclipse.jface.viewers.ColumnLabelProvider
import org.eclipse.jface.viewers.ColumnViewerEditor
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy
import org.eclipse.jface.viewers.EditingSupport
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter
import org.eclipse.jface.viewers.ITreeContentProvider
import org.eclipse.jface.viewers.TextCellEditor
import org.eclipse.jface.viewers.TreeViewer
import org.eclipse.jface.viewers.TreeViewerColumn
import org.eclipse.jface.viewers.TreeViewerEditor
import org.eclipse.jface.viewers.TreeViewerFocusCellManager
import org.eclipse.jface.viewers.Viewer
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell


class WebServerConfig(parent: Composite) extends Composite(parent, SWT.NONE) {
  /*
webservice {
  port = 8000
  interface = 0.0.0.0
  binding-timeout = 5s
  instance.name = "reference"
}
*/
  val layout = new FillLayout(SWT.VERTICAL)
  setLayout(layout)
  val portLabel = new Label(this)
  portLabel.setText("Port")

  val portText = new Text(this)
  portText.setText("8000")


}


/*
val display = new Display
val shell = new Shell(display)
shell.setLayout()


shell.open
while ( {
  !shell.isDisposed
}) if (!display.readAndDispatch) display.sleep
display.dispose
*/
