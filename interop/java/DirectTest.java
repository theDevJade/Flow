/**
 * Direct test without using NativeLoader static blocks
 */
public class DirectTest {
    static {
        // Load library before any class loading
        String libPath = "/Users/alyx/Flow/interop/java/libflowjni.dylib";
        System.out.println("Loading: " + libPath);
        System.load(libPath);
        System.out.println("✓ Loaded\n");
    }
    
    // Minimal FlowModule clone to test reflection
    private static class TestModule {
        private long nativeHandle;
        
        TestModule(long handle) {
            this.nativeHandle = handle;
        }
        
        long getNativeHandle() {
            return nativeHandle;
        }
        
        public native int getFunctionCount();
    }
    
    public static void main(String[] args) {
        System.out.println("=== Direct JNI Test ===\n");
        
        try {
            // This will trigger the FlowRuntime static block
            com.flowlang.bindings.FlowRuntime runtime = new com.flowlang.bindings.FlowRuntime();
            System.out.println("✓ Runtime created\n");
            
            String code = "func add(a: int, b: int) -> int { return a + b; }\nfunc sub(x: int, y: int) -> int { return x - y; }";
            com.flowlang.bindings.FlowModule module = runtime.compile(code, "test");
            System.out.println("✓ Module compiled\n");
            
            // Try reflection
            System.out.println("Calling getFunctionCount()...");
            int count = module.getFunctionCount();
            System.out.println("Function count: " + count);
            System.out.println("\n✓ SUCCESS!");
            
        } catch (Throwable e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            
            // Try with test class
            System.out.println("\n--- Testing with minimal class ---");
            try {
                TestModule test = new TestModule(12345L);
                int count = test.getFunctionCount();
                System.out.println("Test count: " + count);
            } catch (Throwable e2) {
                e2.printStackTrace();
            }
        }
    }
}

