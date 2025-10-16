package com.flowlang.bindings;

/**
 * Exception thrown when Flow operations fail
 */
public class FlowException extends Exception {
    
    private final int errorCode;
    
    public FlowException(String message) {
        super(message);
        this.errorCode = -1;
    }
    
    public FlowException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public FlowException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = -1;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
}

