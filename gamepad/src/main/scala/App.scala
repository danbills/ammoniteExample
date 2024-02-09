package tutorial.webapp

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html
import org.scalajs.dom.Gamepad
import org.scalajs.dom.html.Canvas

object TutorialApp {
  def main(args: Array[String]): Unit = {
    println("Hello world!")
//    appendPar(document.body, "Hello World")
    val canvas: Canvas = dom.document.getElementById("gameCanvas").asInstanceOf[html.Canvas]
    if (canvas == null) {
      println("Canvas is null")
    }
    val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    val resultDiv = dom.document.getElementById("result").asInstanceOf[html.Div]

    var playerX: Double = canvas.width / 2

    dom.window.addEventListener("gamepadconnected", (e: dom.Event) => {
      val gamepad = e.asInstanceOf[dom.GamepadEvent].gamepad
      handleGamepad(gamepad)
    })


    // every 2000 milliseconds print out a full report of the gamepad state
    dom.window.setInterval(() => {
      val gamepads = dom.window.navigator.getGamepads()
      for (gamepad <- gamepads) {
        if (gamepad != null) {
          println(s"Gamepad: ${gamepad.id}")
          println(s"  Axes: ${gamepad.axes.mkString(", ")}")
          println(s"  Buttons: ${gamepad.buttons.mkString(", ")}")
//          println(s" Axis sample: ${gamepad.axes(1)}")
        }
      }
    }, 5000) // Check every 50 milliseconds


    // Periodically check for updated gamepad state
    dom.window.setInterval(() => {
      val gamepads = dom.window.navigator.getGamepads()
      for (gamepad <- gamepads) {
        if (gamepad != null) handleGamepad(gamepad)
      }
    }, 50) // Check every 50 milliseconds

    def handleGamepad(gamepad: Gamepad): Unit = {
      // Basic movement with the first axis
      playerX = playerX + gamepad.axes(0) * 5

      resultDiv.textContent = s"Player X: $playerX"
    }

    def gameLoop(timestamp: Double): Unit = {
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      ctx.fillStyle = "blue"

      ctx.fillRect(playerX, canvas.height / 2, 20, 20)
      dom.window.requestAnimationFrame(gameLoop) // Repeat animation
    }

    gameLoop(0.0) // Start the game loop
  }

  def appendPar(targetNode: dom.Node, text: String): Unit = {
    val parNode = document.createElement("p")
    parNode.textContent = text
    targetNode.appendChild(parNode)
  }
}
