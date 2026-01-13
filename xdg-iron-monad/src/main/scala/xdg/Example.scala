package xdg

import scalaz.*
import Scalaz.*
import xdg.XdgTypes.*
import xdg.XdgMonad.*
import xdg.XdgMonad.XdgOps.*
import scala.util.{Try, Success, Failure}

/**
 * Example application demonstrating the XDG RWS monad
 *
 * This application:
 * - Reads XDG environment configuration (Reader)
 * - Logs operations to XDG_DATA_HOME (Writer)
 * - Maintains application state in XDG_STATE_HOME (State)
 */
object Example:

  /**
   * Example computation using the XDG monad
   *
   * This simulates a simple application that:
   * 1. Checks and logs XDG directories
   * 2. Maintains a visit counter in state
   * 3. Stores user preferences in state
   */
  def exampleComputation: XdgRWS[String] = for {
    // Reader: Get XDG environment
    env <- askEnv
    _ <- log("Application started")
    _ <- log(s"Using XDG directories:")
    _ <- log(s"  Data: ${env.dataHome}")
    _ <- log(s"  Config: ${env.configHome}")
    _ <- log(s"  State: ${env.stateHome}")
    _ <- log(s"  Cache: ${env.cacheHome}")

    // State: Get and increment visit counter
    visitCountOpt <- getState("visit_count")
    visitCount = visitCountOpt.map(_.toInt).getOrElse(0)
    newCount = visitCount + 1
    _ <- setState("visit_count", newCount.toString)
    _ <- log(s"Visit count: $newCount")

    // State: Store some application preferences
    _ <- setState("theme", "dark")
    _ <- setState("language", "en")
    _ <- log("Preferences updated")

    // State: Read back preferences
    theme <- getState("theme")
    language <- getState("language")
    _ <- log(s"Current theme: ${theme.getOrElse("default")}")
    _ <- log(s"Current language: ${language.getOrElse("en")}")

    // Get all directories from Reader
    dataDir <- getDataDir
    configDir <- getConfigDir
    stateDir <- getStateDir
    cacheDir <- getCacheDir

    _ <- log(s"Data directory path: $dataDir")
    _ <- log(s"State directory path: $stateDir")

    // Return final result
    result = s"Application completed successfully (visit #$newCount)"
    _ <- log(result)

  } yield result

  /**
   * More complex example: User profile management
   */
  def userProfileExample(username: String): XdgRWS[Map[String, String]] = for {
    _ <- log(s"Loading profile for user: $username")

    // Check if user profile exists
    existingProfile <- getState(s"user:$username:name")

    result <- existingProfile match {
      case Some(name) =>
        for {
          _ <- log(s"Found existing profile: $name")
          email <- getState(s"user:$username:email")
          _ <- log(s"Email: ${email.getOrElse("not set")}")
        } yield Map(
          "name" -> name,
          "email" -> email.getOrElse("")
        )

      case None =>
        for {
          _ <- log(s"Creating new profile for: $username")
          _ <- setState(s"user:$username:name", username)
          _ <- setState(s"user:$username:email", s"$username@example.com")
          _ <- setState(s"user:$username:created", java.time.Instant.now().toString)
          _ <- log("Profile created successfully")
        } yield Map(
          "name" -> username,
          "email" -> s"$username@example.com"
        )
    }
  } yield result

  /**
   * Example: Working with XDG directories
   */
  def directoryInfoExample: XdgRWS[List[String]] = for {
    _ <- log("Gathering XDG directory information")

    dataDir <- getDataDir
    configDir <- getConfigDir
    stateDir <- getStateDir
    cacheDir <- getCacheDir

    dirs = List(
      s"Data: $dataDir",
      s"Config: $configDir",
      s"State: $stateDir",
      s"Cache: $cacheDir"
    )

    _ <- dirs.traverse_(dir => log(s"  $dir"))

  } yield dirs

  /**
   * Main entry point
   */
  @main def runExample(): Unit =
    println("=" * 60)
    println("XDG Iron Monad Example")
    println("=" * 60)
    println()

    // Get XDG environment from system
    val env = XdgEnvironment.fromEnv
    println(s"XDG Environment:")
    println(s"  Data Home:   ${env.dataHome}")
    println(s"  Config Home: ${env.configHome}")
    println(s"  State Home:  ${env.stateHome}")
    println(s"  Cache Home:  ${env.cacheHome}")
    println()

    // Load previous state if it exists
    val appName = "xdg-example"
    val initialState = XdgRunner.loadState(env, appName)
    println(s"Loaded initial state: $initialState")
    println()

    // Run the example computation
    println("Running example computation...")
    println("-" * 60)

    XdgRunner.run(exampleComputation, env, initialState, appName) match {
      case Success((logs, finalState, result)) =>
        println(s"Result: $result")
        println()
        println("Logs written:")
        logs.foreach(log => println(s"  - $log"))
        println()
        println(s"Final state: $finalState")
        println()
        println(s"✓ Logs persisted to: ${env.dataPath.resolve(appName)}/logs.txt")
        println(s"✓ State persisted to: ${env.statePath.resolve(appName)}/state.txt")

      case Failure(exception) =>
        println(s"✗ Error: ${exception.getMessage}")
        exception.printStackTrace()
    }

    println()
    println("-" * 60)
    println("Running user profile example...")
    println()

    XdgRunner.run(userProfileExample("alice"), env, initialState, appName) match {
      case Success((logs, finalState, profile)) =>
        println(s"User profile: $profile")
        println()
        println("Logs:")
        logs.foreach(log => println(s"  - $log"))

      case Failure(exception) =>
        println(s"✗ Error: ${exception.getMessage}")
    }

    println()
    println("-" * 60)
    println("Running directory info example...")
    println()

    XdgRunner.run(directoryInfoExample, env, initialState, appName) match {
      case Success((logs, finalState, dirs)) =>
        println("XDG Directories:")
        dirs.foreach(println)

      case Failure(exception) =>
        println(s"✗ Error: ${exception.getMessage}")
    }

    println()
    println("=" * 60)
