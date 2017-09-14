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
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell


/**
  * A simple TreeViewer to demonstrate usage
  *
  * @author Tom Schindl <tom.schindl@bestsolution.at>
  *
  */

class Snippet026TreeViewerTabEditing(val shell: Shell) {
  val b = new Button(shell, SWT.PUSH)
  b.setText("Remove column")
  val v = new TreeViewer(shell, SWT.BORDER | SWT.FULL_SELECTION)
  v.getTree.setLinesVisible(true)
  v.getTree.setHeaderVisible(true)
  b.addSelectionListener(new SelectionListener() {
    def widgetDefaultSelected(e: SelectionEvent): Unit = {
    }

    def widgetSelected(e: SelectionEvent): Unit
    =
    {
      v.getTree.getColumn(1).dispose
    }
  })
  val focusCellManager = new TreeViewerFocusCellManager(v, new FocusCellOwnerDrawHighlighter(v))
  val actSupport = new ColumnViewerEditorActivationStrategy(v) {
    override protected def isEditorActivationEvent(event: ColumnViewerEditorActivationEvent): Boolean =
      (event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL) ||
      (event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION) ||
      (
        (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED) &&
        (event.keyCode == SWT.CR)
      ) ||
      (event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC)
  }
  val feature: Int = ColumnViewerEditor.TABBING_HORIZONTAL | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR | ColumnViewerEditor.TABBING_VERTICAL | ColumnViewerEditor.KEYBOARD_ACTIVATION
  TreeViewerEditor.create(v, focusCellManager, actSupport, feature)
  val textCellEditor = new TextCellEditor(v.getTree)
  val columLabels = Array("Column 1", "Column 2", "Column 3")
  val labelPrefix = Array("Column 1 => ", "Column 2 => ", "Column 3 => ")
  var i = 0
  while ( {
    i < columLabels.length
  }) {
    val column = new TreeViewerColumn(v, SWT.NONE)
    column.getColumn.setWidth(200)
    column.getColumn.setMoveable(true)
    column.getColumn.setText(columLabels(i))
    column.setLabelProvider(createColumnLabelProvider(labelPrefix(i)))
    column.setEditingSupport(createEditingSupportFor(v, textCellEditor))

    {
      i += 1; i - 1
    }
  }
  v.setContentProvider(new MyContentProvider())
  v.setInput(createModel)

  private def createColumnLabelProvider(prefix: String) = new ColumnLabelProvider() {
    override def getText(element: Any): String = return prefix + element.toString
  }

  private def createEditingSupportFor(viewer: TreeViewer, textCellEditor: TextCellEditor) = new EditingSupport(viewer) {
    protected def canEdit(element: AnyRef): Boolean = true

    protected

    def getCellEditor(element: AnyRef): CellEditor = return textCellEditor

    protected

    def getValue(element: AnyRef): AnyRef = return element.asInstanceOf[Snippet026TreeViewerTabEditing#MyModel].counter + ""

    protected

    def setValue(element: Any, value: Any): Unit = {
      element.asInstanceOf[Snippet026TreeViewerTabEditing#MyModel].counter = value.toString.toInt
      viewer.update(element, null)
    }
  }

  private def createModel = {
    val root = new MyModel(0, null)
    root.counter = 0
    var tmp:MyModel = null
    var subItem:MyModel = null
    var i = 1
    while ( {
      i < 10
    }) {
      tmp = new MyModel(i, root)
      root.child.add(tmp)
      var j = 1
      while ( {
        j < i
      }) {
        subItem = new MyModel(j, tmp)
        subItem.child.add(new MyModel(j * 100, subItem))
        tmp.child.add(subItem)

        {
          j += 1; j - 1
        }
      }

      {
        i += 1; i - 1
      }
    }
    root
  }

  private class MyContentProvider extends ITreeContentProvider {
    def getElements(inputElement: Any): Array[AnyRef] = inputElement.asInstanceOf[Snippet026TreeViewerTabEditing#MyModel].child.toArray

    override def dispose(): Unit = {
    }

    override def inputChanged(viewer: Viewer, oldInput: AnyRef, newInput: AnyRef): Unit = {
    }

    def getChildren(parentElement: AnyRef): Array[AnyRef] = getElements(parentElement)

    def getParent(element: AnyRef): AnyRef = {
      if (element == null) return null
      element.asInstanceOf[Snippet026TreeViewerTabEditing#MyModel].parent
    }

    def hasChildren(element: Any): Boolean = element.asInstanceOf[Snippet026TreeViewerTabEditing#MyModel].child.size > 0
  }

  class MyModel(var counter: Int, var parent: Snippet026TreeViewerTabEditing#MyModel) {
    var child = new util.ArrayList[Snippet026TreeViewerTabEditing#MyModel]

    override def toString: String = {
      var rv = "Item "
      if (parent != null) rv = parent.toString + "."
      rv += counter
      rv
    }
  }

}

val display = new Display
val shell = new Shell(display)
shell.setLayout(new FillLayout())
new Snippet026TreeViewerTabEditing(shell)
shell.open
while ( {
  !shell.isDisposed
}) if (!display.readAndDispatch) display.sleep
display.dispose
