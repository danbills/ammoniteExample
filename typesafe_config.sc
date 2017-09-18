import $ivy.`com.typesafe:config:1.3.1`

import com.typesafe.config.Config
import scala.collection.JavaConverters._
import com.typesafe.config.ConfigFactory

interp.repositories() ++= Seq(coursier.maven.MavenRepository(
  "http://maven-eclipse.github.io/maven/"))

import $ivy.`org.clapper::grizzled-scala:4.4.1`

import grizzled.sys._
import grizzled.sys.OperatingSystem._

os match {
  case Mac => interp.load.ivy("org.eclipse.swt" % "org.eclipse.swt.cocoa.macosx.x86_64" % "4.6.1")
  case Posix => interp.load.ivy("org.eclipse.swt" % "org.eclipse.swt.gtk.linux.x86_64" % "4.6.1")
  case _ => //do nothing
}

@

val config = ConfigFactory.parseFile(new java.io.File("/Users/danb/cromwell/core/src/main/resources/reference.conf"))

val configs = config.root.values.asScala

import org.eclipse.swt.graphics._
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.widgets._
import org.eclipse.swt.dnd._
import org.eclipse.swt.events._
import org.eclipse.swt.layout._
import org.eclipse.swt.SWT

val display = new Display ();
val shell = new Shell(display)
shell.setText("Demo")

/*
val resultsComposite = new Composite(shell, SWT.NONE)
val rl = new RowLayout(SWT.HORIZONTAL)
resultsComposite.setLayout(rl)

val l = new Label(resultsComposite, SWT.NONE)
l.setText("address of cromwell: ")
l.setSize(50, 15)

val t = new Text(resultsComposite, SWT.SINGLE)

shell.setLayout(rl)
*/
shell.setLayout(new FillLayout());
//val tree = new Tree (shell, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
val tree = new Tree (shell, SWT.SINGLE);
tree.setHeaderVisible(true);
val column1 = new TreeColumn(tree, SWT.LEFT);
column1.setText("Column 1");
column1.setWidth(200);
val column2 = new TreeColumn(tree, SWT.CENTER);
column2.setText("Column 2");
column2.setWidth(200);
val column3 = new TreeColumn(tree, SWT.RIGHT);
column3.setText("Column 3");
column3.setWidth(200);
	for (i<- 0 to 4) {
    val iItem = new TreeItem (tree, SWT.NONE);
		iItem.setText (Array("TreeItem (0) -" + i, "s", "3")/*.asJava*/);
    for (j<- 0 to 4) {
      val jItem = new TreeItem (iItem, SWT.NONE);
			jItem.setText (Array("TreeItem (1) -" + j, "s", "3")/*.asJava*/);
		}
	}

val editor = new TreeEditor(tree);
editor.horizontalAlignment = SWT.LEFT;
editor.grabHorizontal = true;

tree.addMouseListener(new MouseAdapter() {
  override def mouseDoubleClick(e: MouseEvent) {
    println(s"button ${e.button} stateMask ${e.stateMask} x ${e.x} y ${e.y}")
  }
})
tree.addKeyListener(new KeyAdapter() {
  override def keyPressed(event: KeyEvent) {
    if (event.keyCode == SWT.F2 && tree.getSelectionCount() == 1) {
      val item = tree.getSelection()(0);

      val text = new Text(tree, SWT.NONE);
      text.setText(item.getText());
      text.selectAll();
      text.setFocus();

      text.addFocusListener(new FocusAdapter() {
        override def focusLost(event: FocusEvent ) {
          item.setText(text.getText());
          text.dispose();
        }
      });

      text.addKeyListener(new KeyAdapter() {
        override def keyPressed(event: KeyEvent) {
          event.keyCode match  {
            case SWT.CR =>
              item.setText(text.getText());
              text.dispose();
            case SWT.ESC =>
              text.dispose();
            case other => println(s"caught other $other")
          }
        }
      });
      editor.setEditor(text, item);
    }
  }
});
shell.setSize(800, 800)
shell.open ();
while (!shell.isDisposed ()) {
        if (!display.readAndDispatch ()) display.sleep ();
}

display.dispose ();
