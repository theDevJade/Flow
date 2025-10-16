package com.flowlang.bindings;

/**
 * Flow value types matching FlowValueType in C API
 */
public enum FlowValueType {
    INT(0),
    FLOAT(1),
    STRING(2),
    BOOL(3),
    ARRAY(4),
    STRUCT(5),
    NULL(6);
    
    private final int value;
    
    FlowValueType(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    public static FlowValueType fromInt(int value) {
        for (FlowValueType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown FlowValueType: " + value);
    }
}

