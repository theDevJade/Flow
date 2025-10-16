package com.flowlang.bindings;

/**
 * Represents a Flow value (int, float, string, bool, etc.)
 * This class wraps the native FlowValue pointer
 */
public class FlowValue implements AutoCloseable {
    
    private long nativeHandle;
    private boolean closed = false;
    
    // Package-private constructor called from native code
    FlowValue(long nativeHandle) {
        this.nativeHandle = nativeHandle;
    }
    
    /**
     * Get the type of this value
     * @return The value type
     */
    public native FlowValueType getType();
    
    /**
     * Get this value as an integer
     * @return The integer value
     * @throws FlowException if type mismatch
     */
    public native long asInt() throws FlowException;
    
    /**
     * Get this value as a float
     * @return The float value
     * @throws FlowException if type mismatch
     */
    public native double asFloat() throws FlowException;
    
    /**
     * Get this value as a string
     * @return The string value
     * @throws FlowException if type mismatch
     */
    public native String asString() throws FlowException;
    
    /**
     * Get this value as a boolean
     * @return The boolean value
     * @throws FlowException if type mismatch
     */
    public native boolean asBool() throws FlowException;
    
    /**
     * Check if this value is null
     * @return true if null
     */
    public boolean isNull() {
        return getType() == FlowValueType.NULL;
    }
    
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
            throw new IllegalStateException("FlowValue has been closed");
        }
        return nativeHandle;
    }
    
    private native void freeNative(long handle);
    
    @Override
    public String toString() {
        try {
            FlowValueType type = getType();
            switch (type) {
                case INT:
                    return String.valueOf(asInt());
                case FLOAT:
                    return String.valueOf(asFloat());
                case STRING:
                    return asString();
                case BOOL:
                    return String.valueOf(asBool());
                case NULL:
                    return "null";
                default:
                    return type.name();
            }
        } catch (FlowException e) {
            return "FlowValue(error: " + e.getMessage() + ")";
        }
    }
}

