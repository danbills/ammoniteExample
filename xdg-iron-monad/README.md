# XDG Iron Monad

A Scala 3 project that models the XDG Base Directory Specification using Iron types and implements a Reader-Writer-State (RWS) monad for managing XDG-compliant application state.

## Overview

This project demonstrates:

- **Iron Types (v3.2.0)**: Fully constrained types for Unix directory paths
- **XDG Specification**: Complete implementation of XDG Base Directory Specification
- **RWS Monad**: Reader-Writer-State monad from ScalaZ for functional state management

## Features

### XDG Directory Specification

The project models all XDG directories with type-safe Iron constraints:

- `XDG_DATA_HOME` - User-specific data files (default: `~/.local/share`)
- `XDG_CONFIG_HOME` - User-specific configuration files (default: `~/.config`)
- `XDG_STATE_HOME` - User-specific state data (default: `~/.local/state`)
- `XDG_CACHE_HOME` - User-specific cache data (default: `~/.cache`)
- `XDG_RUNTIME_DIR` - User-specific runtime files

### Iron Type Constraints

All directory paths are validated using Iron's refinement types:

```scala
type ValidUnixPath = Match["^[a-zA-Z0-9/_.-]+$"]
type AbsolutePath = Match["^/.*$"]
type ValidAbsoluteUnixPath = ValidUnixPath & AbsolutePath
```

### RWS Monad

The `XdgRWS` monad combines three effects:

- **Reader**: Provides access to `XdgEnvironment` configuration
- **Writer**: Logs messages that are persisted to `XDG_DATA_HOME`
- **State**: Application state persisted to `XDG_STATE_HOME`

## Project Structure

```
xdg-iron-monad/
├── build.sbt                      # SBT build configuration
├── project/
│   └── build.properties           # SBT version (1.11.0)
└── src/main/scala/xdg/
    ├── XdgTypes.scala             # Iron type definitions for XDG
    ├── XdgMonad.scala             # RWS monad implementation
    └── Example.scala              # Example usage and main entry point
```

## Requirements

- Scala 3.8.0
- SBT 1.11.0
- Iron 3.2.0
- ScalaZ 7.3.8

## Building

```bash
cd xdg-iron-monad
sbt compile
```

## Running

```bash
sbt run
```

This will run the example application that demonstrates:

1. Reading XDG environment configuration
2. Logging operations to the data directory
3. Managing application state in the state directory
4. User profile management
5. Directory information queries

## Example Usage

```scala
import xdg.XdgMonad.*
import xdg.XdgMonad.XdgOps.*

// Define a computation using the XDG monad
val computation: XdgRWS[String] = for {
  env <- askEnv                           // Reader: Get XDG environment
  _ <- log("Application started")         // Writer: Log message
  count <- getState("visit_count")        // State: Get counter
  _ <- setState("visit_count", "1")       // State: Update counter
} yield "Done"

// Run the computation
val env = XdgEnvironment.fromEnv
XdgRunner.run(computation, env, Map.empty, "my-app")
```

## Output

The application creates XDG-compliant directories and files:

```
~/.local/share/xdg-example/logs.txt    # Writer logs
~/.local/state/xdg-example/state.txt   # Application state
```

## Type Safety

All directory paths are validated at compile-time where possible, and at runtime using Iron's refinement types:

```scala
// This will compile and validate at runtime
val dataHome: XdgDataHome = "/home/user/.local/share"
  .refineUnsafe[ValidAbsoluteUnixPath]

// This will fail at runtime
val invalid = "not-absolute"
  .refineUnsafe[ValidAbsoluteUnixPath]  // Throws exception
```

## Architecture

### XdgTypes.scala

Defines Iron-constrained types for:
- Unix path validation
- XDG directory types (opaque types)
- XDG environment configuration

### XdgMonad.scala

Implements the RWS monad with:
- `XdgRWS[A]` type alias
- Monadic operations (askEnv, log, getState, setState)
- Runner with automatic persistence

### Example.scala

Demonstrates practical usage:
- Visit counter with persistent state
- User profile management
- XDG directory queries
- Logging and state persistence

## License

MIT
