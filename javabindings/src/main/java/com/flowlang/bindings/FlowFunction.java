package com.flowlang.bindings;

/**
 * Represents a Flow function that can be called from Java
 */
public class FlowFunction {
    
    private final long nativeHandle;
    private final String name;
    
    // Package-private constructor
    FlowFunction(long nativeHandle, String name) {
        this.nativeHandle = nativeHandle;
        this.name = name;
    }
    
    /**
     * Get the name of this function
     * @return Function name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the number of parameters this function takes
     * @return Parameter count
     */
    public native int getParameterCount();
    
    /**
     * Call this function with the given arguments
     * @param runtime The runtime to execute in
     * @param args Arguments to pass
     * @return The return value
     * @throws FlowException if execution fails
     */
    public native FlowValue call(FlowRuntime runtime, FlowValue... args) throws FlowException;
    
    long getNativeHandle() {
        return nativeHandle;
    }
    
    @Override
    public String toString() {
        return "FlowFunction(" + name + ")";
    }
}

