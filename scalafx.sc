interp.repositories() ++= Seq(coursier.maven.MavenRepository(
  "https://oss.sonatype.org/content/repositories/snapshots/"))
@
import $ivy.`org.scalafx::scalafx:8.0.102-R12-SNAPSHOT`

import _root_.scalafx.application.JFXApp
import _root_.scalafx.application.JFXApp.PrimaryStage
import _root_.scalafx.geometry.Insets
import _root_.scalafx.scene.Scene
import _root_.scalafx.scene.effect.DropShadow
import _root_.scalafx.scene.layout.HBox
import _root_.scalafx.scene.paint.Color._
import _root_.scalafx.scene.paint._
import _root_.scalafx.scene.text.Text

object ScalaFXHelloWorld extends JFXApp {

  stage = new PrimaryStage {
    //    initStyle(StageStyle.Unified)
    title = "ScalaFX Hello World"
    scene = new Scene {
      fill = Color.rgb(38, 38, 38)
      content = new HBox {
        padding = Insets(50, 80, 50, 80)
        children = Seq(
          new Text {
            text = "Scala"
            style = "-fx-font: normal bold 100pt sans-serif"
            fill = new LinearGradient(
              endX = 0,
              stops = Stops(Red, DarkRed))
          },
          new Text {
            text = "FX"
            style = "-fx-font: italic bold 100pt sans-serif"
            fill = new LinearGradient(
              endX = 0,
              stops = Stops(White, DarkGray)
            )
            effect = new DropShadow {
              color = DarkGray
              radius = 15
              spread = 0.25
            }
          }
        )
      }
    }

  }
}

ScalaFXHelloWorld.main(Array.empty)
