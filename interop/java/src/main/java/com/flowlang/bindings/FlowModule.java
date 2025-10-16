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
    
    // ========================================================================
    // REFLECTION API
    // ========================================================================
    
    /**
     * Get the number of functions in this module
     * @return Number of functions
     */
    public native int getFunctionCount();
    
    /**
     * List all function names in this module
     * @return Array of function names
     */
    public native String[] listFunctions();
    
    /**
     * Get detailed information about a function
     * @param functionName Name of the function
     * @return Function information
     * @throws FlowException if function not found
     */
    public native FunctionInfo getFunctionInfo(String functionName) throws FlowException;
    
    /**
     * Get a human-readable string representation of all functions in the module
     * @return Formatted string with function signatures
     */
    public String inspect() {
        String[] functions = listFunctions();
        
        if (functions == null || functions.length == 0) {
            return "Module contains no functions";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Module contains ").append(functions.length).append(" function(s):\n\n");
        
        for (String funcName : functions) {
            try {
                FunctionInfo info = getFunctionInfo(funcName);
                sb.append("  ").append(info.toString()).append("\n");
            } catch (FlowException e) {
                sb.append("  ").append(funcName).append(" (error getting info: ").append(e.getMessage()).append(")\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Information about a function's signature
     */
    public static class FunctionInfo {
        private final String name;
        private final String returnType;
        private final ParameterInfo[] parameters;
        
        public FunctionInfo(String name, String returnType, ParameterInfo[] parameters) {
            this.name = name;
            this.returnType = returnType;
            this.parameters = parameters;
        }
        
        public String getName() {
            return name;
        }
        
        public String getReturnType() {
            return returnType;
        }
        
        public ParameterInfo[] getParameters() {
            return parameters;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append("(");
            
            if (parameters != null && parameters.length > 0) {
                for (int i = 0; i < parameters.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(parameters[i].toString());
                }
            }
            
            sb.append(") -> ").append(returnType);
            return sb.toString();
        }
    }
    
    /**
     * Information about a function parameter
     */
    public static class ParameterInfo {
        private final String name;
        private final String type;
        
        public ParameterInfo(String name, String type) {
            this.name = name;
            this.type = type;
        }
        
        public String getName() {
            return name;
        }
        
        public String getType() {
            return type;
        }
        
        @Override
        public String toString() {
            return name + ": " + type;
        }
    }
    
    @Override
    public String toString() {
        return "FlowModule(" + name + ")";
    }
}

