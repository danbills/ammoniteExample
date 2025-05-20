// A Scala JLine3 console application with command completion, enum arguments, and Iron Types validation.
// Demonstrates JLine3 for interactive input, Scala 3 enums, and Iron for refined type validation.

//> using scala "3.3.3"
//> using dep "org.jline:jline:3.26.1"
//> using dep "io.github.iltotore::iron:3.0.1"

import org.jline.terminal.TerminalBuilder
import org.jline.reader.LineReaderBuilder
import org.jline.reader.LineReader
// import org.jline.utils.InfoCmp.Capability // Optional, for advanced terminal features if needed
import org.jline.reader.UserInterruptException
import org.jline.reader.EndOfFileException
import org.jline.reader.impl.completer.{StringsCompleter, ArgumentCompleter, AggregateCompleter, NullCompleter}
import org.jline.reader.Completer // Base Completer interface
import org.jline.reader.impl.LineReaderImpl // Important for setCompleter

// Iron Types specific imports
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.numeric.*
// import io.github.iltotore.iron.macros.autoRefine // Not used in this iteration

// Definition for the 'Color' enum used by the 'set-color' command.
enum Color:
  case Red, Green, Blue
object Color:
  // Utility to parse a String to a Color, ignoring case.
  def fromString(s: String): Option[Color] = 
    Color.values.find(_.toString.equalsIgnoreCase(s))

// Iron type definition for positive integers, used by the 'sum' command.
// Ensures that numbers provided to 'sum' are greater than zero at runtime.
type PositiveInt = Int :| Positive

// Section: Command Handlers
// Each function handles the logic for a specific command.
// They take an array of string arguments and return a string response.

def handleGreet(args: Array[String]): String = {
  if (args.length >= 1) s"Hello, ${args(0)}!" else "Usage: greet <name>"
}

def handleSum(args: Array[String]): String = {
  if (args.length >= 2) {
    try {
      val num1Str = args(0)
      val num2Str = args(1)

      // Attempt to parse to Int first
      val num1Int = num1Str.toInt
      val num2Int = num2Str.toInt

      // Attempt to refine to PositiveInt
      val num1Positive: Either[String, PositiveInt] = num1Int.refineEither[Positive]
      val num2Positive: Either[String, PositiveInt] = num2Int.refineEither[Positive]

      (num1Positive, num2Positive) match {
        case (Right(n1), Right(n2)) =>
          s"Sum: ${n1.value + n2.value}" // Access the value using .value
        case (Left(err1), _) => s"Error for first number ($num1Str): $err1"
        case (_, Left(err2)) => s"Error for second number ($num2Str): $err2"
      }
    } catch {
      case e: NumberFormatException => "Invalid number format. Please provide integers. Usage: sum <positive-number1> <positive-number2>"
    }
  } else {
    "Usage: sum <positive-number1> <positive-number2>"
  }
}

def handleSetColor(args: Array[String]): String = {
  if (args.length >= 1) {
    Color.fromString(args(0)) match {
      case Some(color) => s"Color set to: $color"
      case None => s"Unknown color: ${args(0)}. Available colors are: ${Color.values.map(_.toString).mkString(", ")}"
    }
  } else {
    s"Please provide a color. Available colors are: ${Color.values.map(_.toString).mkString(", ")}"
  }
}

val commandHandlers: Map[String, Array[String] => String] = Map(
  "greet" -> handleGreet,
  "sum" -> handleSum,
  "set-color" -> handleSetColor // New command
)

// Main entry point for the console application.
@main def runConsole(): Unit = {
  // Setup JLine Terminal and LineReader for interactive console input.
  val terminal = TerminalBuilder.builder().system(true).build()
  var lineReader = LineReaderBuilder.builder().terminal(terminal).build()

  // Configure command completion using JLine's completer system.
  // An AggregateCompleter is used to combine multiple completers for different commands.
  if (lineReader.isInstanceOf[LineReaderImpl]) {
    // Completer for "greet": command name only.
    val greetCompleter = new ArgumentCompleter(
      new StringsCompleter("greet"),
      NullCompleter.INSTANCE // No specific completer for arguments of "greet".
    )

    // Completer for "sum": command name only. Arguments are validated at runtime.
    val sumCompleter = new ArgumentCompleter(
      new StringsCompleter("sum"),
      NullCompleter.INSTANCE, // No specific completer for the first argument.
      NullCompleter.INSTANCE  // No specific completer for the second argument.
    )

    // Completer for "set-color": command name and Color enum values.
    val setColorCompleter = new ArgumentCompleter(
      new StringsCompleter("set-color"),
      new StringsCompleter(Color.values.map(_.toString): _*) // Provides "Red", "Green", "Blue" as completions.
    )
    
    // Completer for "exit" and "quit" commands.
    val exitQuitCompleter = new StringsCompleter("exit", "quit")

    // Combine all completers.
    val aggregateCompleter = new AggregateCompleter(
      greetCompleter,
      sumCompleter,
      setColorCompleter,
      exitQuitCompleter
    )
    lineReader.asInstanceOf[LineReaderImpl].setCompleter(aggregateCompleter)
  } else {
    println("Warning: Cannot set completer, LineReader is not an instance of LineReaderImpl.")
  }

  val prompt = "prompt> "
  var running = true // Controls the main input loop.

  // Display welcome message and instructions.
  println("Welcome to the JLine3 Scala Console!")
  println("Available commands: greet <name>, sum <positive-num1> <positive-num2>, set-color <Red|Green|Blue>, exit, quit")
  println("Press Tab for completion. Ctrl+D or 'exit'/'quit' to leave.")
  println("Press Ctrl+C to interrupt current input.")


  // Main input loop.
  try {
    while (running) {
      val line = try {
        // Read a line of input from the user.
        lineReader.readLine(prompt)
      } catch {
        case _: UserInterruptException => // Handle Ctrl+C.
          println("Ctrl+C pressed. Type 'exit' or 'quit' to leave.")
          "" // Return an empty line to continue the loop, allowing user to type exit.
        case _: EndOfFileException => // Handle Ctrl+D.
          println("Ctrl+D pressed. Exiting...")
          running = false
          null // Signal to exit the loop.
      }

      if (line == null) { // Null line typically means EOF (Ctrl+D) was processed.
        running = false
      } else {
        val trimmedLine = line.trim
        // Command processing logic.
        if (trimmedLine.equalsIgnoreCase("exit") || trimmedLine.equalsIgnoreCase("quit")) {
          println("Exiting...")
          running = false
        } else if (trimmedLine.nonEmpty) {
          // Split input into command and arguments.
          val parts = trimmedLine.split("\\s+", 2) 
          val command = parts(0)
          val args = if (parts.length > 1) parts(1).split("\\s+") else Array.empty[String]

          // Execute command via the commandHandlers map.
          commandHandlers.get(command) match {
            case Some(handler) =>
              println(handler(args)) // Print result of command execution.
            case None =>
              // Handle unknown commands.
              println(s"Unknown command: $command. Available: greet, sum, set-color, exit, quit.")
          }
        }
      }
    }
  } catch {
    case t: Throwable =>
      println(s"An unexpected error occurred: ${t.getMessage}")
      t.printStackTrace()
  } finally {
    try {
      terminal.close()
      println("Terminal closed.")
    } catch {
      case e: Exception =>
        println(s"Error closing terminal: ${e.getMessage}")
        e.printStackTrace()
    }
  }
}
