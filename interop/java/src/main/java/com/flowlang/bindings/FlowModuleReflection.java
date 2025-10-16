package com.flowlang.bindings;

/**
 * JNI bridge for Flow module reflection
 * This class provides the native implementations for reflection methods
 */
class FlowModuleReflection {
    
    // Load native library
    static {
        NativeLoader.loadNativeLibrary();
    }
    
    /**
     * Get the number of functions in a module
     * @param moduleHandle Native handle to the module
     * @return Number of functions
     */
    static native int getFunctionCount(long moduleHandle);
    
    /**
     * List all function names in a module
     * @param moduleHandle Native handle to the module
     * @return Array of function names
     */
    static native String[] listFunctions(long moduleHandle);
    
    /**
     * Get detailed information about a function
     * @param moduleHandle Native handle to the module
     * @param functionName Name of the function
     * @return Function information array: [name, returnType, param1Name, param1Type, param2Name, param2Type, ...]
     * @throws FlowException if function not found
     */
    static native String[] getFunctionInfoNative(long moduleHandle, String functionName) throws FlowException;
}

