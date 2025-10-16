#include "flow.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void test_inline_compilation() {
    printf("=== Test 1: Inline String Compilation ===\n");

    const char* source =
        "func add(a: int, b: int) -> int {\n"
        "    return a + b;\n"
        "}\n";

    flow_module_t* mod = flow_compile_string(source);
    if (!mod) {
        printf("ERROR: Failed to compile: %s\n", flow_get_error());
        return;
    }

    flow_value_t result = flowc_call(mod, "add", 2, flow_int(10), flow_int(20));

    if (result.type == FLOW_VAL_INT) {
        printf("SUCCESS: add(10, 20) = %lld\n", flow_as_int(result));
    } else {
        printf("ERROR: Wrong return type or call failed: %s\n", flow_get_error());
    }

    flow_unload_module(mod);
    printf("\n");
}

void test_file_loading() {
    printf("=== Test 2: File Loading ===\n");

    flow_module_t* mod = flow_load_module("test_module.flow");
    if (!mod) {
        printf("ERROR: Failed to load module: %s\n", flow_get_error());
        return;
    }
    printf("SUCCESS: Module loaded\n");


    flow_value_t result1 = flowc_call(mod, "add", 2, flow_int(5), flow_int(3));
    if (result1.type == FLOW_VAL_INT) {
        printf("add(5, 3) = %lld\n", flow_as_int(result1));
    } else {
        printf("ERROR: add() failed: %s\n", flow_get_error());
    }

    flow_value_t result2 = flowc_call(mod, "multiply", 2, flow_int(6), flow_int(7));
    if (result2.type == FLOW_VAL_INT) {
        printf("multiply(6, 7) = %lld\n", flow_as_int(result2));
    } else {
        printf("ERROR: multiply() failed: %s\n", flow_get_error());
    }

    flow_value_t result3 = flowc_call(mod, "subtract", 2, flow_int(10), flow_int(3));
    if (result3.type == FLOW_VAL_INT) {
        printf("subtract(10, 3) = %lld\n", flow_as_int(result3));
    } else {
        printf("ERROR: subtract() failed: %s\n", flow_get_error());
    }


    flow_value_t result4 = flowc_call(mod, "is_positive", 2, flow_int(5), flow_int(0));
    if (result4.type == FLOW_VAL_BOOL) {
        printf("is_positive(5) = %s\n", flow_as_bool(result4) ? "true" : "false");
    } else {
        printf("ERROR: is_positive() failed: %s\n", flow_get_error());
    }

    flow_value_t result5 = flowc_call(mod, "is_positive", 2, flow_int(-3), flow_int(0));
    if (result5.type == FLOW_VAL_BOOL) {
        printf("is_positive(-3) = %s\n", flow_as_bool(result5) ? "true" : "false");
    } else {
        printf("ERROR: is_positive() failed: %s\n", flow_get_error());
    }


    flow_value_t result6 = flowc_call(mod, "square", 2, flow_float(4.0), flow_float(0.0));
    if (result6.type == FLOW_VAL_FLOAT) {
        printf("square(4.0) = %.2f\n", flow_as_float(result6));
    } else {
        printf("ERROR: square() failed: %s\n", flow_get_error());
    }

    flow_unload_module(mod);
    printf("\n");
}

void test_error_handling() {
    printf("=== Test 3: Error Handling ===\n");


    flow_cleanup();
    flow_module_t* mod = flow_load_module("nonexistent.flow");
    if (!mod) {
        printf("Expected error: %s\n", flow_get_error());
    }

    // Re-initialize
    if (flow_init() != 0) {
        printf("ERROR: Failed to re-initialize\n");
        return;
    }

    // Try to load non-existent file
    mod = flow_load_module("nonexistent.flow");
    if (!mod) {
        printf("Expected error for missing file: %s\n", flow_get_error());
    }

    // Try to compile invalid code
    const char* bad_source = "func invalid syntax here";
    mod = flow_compile_string(bad_source);
    if (!mod) {
        printf("Expected error for invalid syntax: %s\n", flow_get_error());
    }

    printf("\n");
}

int main() {
    printf("Flow C Library Test Suite\n");
    printf("==========================\n\n");

    // Initialize Flow runtime
    if (flow_init() != 0) {
        fprintf(stderr, "Failed to initialize Flow runtime: %s\n", flow_get_error());
        return 1;
    }
    printf("Flow runtime initialized successfully\n\n");

    // Run tests
    test_inline_compilation();
    test_file_loading();
    test_error_handling();

    // Cleanup
    flow_cleanup();
    printf("Flow runtime cleaned up\n");
    printf("\nAll tests completed!\n");

    return 0;
}

