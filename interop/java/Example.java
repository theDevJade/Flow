import com.flowlang.bindings.*;

/**
 * Example program demonstrating Flow Java bindings
 * 
 * To run:
 * 1. Build the project: ./gradlew build
 * 2. Compile: javac -cp build/libs/flow-java-bindings-0.1.0.jar Example.java
 * 3. Run: java -Djava.library.path=../flowbase/build -cp .:build/libs/flow-java-bindings-0.1.0.jar Example
 */
public class Example {
    
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║   Flow Java Bindings - Example Program   ║");
        System.out.println("╚═══════════════════════════════════════════╝");
        System.out.println();
        
        try (FlowRuntime runtime = new FlowRuntime()) {
            // Example 1: Simple arithmetic
            simpleArithmetic(runtime);
            System.out.println();
            
            // Example 2: String manipulation
            stringManipulation(runtime);
            System.out.println();
            
            // Example 3: Multiple function calls
            multipleFunctions(runtime);
            System.out.println();
            
            System.out.println("✓ All examples completed successfully!");
            
        } catch (FlowException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static void simpleArithmetic(FlowRuntime runtime) throws FlowException {
        System.out.println("=== Example 1: Simple Arithmetic ===");
        
        String source = """
            func add(a: int, b: int) -> int {
                return a + b;
            }
            
            func multiply(a: int, b: int) -> int {
                return a * b;
            }
            """;
        
        try (FlowModule module = runtime.compile(source, "math")) {
            // Test addition
            try (FlowValue a = runtime.createInt(10);
                 FlowValue b = runtime.createInt(20);
                 FlowValue result = module.call(runtime, "add", a, b)) {
                System.out.println("add(10, 20) = " + result.asInt());
            }
            
            // Test multiplication
            try (FlowValue a = runtime.createInt(5);
                 FlowValue b = runtime.createInt(6);
                 FlowValue result = module.call(runtime, "multiply", a, b)) {
                System.out.println("multiply(5, 6) = " + result.asInt());
            }
        }
    }
    
    static void stringManipulation(FlowRuntime runtime) throws FlowException {
        System.out.println("=== Example 2: String Manipulation ===");
        
        String source = """
            func greet(name: string) -> string {
                return "Hello, " + name + "!";
            }
            """;
        
        try (FlowModule module = runtime.compile(source, "strings")) {
            String[] names = {"World", "Java", "Flow"};
            
            for (String name : names) {
                try (FlowValue nameVal = runtime.createString(name);
                     FlowValue result = module.call(runtime, "greet", nameVal)) {
                    System.out.println(result.asString());
                }
            }
        }
    }
    
    static void multipleFunctions(FlowRuntime runtime) throws FlowException {
        System.out.println("=== Example 3: Multiple Function Calls ===");
        
        String source = """
            func square(x: int) -> int {
                return x * x;
            }
            
            func cube(x: int) -> int {
                return x * x * x;
            }
            """;
        
        try (FlowModule module = runtime.compile(source, "power")) {
            int number = 5;
            
            try (FlowValue n = runtime.createInt(number);
                 FlowValue squared = module.call(runtime, "square", n)) {
                System.out.println(number + "² = " + squared.asInt());
            }
            
            try (FlowValue n = runtime.createInt(number);
                 FlowValue cubed = module.call(runtime, "cube", n)) {
                System.out.println(number + "³ = " + cubed.asInt());
            }
        }
    }
}

