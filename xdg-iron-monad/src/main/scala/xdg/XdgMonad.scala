package xdg

import scalaz.*
import Scalaz.*
import xdg.XdgTypes.*
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.nio.charset.StandardCharsets
import scala.util.{Try, Success, Failure}

/**
 * XDG-specific Reader-Writer-State monad
 *
 * - Reader (R): XdgEnvironment - provides access to XDG directories
 * - Writer (W): List[String] - logs written to XDG_DATA_HOME
 * - State (S): Map[String, String] - application state stored in XDG_STATE_HOME
 */
object XdgMonad:

  /**
   * Type alias for our XDG RWS monad
   * R = XdgEnvironment (Reader - provides XDG directory paths)
   * W = List[String] (Writer - logs to be written to data directory)
   * S = Map[String, String] (State - kept in state directory)
   * A = Result value
   */
  type XdgRWS[A] = RWST[Id, XdgEnvironment, List[String], Map[String, String], A]

  /**
   * Operations for working with the XDG monad
   */
  object XdgOps:

    /**
     * Get the XDG environment from the Reader
     */
    def askEnv: XdgRWS[XdgEnvironment] =
      RWST[Id, XdgEnvironment, List[String], Map[String, String], XdgEnvironment] {
        (env, state) => (Nil, state, env)
      }

    /**
     * Log a message (will be written to XDG_DATA_HOME)
     */
    def log(message: String): XdgRWS[Unit] =
      RWST[Id, XdgEnvironment, List[String], Map[String, String], Unit] {
        (env, state) => (List(message), state, ())
      }

    /**
     * Get a value from the state
     */
    def getState(key: String): XdgRWS[Option[String]] =
      RWST[Id, XdgEnvironment, List[String], Map[String, String], Option[String]] {
        (env, state) => (Nil, state, state.get(key))
      }

    /**
     * Set a value in the state
     */
    def setState(key: String, value: String): XdgRWS[Unit] =
      RWST[Id, XdgEnvironment, List[String], Map[String, String], Unit] {
        (env, state) => (Nil, state + (key -> value), ())
      }

    /**
     * Modify the entire state
     */
    def modifyState(f: Map[String, String] => Map[String, String]): XdgRWS[Unit] =
      RWST[Id, XdgEnvironment, List[String], Map[String, String], Unit] {
        (env, state) => (Nil, f(state), ())
      }

    /**
     * Get the current state
     */
    def getCurrentState: XdgRWS[Map[String, String]] =
      RWST[Id, XdgEnvironment, List[String], Map[String, String], Map[String, String]] {
        (env, state) => (Nil, state, state)
      }

    /**
     * Get the data directory path
     */
    def getDataDir: XdgRWS[Path] =
      askEnv.map(_.dataPath)

    /**
     * Get the config directory path
     */
    def getConfigDir: XdgRWS[Path] =
      askEnv.map(_.configPath)

    /**
     * Get the state directory path
     */
    def getStateDir: XdgRWS[Path] =
      askEnv.map(_.statePath)

    /**
     * Get the cache directory path
     */
    def getCacheDir: XdgRWS[Path] =
      askEnv.map(_.cachePath)

    /**
     * Pure value lifted into XdgRWS
     */
    def pure[A](a: A): XdgRWS[A] =
      RWST[Id, XdgEnvironment, List[String], Map[String, String], A] {
        (env, state) => (Nil, state, a)
      }

  /**
   * Runner for XDG monad computations
   */
  object XdgRunner:

    /**
     * Run an XDG computation with the given environment and initial state
     */
    def run[A](
      computation: XdgRWS[A],
      env: XdgEnvironment,
      initialState: Map[String, String] = Map.empty,
      appName: String = "xdg-app"
    ): Try[(List[String], Map[String, String], A)] =
      Try {
        // Ensure XDG directories exist
        ensureDirectories(env, appName)

        // Run the computation
        val (logs, finalState, result) = computation.run(env, initialState)

        // Persist logs to XDG_DATA_HOME
        persistLogs(env, appName, logs)

        // Persist state to XDG_STATE_HOME
        persistState(env, appName, finalState)

        (logs, finalState, result)
      }

    /**
     * Run and return only the result value
     */
    def runResult[A](
      computation: XdgRWS[A],
      env: XdgEnvironment,
      initialState: Map[String, String] = Map.empty,
      appName: String = "xdg-app"
    ): Try[A] =
      run(computation, env, initialState, appName).map(_._3)

    /**
     * Ensure all necessary XDG directories exist
     */
    private def ensureDirectories(env: XdgEnvironment, appName: String): Unit =
      List(
        env.dataPath.resolve(appName),
        env.statePath.resolve(appName),
        env.configPath.resolve(appName),
        env.cachePath.resolve(appName)
      ).foreach { path =>
        if (!Files.exists(path)) {
          Files.createDirectories(path)
        }
      }

    /**
     * Persist logs to XDG_DATA_HOME/appName/logs.txt
     */
    private def persistLogs(env: XdgEnvironment, appName: String, logs: List[String]): Unit =
      if (logs.nonEmpty) {
        val logFile = env.dataPath.resolve(appName).resolve("logs.txt")
        val timestamp = java.time.Instant.now().toString
        val logContent = logs.map(msg => s"[$timestamp] $msg").mkString("\n") + "\n"

        Files.write(
          logFile,
          logContent.getBytes(StandardCharsets.UTF_8),
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND
        )
      }

    /**
     * Persist state to XDG_STATE_HOME/appName/state.txt
     */
    private def persistState(env: XdgEnvironment, appName: String, state: Map[String, String]): Unit =
      val stateFile = env.statePath.resolve(appName).resolve("state.txt")
      val stateContent = state.map { case (k, v) => s"$k=$v" }.mkString("\n")

      Files.write(
        stateFile,
        stateContent.getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      )

    /**
     * Load state from XDG_STATE_HOME/appName/state.txt
     */
    def loadState(env: XdgEnvironment, appName: String): Map[String, String] =
      val stateFile = env.statePath.resolve(appName).resolve("state.txt")

      if (Files.exists(stateFile)) {
        val content = new String(Files.readAllBytes(stateFile), StandardCharsets.UTF_8)
        content.split("\n")
          .map(_.trim)
          .filter(_.nonEmpty)
          .map { line =>
            val parts = line.split("=", 2)
            if (parts.length == 2) Some(parts(0) -> parts(1))
            else None
          }
          .flatten
          .toMap
      } else {
        Map.empty
      }
