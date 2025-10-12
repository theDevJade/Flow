package com.flowlang.bindings;

import java.nio.file.Path;

/**
 * Main entry point for the Flow runtime
 * <p>
 * Example usage:
 * <pre>{@code
 * try (FlowRuntime runtime = new FlowRuntime()) {
 *     // Compile and load a module
 *     FlowModule module = runtime.compile("func add(a: int, b: int) -> int { return a + b; }", "example");
 *     
 *     // Call a function
 *     try (FlowValue a = runtime.createInt(10);
 *          FlowValue b = runtime.createInt(20);
 *          FlowValue result = module.call(runtime, "add", a, b)) {
 *         System.out.println("Result: " + result.asInt());
 *     }
 * }
 * }</pre>
 */
public class FlowRuntime implements AutoCloseable {
    
    static {
        NativeLoader.loadNativeLibrary();
    }
    
    private long nativeHandle;
    private boolean closed = false;
    
    /**
     * Create a new Flow runtime instance
     * @throws FlowException if runtime creation fails
     */
    public FlowRuntime() throws FlowException {
        this.nativeHandle = createNative();
        if (this.nativeHandle == 0) {
            throw new FlowException("Failed to create Flow runtime");
        }
    }
    
    /**
     * Compile Flow source code into a module
     * @param source Flow source code
     * @param moduleName Name for the module
     * @return Compiled module
     * @throws FlowException if compilation fails
     */
    public native FlowModule compile(String source, String moduleName) throws FlowException;
    
    /**
     * Load a Flow module from a file
     * @param filePath Path to .flow file
     * @return Loaded module
     * @throws FlowException if loading fails
     */
    public FlowModule loadFile(String filePath) throws FlowException {
        return loadFileNative(filePath);
    }
    
    /**
     * Load a Flow module from a Path
     * @param path Path to .flow file
     * @return Loaded module
     * @throws FlowException if loading fails
     */
    public FlowModule loadFile(Path path) throws FlowException {
        return loadFile(path.toString());
    }
    
    /**
     * Get the last error message from the runtime
     * @return Error message or null
     */
    public native String getLastError();
    
    // ========================================
    // Value Creation Methods
    // ========================================
    
    /**
     * Create a new integer value
     * @param value The integer
     * @return Flow value
     */
    public native FlowValue createInt(long value);
    
    /**
     * Create a new float value
     * @param value The float
     * @return Flow value
     */
    public native FlowValue createFloat(double value);
    
    /**
     * Create a new string value
     * @param value The string
     * @return Flow value
     */
    public native FlowValue createString(String value);
    
    /**
     * Create a new boolean value
     * @param value The boolean
     * @return Flow value
     */
    public native FlowValue createBool(boolean value);
    
    /**
     * Create a null value
     * @return Flow null value
     */
    public native FlowValue createNull();
    
    @Override
    public void close() {
        if (!closed && nativeHandle != 0) {
            freeNative(nativeHandle);
            nativeHandle = 0;
            closed = true;
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
    
    long getNativeHandle() {
        if (closed) {
            throw new IllegalStateException("FlowRuntime has been closed");
        }
        return nativeHandle;
    }
    
    // Native methods
    private native long createNative();
    private native void freeNative(long handle);
    private native FlowModule loadFileNative(String filePath) throws FlowException;
    
    @Override
    public String toString() {
        return "FlowRuntime(handle=" + nativeHandle + ", closed=" + closed + ")";
    }
}

