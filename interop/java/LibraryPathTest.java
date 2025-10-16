public class LibraryPathTest {
    public static void main(String[] args) {
        System.out.println("=== Library Path Investigation ===\n");
        
        System.out.println("java.library.path:");
        String libPath = System.getProperty("java.library.path");
        for (String path : libPath.split(":")) {
            System.out.println("  " + path);
        }
        
        System.out.println("\nCurrent directory: " + System.getProperty("user.dir"));
        
        System.out.println("\nLibrary name for 'flowjni': " + System.mapLibraryName("flowjni"));
        
        System.out.println("\nAttempting to load 'flowjni'...");
        try {
            System.loadLibrary("flowjni");
            System.out.println("✓ Successfully loaded 'flowjni'\n");
            
            // Check if reflection methods are available
            System.out.println("Checking for FlowModule.getFunctionCount in loaded library...");
            try {
                Class<?> clazz = Class.forName("com.flowlang.bindings.FlowModule");
                java.lang.reflect.Method method = clazz.getDeclaredMethod("getFunctionCount");
                System.out.println("  Method found: " + method);
                System.out.println("  Native: " + java.lang.reflect.Modifier.isNative(method.getModifiers()));
            } catch (Exception e) {
                System.out.println("  Error: " + e.getMessage());
            }
            
        } catch (UnsatisfiedLinkError e) {
            System.out.println("✗ Failed to load: " + e.getMessage());
        }
    }
}

