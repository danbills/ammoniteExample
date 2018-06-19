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



import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets._

object DirectoryPage {
  val PAGE_NAME = "Directory"
}

class DirectoryPage() extends WizardPage(DirectoryPage.PAGE_NAME, "Directory Page", null) {
  var button: Button = null


  def createControl(parent: Composite) = {
    val topLevel = new Composite(parent, SWT.NONE);
    topLevel.setLayout(new GridLayout(2, false));

    val l = new Label(topLevel, SWT.CENTER);
    l.setText("Use default directory?");

    button = new Button(topLevel, SWT.CHECK);

    setControl(topLevel);
    setPageComplete(true);
  }

  def useDefaultDirectory(): Boolean = {
    button.getSelection();
  }
}


import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets._
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets._
import org.eclipse.jface.wizard.WizardPage
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label




object WizardDialogDemo extends App {
    val testWindow = new ApplicationWindow(null);

    testWindow.setBlockOnOpen(false);
    testWindow.open();

    val wizard = new ProjectWizard();
    val wizardDialog = new WizardDialog(
                                  testWindow.getShell(),
                                  wizard);
    wizardDialog.create();
    wizardDialog.open();
}


class ProjectWizard() extends Wizard {
  override def addPages(): Unit = {
    addPage(new DirectoryPage());
    addPage(new ChooseDirectoryPage());
    addPage(new SummaryPage());
  }

  def performFinish: Boolean = {
    val dirPage = getDirectoryPage
    if (dirPage.useDefaultDirectory) System.out.println("Using default directory")
    else {
      val choosePage = getChoosePage
      System.out.println("Using directory: " + choosePage.getDirectory)
    }
    true
  }

  private def getChoosePage = getPage(ChooseDirectoryPage.PAGE_NAME).asInstanceOf[ChooseDirectoryPage]

  private def getDirectoryPage = getPage(DirectoryPage.PAGE_NAME).asInstanceOf[DirectoryPage]

  override def performCancel: Boolean = {
    println("Perform Cancel called")
    true
  }

  override def getNextPage(page: IWizardPage) = {
    if (page.isInstanceOf[DirectoryPage]) {
      val dirPage = page.asInstanceOf[DirectoryPage]
      if (dirPage.useDefaultDirectory) {
        val summaryPage = getPage(SummaryPage.PAGE_NAME).asInstanceOf[SummaryPage]
        summaryPage.updateText("Using default directory")
        summaryPage
      }
    }
    val nextPage: IWizardPage = super.getNextPage(page)
    if (nextPage.isInstanceOf[SummaryPage]) {
      val summary = nextPage.asInstanceOf[SummaryPage]
      val dirPage = getDirectoryPage
      summary.updateText(if (dirPage.useDefaultDirectory) "Using default directory"
      else "Using directory:" + getChoosePage.getDirectory)
    }
    nextPage
  }
}

object ChooseDirectoryPage {
  val PAGE_NAME = "Choose Directory"
}

class ChooseDirectoryPage() extends WizardPage(ChooseDirectoryPage.PAGE_NAME, "Choose Directory Page", null) {
  private var text: Text = null

  def createControl(parent: Composite): Unit = {
    val topLevel = new Composite(parent, SWT.NONE)
    topLevel.setLayout(new GridLayout(2, false))
    val l = new Label(topLevel, SWT.CENTER)
    l.setText("Enter the directory to use:")
    text = new Text(topLevel, SWT.SINGLE)
    text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL))
    setControl(topLevel)
    setPageComplete(true)
  }

  def getDirectory: String = text.getText
}


object SummaryPage {
  val PAGE_NAME = "Summary"
}

class SummaryPage() extends WizardPage(SummaryPage.PAGE_NAME, "Summary Page", null) {
  private var textLabel: Text = null

  def createControl(parent: Composite): Unit = {
    val topLevel = new Composite(parent, SWT.NONE)
    topLevel.setLayout(new FillLayout)
    textLabel = new Text(topLevel, SWT.CENTER)
    textLabel.setText("")
    setControl(topLevel)
    setPageComplete(true)
  }

  def updateText(newText: String): Unit = {
    textLabel.setText(newText)
  }
}

WizardDialogDemo.main(Array.empty)




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
