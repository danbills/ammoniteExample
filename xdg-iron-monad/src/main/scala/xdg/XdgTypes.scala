package xdg

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import java.nio.file.{Path, Paths}

/**
 * Iron constraints for Unix directory paths
 */
object UnixPathConstraints:
  /**
   * Constraint for valid Unix path characters
   * Allows alphanumeric, dash, underscore, dot, and forward slash
   */
  type ValidUnixPath = Match["^[a-zA-Z0-9/_.-]+$"]

  /**
   * Constraint for absolute paths (must start with /)
   */
  type AbsolutePath = Match["^/.*$"]

  /**
   * Constraint for directory names (no slashes)
   */
  type DirectoryName = Match["^[a-zA-Z0-9_.-]+$"]

  /**
   * Combined constraint for valid absolute Unix directory path
   */
  type ValidAbsoluteUnixPath = ValidUnixPath & AbsolutePath

/**
 * XDG Base Directory Specification types using Iron
 */
object XdgTypes:
  import UnixPathConstraints.*

  // Constrained types for XDG paths
  opaque type XdgDataHome = String :| ValidAbsoluteUnixPath
  opaque type XdgConfigHome = String :| ValidAbsoluteUnixPath
  opaque type XdgStateHome = String :| ValidAbsoluteUnixPath
  opaque type XdgCacheHome = String :| ValidAbsoluteUnixPath
  opaque type XdgRuntimeDir = String :| ValidAbsoluteUnixPath

  object XdgDataHome:
    def apply(path: String :| ValidAbsoluteUnixPath): XdgDataHome = path
    def default(home: String): XdgDataHome =
      s"$home/.local/share".assume[ValidAbsoluteUnixPath]

  object XdgConfigHome:
    def apply(path: String :| ValidAbsoluteUnixPath): XdgConfigHome = path
    def default(home: String): XdgConfigHome =
      s"$home/.config".assume[ValidAbsoluteUnixPath]

  object XdgStateHome:
    def apply(path: String :| ValidAbsoluteUnixPath): XdgStateHome = path
    def default(home: String): XdgStateHome =
      s"$home/.local/state".assume[ValidAbsoluteUnixPath]

  object XdgCacheHome:
    def apply(path: String :| ValidAbsoluteUnixPath): XdgCacheHome = path
    def default(home: String): XdgCacheHome =
      s"$home/.cache".assume[ValidAbsoluteUnixPath]

  object XdgRuntimeDir:
    def apply(path: String :| ValidAbsoluteUnixPath): XdgRuntimeDir = path

  /**
   * Complete XDG environment specification
   */
  case class XdgEnvironment(
    dataHome: XdgDataHome,
    configHome: XdgConfigHome,
    stateHome: XdgStateHome,
    cacheHome: XdgCacheHome,
    runtimeDir: Option[XdgRuntimeDir]
  ):
    def toPath(xdgPath: XdgDataHome | XdgConfigHome | XdgStateHome | XdgCacheHome | XdgRuntimeDir): Path =
      Paths.get(xdgPath.toString)

    def dataPath: Path = toPath(dataHome)
    def configPath: Path = toPath(configHome)
    def statePath: Path = toPath(stateHome)
    def cachePath: Path = toPath(cacheHome)
    def runtimePath: Option[Path] = runtimeDir.map(toPath)

  object XdgEnvironment:
    /**
     * Create XDG environment from system environment variables
     */
    def fromEnv: XdgEnvironment =
      val home = sys.env.getOrElse("HOME", "/tmp")

      XdgEnvironment(
        dataHome = sys.env.get("XDG_DATA_HOME")
          .flatMap(_.refineOption[ValidAbsoluteUnixPath])
          .map(XdgDataHome.apply)
          .getOrElse(XdgDataHome.default(home)),

        configHome = sys.env.get("XDG_CONFIG_HOME")
          .flatMap(_.refineOption[ValidAbsoluteUnixPath])
          .map(XdgConfigHome.apply)
          .getOrElse(XdgConfigHome.default(home)),

        stateHome = sys.env.get("XDG_STATE_HOME")
          .flatMap(_.refineOption[ValidAbsoluteUnixPath])
          .map(XdgStateHome.apply)
          .getOrElse(XdgStateHome.default(home)),

        cacheHome = sys.env.get("XDG_CACHE_HOME")
          .flatMap(_.refineOption[ValidAbsoluteUnixPath])
          .map(XdgCacheHome.apply)
          .getOrElse(XdgCacheHome.default(home)),

        runtimeDir = sys.env.get("XDG_RUNTIME_DIR")
          .flatMap(_.refineOption[ValidAbsoluteUnixPath])
          .map(XdgRuntimeDir.apply)
      )

    /**
     * Create XDG environment with custom home directory
     */
    def withHome(home: String): XdgEnvironment =
      XdgEnvironment(
        dataHome = XdgDataHome.default(home),
        configHome = XdgConfigHome.default(home),
        stateHome = XdgStateHome.default(home),
        cacheHome = XdgCacheHome.default(home),
        runtimeDir = None
      )
