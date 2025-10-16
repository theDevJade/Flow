public class QuickTest2 {
    public static void main(String[] args) {
        System.out.println("=== Testing Direct Library Load ===\n");
        
        // Load library directly with absolute path
        String libPath = "/Users/alyx/Flow/interop/java/libflowjni.dylib";
        System.out.println("Loading library from: " + libPath);
        System.load(libPath);
        System.out.println("✓ Library loaded successfully!\n");
        
        // Now try using the bindings
        try {
            com.flowlang.bindings.FlowRuntime runtime = new com.flowlang.bindings.FlowRuntime();
            System.out.println("✓ Runtime created\n");
            
            String code = "func add(a: int, b: int) -> int { return a + b; }";
            com.flowlang.bindings.FlowModule module = runtime.compile(code, "test");
            System.out.println("✓ Module compiled\n");
            
            int count = module.getFunctionCount();
            System.out.println("Function count: " + count);
            
            String[] functions = module.listFunctions();
            System.out.println("Functions: " + String.join(", ", functions));
            
            com.flowlang.bindings.FlowModule.FunctionInfo info = module.getFunctionInfo("add");
            System.out.println("Signature: " + info.toString());
            System.out.println("\n✓ Reflection working!\n");
            
            com.flowlang.bindings.FlowValue a = runtime.createInt(10);
            com.flowlang.bindings.FlowValue b = runtime.createInt(20);
            com.flowlang.bindings.FlowValue result = module.call(runtime, "add", a, b);
            
            long resultValue = result.asInt();
            System.out.println("add(10, 20) = " + resultValue);
            
            if (resultValue == 30) {
                System.out.println("\n=== ALL TESTS PASSED! ===");
            } else {
                System.out.println("\n✗ Expected 30, got " + resultValue);
            }
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

