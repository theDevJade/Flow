package com.flowlang.bindings;

/**
 * Represents a compiled Flow module
 */
public class FlowModule implements AutoCloseable {
    
    private long nativeHandle;
    private boolean closed = false;
    private final String name;
    
    // Package-private constructor
    FlowModule(long nativeHandle, String name) {
        this.nativeHandle = nativeHandle;
        this.name = name;
    }
    
    /**
     * Get the name of this module
     * @return Module name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get a function from this module by name
     * @param functionName Name of the function
     * @return The function, or null if not found
     */
    public native FlowFunction getFunction(String functionName);
    
    /**
     * Call a function in this module
     * @param runtime The runtime
     * @param functionName Name of the function to call
     * @param args Arguments to pass
     * @return The return value
     * @throws FlowException if function not found or execution fails
     */
    public native FlowValue call(FlowRuntime runtime, String functionName, FlowValue... args) throws FlowException;
    
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
            throw new IllegalStateException("FlowModule has been closed");
        }
        return nativeHandle;
    }
    
    private native void freeNative(long handle);
    
    @Override
    public String toString() {
        return "FlowModule(" + name + ")";
    }
}

