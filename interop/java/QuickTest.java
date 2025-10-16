import com.flowlang.bindings.*;

public class QuickTest {
    public static void main(String[] args) {
        System.out.println("=== Flow Java Bindings Quick Test ===\n");
        
        try {
            // Create runtime
            System.out.println("1. Creating Flow runtime...");
            FlowRuntime runtime = new FlowRuntime();
            System.out.println("   ✓ Runtime created successfully\n");
            
            // Compile simple module
            System.out.println("2. Compiling Flow module...");
            String code = "func add(a: int, b: int) -> int { return a + b; }";
            FlowModule module = runtime.compile(code, "test");
            System.out.println("   ✓ Module compiled successfully\n");
            
            // Test reflection
            System.out.println("3. Testing reflection API...");
            int count = module.getFunctionCount();
            System.out.println("   Function count: " + count);
            
            String[] functions = module.listFunctions();
            System.out.println("   Functions: " + String.join(", ", functions));
            
            FlowModule.FunctionInfo info = module.getFunctionInfo("add");
            System.out.println("   Function signature: " + info.toString());
            System.out.println("   ✓ Reflection working!\n");
            
            // Test function call
            System.out.println("4. Testing function call...");
            FlowValue a = runtime.createInt(10);
            FlowValue b = runtime.createInt(20);
            FlowValue result = module.call(runtime, "add", a, b);
            
            long resultValue = result.asInt();
            System.out.println("   add(10, 20) = " + resultValue);
            
            if (resultValue == 30) {
                System.out.println("   ✓ Function call working!\n");
            } else {
                System.out.println("   ✗ Expected 30, got " + resultValue + "\n");
            }
            
            // Cleanup
            result.close();
            a.close();
            b.close();
            module.close();
            runtime.close();
            
            System.out.println("=== ALL TESTS PASSED! ===");
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

