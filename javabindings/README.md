# Flow Java Bindings

Java bindings for the [Flow programming language](../flowbase/README.md), allowing you to compile and execute Flow code from Java applications.

## Features

- ✅ Compile Flow source code from Java
- ✅ Load Flow modules from files
- ✅ Call Flow functions with type-safe values
- ✅ Automatic resource management with try-with-resources
- ✅ Full access to Flow's type system
- ✅ Exception-based error handling
- ✅ Cross-platform support (macOS, Linux, Windows)

## Requirements

- Java 11 or higher
- Gradle 7.0 or higher
- Flow compiler (flowbase must be built)

## Building

### 1. Build the Flow Compiler

First, build the Flow compiler:

```bash
cd ../flowbase
./build.sh
```

### 2. Build the Java Bindings

```bash
cd javabindings
./gradlew build
```

This will:
- Compile the Java wrapper classes
- Run all tests
- Create a JAR file in `build/libs/`

## Quick Start

### Basic Usage

```java
import com.flowlang.bindings.*;

public class Example {
    public static void main(String[] args) {
        // Create a runtime (auto-closeable)
        try (FlowRuntime runtime = new FlowRuntime()) {
            
            // Compile Flow code
            String source = """
                func add(a: int, b: int) -> int {
                    return a + b;
                }
                """;
            
            FlowModule module = runtime.compile(source, "math");
            
            // Call the function
            try (FlowValue a = runtime.createInt(10);
                 FlowValue b = runtime.createInt(20);
                 FlowValue result = module.call(runtime, "add", a, b)) {
                
                System.out.println("Result: " + result.asInt()); // 30
            }
            
        } catch (FlowException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
```

### Loading from File

```java
try (FlowRuntime runtime = new FlowRuntime()) {
    // Load a .flow file
    FlowModule module = runtime.loadFile("path/to/script.flow");
    
    // Get and call a function
    FlowFunction func = module.getFunction("main");
    if (func != null) {
        FlowValue result = func.call(runtime);
        System.out.println("Result: " + result);
    }
} catch (FlowException e) {
    e.printStackTrace();
}
```

### Working with Different Types

```java
try (FlowRuntime runtime = new FlowRuntime()) {
    // Create different value types
    FlowValue intVal = runtime.createInt(42);
    FlowValue floatVal = runtime.createFloat(3.14);
    FlowValue strVal = runtime.createString("Hello, Flow!");
    FlowValue boolVal = runtime.createBool(true);
    FlowValue nullVal = runtime.createNull();
    
    // Check types
    System.out.println(intVal.getType());     // INT
    System.out.println(floatVal.getType());   // FLOAT
    
    // Extract values
    long i = intVal.asInt();
    double f = floatVal.asFloat();
    String s = strVal.asString();
    boolean b = boolVal.asBool();
    
    // Remember to close
    intVal.close();
    floatVal.close();
    strVal.close();
    boolVal.close();
    nullVal.close();
    
} catch (FlowException e) {
    e.printStackTrace();
}
```

### Advanced: Function Objects

```java
try (FlowRuntime runtime = new FlowRuntime()) {
    String source = """
        func fibonacci(n: int) -> int {
            if (n <= 1) {
                return n;
            }
            let a: int = fibonacci(n - 1);
            let b: int = fibonacci(n - 2);
            return a + b;
        }
        """;
    
    FlowModule module = runtime.compile(source, "recursion");
    FlowFunction fib = module.getFunction("fibonacci");
    
    // Call the function multiple times
    for (int i = 0; i < 10; i++) {
        try (FlowValue n = runtime.createInt(i);
             FlowValue result = fib.call(runtime, n)) {
            System.out.println("fib(" + i + ") = " + result.asInt());
        }
    }
    
} catch (FlowException e) {
    e.printStackTrace();
}
```

## API Reference

### FlowRuntime

Main entry point for the Flow runtime.

**Methods:**
- `FlowRuntime()` - Create a new runtime
- `compile(String source, String moduleName)` - Compile Flow source code
- `loadFile(String path)` - Load a Flow module from file
- `createInt(long value)` - Create an integer value
- `createFloat(double value)` - Create a float value
- `createString(String value)` - Create a string value
- `createBool(boolean value)` - Create a boolean value
- `createNull()` - Create a null value
- `close()` - Clean up resources

### FlowModule

Represents a compiled Flow module.

**Methods:**
- `getName()` - Get the module name
- `getFunction(String name)` - Get a function by name
- `call(FlowRuntime runtime, String funcName, FlowValue... args)` - Call a function
- `close()` - Clean up resources

### FlowFunction

Represents a Flow function that can be called.

**Methods:**
- `getName()` - Get the function name
- `getParameterCount()` - Get the number of parameters
- `call(FlowRuntime runtime, FlowValue... args)` - Call the function

### FlowValue

Represents a value in Flow (int, float, string, etc.).

**Methods:**
- `getType()` - Get the value type
- `asInt()` - Extract as integer
- `asFloat()` - Extract as float
- `asString()` - Extract as string
- `asBool()` - Extract as boolean
- `isNull()` - Check if null
- `close()` - Clean up resources

### FlowValueType

Enum of Flow value types:
- `INT` - Integer
- `FLOAT` - Floating point
- `STRING` - String
- `BOOL` - Boolean
- `ARRAY` - Array
- `STRUCT` - Struct
- `NULL` - Null value

### FlowException

Exception thrown when Flow operations fail.

**Methods:**
- `getMessage()` - Get error message
- `getErrorCode()` - Get error code

## Project Structure

```
javabindings/
├── build.gradle              # Build configuration
├── settings.gradle           # Project settings
├── gradle.properties         # Gradle properties
├── README.md                 # This file
├── src/
│   ├── main/
│   │   ├── java/com/flowlang/bindings/
│   │   │   ├── FlowRuntime.java      # Main runtime class
│   │   │   ├── FlowModule.java       # Module wrapper
│   │   │   ├── FlowFunction.java     # Function wrapper
│   │   │   ├── FlowValue.java        # Value wrapper
│   │   │   ├── FlowValueType.java    # Value type enum
│   │   │   ├── FlowException.java    # Exception class
│   │   │   └── NativeLoader.java     # Native library loader
│   │   └── resources/
│   │       └── native/               # Bundled native libraries
│   └── test/
│       └── java/com/flowlang/bindings/
│           └── FlowRuntimeTest.java  # Unit tests
└── gradle/                   # Gradle wrapper
```

## Running Tests

```bash
./gradlew test
```

Tests will compile Flow code and verify the bindings work correctly.

## Publishing

To build a distributable JAR:

```bash
./gradlew jar
```

The JAR will be in `build/libs/flow-java-bindings-0.1.0.jar`.

To include sources and javadoc:

```bash
./gradlew build
```

This creates:
- `flow-java-bindings-0.1.0.jar` - Main JAR
- `flow-java-bindings-0.1.0-sources.jar` - Sources
- `flow-java-bindings-0.1.0-javadoc.jar` - Javadoc

## Integration with Maven/Gradle

### Gradle

```gradle
dependencies {
    implementation files('path/to/flow-java-bindings-0.1.0.jar')
}
```

### Maven

```xml
<dependency>
    <groupId>com.flowlang</groupId>
    <artifactId>flow-java-bindings</artifactId>
    <version>0.1.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/flow-java-bindings-0.1.0.jar</systemPath>
</dependency>
```

## Troubleshooting

### Native Library Not Found

If you get `UnsatisfiedLinkError`, make sure:

1. The Flow compiler is built: `cd ../flowbase && ./build.sh`
2. The native library is in your library path
3. Or set: `-Djava.library.path=../flowbase/build`

### Example Command

```bash
java -Djava.library.path=../flowbase/build -cp build/libs/flow-java-bindings-0.1.0.jar YourMainClass
```

## Contributing

Contributions are welcome! Please ensure:
- All tests pass: `./gradlew test`
- Code follows Java conventions
- Add tests for new features

## License

MIT License - See ../flowbase/README.md for details

## See Also

- [Flow Language Documentation](../flowbase/docs/)
- [Flow Compiler](../flowbase/)
- [VSCode Extension](../flowvscode/)

