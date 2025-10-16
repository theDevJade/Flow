

#include "flow.h"
#include "flow_reflect.h"
#include <stdio.h>
#include <string.h>
#include <assert.h>


static int tests_run = 0;
static int tests_passed = 0;
static int tests_failed = 0;

// Helper macros
#define TEST(name) \
    do { \
        printf("\n=== Test %d: %s ===\n", ++tests_run, name); \
    } while(0)

#define ASSERT(condition, message) \
    do { \
        if (!(condition)) { \
            printf("✗ FAILED: %s\n", message); \
            tests_failed++; \
            return 0; \
        } \
    } while(0)

#define TEST_PASS() \
    do { \
        printf("✓ PASSED\n"); \
        tests_passed++; \
        return 1; \
    } while(0)

// Test functions
int test_function_count()
{
    TEST("Function count in module");

    const char* source =
        "func add(a: int, b: int) -> int { return a + b; }\n"
        "func multiply(x: int, y: int) -> int { return x * y; }\n"
        "func negate(n: int) -> int { return -n; }";

    flow_module_t* mod = flow_compile_string(source);
    ASSERT(mod != NULL, "Module compilation failed");

    int count = flow_reflect_function_count(mod);
    ASSERT(count == 3, "Expected 3 functions");

    flow_unload_module(mod);
    TEST_PASS();
}

int test_list_functions()
{
    TEST("List all function names");

    const char* source =
        "func alpha(x: int) -> int { return x; }\n"
        "func beta(y: int) -> int { return y; }\n"
        "func gamma(z: int) -> int { return z; }";

    flow_module_t* mod = flow_compile_string(source);
    ASSERT(mod != NULL, "Module compilation failed");

    char** names = NULL;
    int count = flow_reflect_list_functions(mod, &names);
    ASSERT(count == 3, "Expected 3 functions");
    ASSERT(names != NULL, "Names array is NULL");

    // Check names (order might vary, so check all are present)
    int found_alpha = 0, found_beta = 0, found_gamma = 0;
    for (int i = 0; i < count; i++) {
        if (strcmp(names[i], "alpha") == 0) found_alpha = 1;
        if (strcmp(names[i], "beta") == 0) found_beta = 1;
        if (strcmp(names[i], "gamma") == 0) found_gamma = 1;
    }

    ASSERT(found_alpha && found_beta && found_gamma, "Not all functions found");

    flow_reflect_free_names(names, count);
    flow_unload_module(mod);
    TEST_PASS();
}

int test_function_signature()
{
    TEST("Get function signature");

    const char* source =
        "func add(a: int, b: int) -> int { return a + b; }";

    flow_module_t* mod = flow_compile_string(source);
    ASSERT(mod != NULL, "Module compilation failed");

    flow_function_info_t* info = flow_reflect_get_function_info(mod, "add");
    ASSERT(info != NULL, "Failed to get function info");
    ASSERT(strcmp(info->name, "add") == 0, "Wrong function name");
    ASSERT(strcmp(info->return_type, "int") == 0, "Wrong return type");
    ASSERT(info->param_count == 2, "Wrong parameter count");

    ASSERT(strcmp(info->params[0].name, "a") == 0, "Wrong first parameter name");
    ASSERT(strcmp(info->params[0].type, "int") == 0, "Wrong first parameter type");
    ASSERT(strcmp(info->params[1].name, "b") == 0, "Wrong second parameter name");
    ASSERT(strcmp(info->params[1].type, "int") == 0, "Wrong second parameter type");

    printf("  Function: %s(", info->name);
    for (int i = 0; i < info->param_count; i++) {
        printf("%s: %s", info->params[i].name, info->params[i].type);
        if (i < info->param_count - 1) printf(", ");
    }
    printf(") -> %s\n", info->return_type);

    flow_reflect_free_function_info(info);
    flow_unload_module(mod);
    TEST_PASS();
}

int test_no_parameters()
{
    TEST("Function with no parameters");

    const char* source = "func get_forty_two() -> int { return 42; }";

    flow_module_t* mod = flow_compile_string(source);
    ASSERT(mod != NULL, "Module compilation failed");

    flow_function_info_t* info = flow_reflect_get_function_info(mod, "get_forty_two");
    ASSERT(info != NULL, "Failed to get function info");
    ASSERT(info->param_count == 0, "Expected 0 parameters");
    ASSERT(info->params == NULL, "Params should be NULL for no parameters");

    printf("  Function: %s() -> %s\n", info->name, info->return_type);

    flow_reflect_free_function_info(info);
    flow_unload_module(mod);
    TEST_PASS();
}

int test_multiple_types()
{
    TEST("Function with multiple parameter types");

    const char* source =
        "func complex(a: int, b: float, c: bool) -> float { return b; }";

    flow_module_t* mod = flow_compile_string(source);
    ASSERT(mod != NULL, "Module compilation failed");

    flow_function_info_t* info = flow_reflect_get_function_info(mod, "complex");
    ASSERT(info != NULL, "Failed to get function info");
    ASSERT(info->param_count == 3, "Expected 3 parameters");

    ASSERT(strcmp(info->params[0].type, "int") == 0, "First param should be int");
    ASSERT(strcmp(info->params[1].type, "float") == 0, "Second param should be float");
    ASSERT(strcmp(info->params[2].type, "bool") == 0, "Third param should be bool");
    ASSERT(strcmp(info->return_type, "float") == 0, "Return should be float");

    printf("  Function: %s(", info->name);
    for (int i = 0; i < info->param_count; i++) {
        printf("%s: %s", info->params[i].name, info->params[i].type);
        if (i < info->param_count - 1) printf(", ");
    }
    printf(") -> %s\n", info->return_type);

    flow_reflect_free_function_info(info);
    flow_unload_module(mod);
    TEST_PASS();
}

int test_function_name_at()
{
    TEST("Get function name by index");

    const char* source =
        "func first(x: int) -> int { return x; }\n"
        "func second(x: int) -> int { return x; }";

    flow_module_t* mod = flow_compile_string(source);
    ASSERT(mod != NULL, "Module compilation failed");

    const char* name0 = flow_reflect_function_name_at(mod, 0);
    const char* name1 = flow_reflect_function_name_at(mod, 1);

    ASSERT(name0 != NULL, "First function name is NULL");
    ASSERT(name1 != NULL, "Second function name is NULL");

    printf("  Functions: %s, %s\n", name0, name1);

    // Out of bounds should return NULL
    const char* name_invalid = flow_reflect_function_name_at(mod, 10);
    ASSERT(name_invalid == NULL, "Out of bounds should return NULL");

    flow_unload_module(mod);
    TEST_PASS();
}

int test_large_module()
{
    TEST("Large module with many functions");

    const char* source =
        "func f1(x: int) -> int { return x; }\n"
        "func f2(x: int) -> int { return x; }\n"
        "func f3(x: int) -> int { return x; }\n"
        "func f4(x: int) -> int { return x; }\n"
        "func f5(x: int) -> int { return x; }\n"
        "func f6(x: int) -> int { return x; }\n"
        "func f7(x: int) -> int { return x; }\n"
        "func f8(x: int) -> int { return x; }\n"
        "func f9(x: int) -> int { return x; }\n"
        "func f10(x: int) -> int { return x; }";

    flow_module_t* mod = flow_compile_string(source);
    ASSERT(mod != NULL, "Module compilation failed");

    int count = flow_reflect_function_count(mod);
    ASSERT(count == 10, "Expected 10 functions");

    char** names = NULL;
    count = flow_reflect_list_functions(mod, &names);
    ASSERT(count == 10, "Expected 10 function names");

    for (int i = 0; i < count; i++) {
        ASSERT(names[i] != NULL, "Function name is NULL");
        printf("  - %s\n", names[i]);
    }

    flow_reflect_free_names(names, count);
    flow_unload_module(mod);
    TEST_PASS();
}

int test_nonexistent_function()
{
    TEST("Get info for non-existent function");

    const char* source = "func exists(x: int) -> int { return x; }";

    flow_module_t* mod = flow_compile_string(source);
    ASSERT(mod != NULL, "Module compilation failed");

    flow_function_info_t* info = flow_reflect_get_function_info(mod, "does_not_exist");
    ASSERT(info == NULL, "Should return NULL for non-existent function");

    printf("  Correctly returned NULL for non-existent function\n");

    flow_unload_module(mod);
    TEST_PASS();
}

int test_empty_module()
{
    TEST("Empty module reflection");

    const char* source = "// Empty module";

    flow_module_t* mod = flow_compile_string(source);
    ASSERT(mod != NULL, "Module compilation failed");

    int count = flow_reflect_function_count(mod);
    ASSERT(count == 0, "Empty module should have 0 functions");

    printf("  Empty module correctly reports 0 functions\n");

    flow_unload_module(mod);
    TEST_PASS();
}

int test_bidirectional_registration()
{
    TEST("Bidirectional reflection - register foreign module");

    // Simulate registering a Python module
    const char* py_funcs[] = {"sin", "cos", "sqrt", "pow"};
    int result = flow_reflect_register_foreign_module("python", "math", py_funcs, 4);
    ASSERT(result == 0, "Failed to register foreign module");

    // Check if module is available
    int available = flow_reflect_has_foreign_module("python", "math");
    ASSERT(available == 1, "Module should be available");

    // List functions
    char** names = NULL;
    int count = flow_reflect_foreign_functions("python", "math", &names);
    ASSERT(count == 4, "Expected 4 functions");

    printf("  Registered Python math module with functions:\n");
    for (int i = 0; i < count; i++) {
        printf("    - %s\n", names[i]);
    }

    flow_reflect_free_names(names, count);
    TEST_PASS();
}

int test_bidirectional_multiple_modules()
{
    TEST("Bidirectional reflection - multiple foreign modules");

    const char* go_funcs[] = {"ReadFile", "WriteFile"};
    const char* js_funcs[] = {"setTimeout", "clearTimeout", "fetch"};

    flow_reflect_register_foreign_module("go", "os", go_funcs, 2);
    flow_reflect_register_foreign_module("javascript", "global", js_funcs, 3);

    int has_go = flow_reflect_has_foreign_module("go", "os");
    int has_js = flow_reflect_has_foreign_module("javascript", "global");
    int has_fake = flow_reflect_has_foreign_module("fake", "module");

    ASSERT(has_go == 1, "Go module should be available");
    ASSERT(has_js == 1, "JS module should be available");
    ASSERT(has_fake == 0, "Fake module should not be available");

    printf("  Registered multiple foreign modules successfully\n");
    TEST_PASS();
}

int main()
{
    printf("╔══════════════════════════════════════════════════════════╗\n");
    printf("║       Flow Reflection API - Comprehensive Tests          ║\n");
    printf("╚══════════════════════════════════════════════════════════╝\n");

    // Initialize Flow runtime
    if (flow_init() != 0) {
        printf("✗ Failed to initialize Flow runtime: %s\n", flow_get_error());
        return 1;
    }
    printf("✓ Flow runtime initialized\n");

    // Run tests
    test_function_count();
    test_list_functions();
    test_function_signature();
    test_no_parameters();
    test_multiple_types();
    test_function_name_at();
    test_large_module();
    test_nonexistent_function();
    test_empty_module();
    test_bidirectional_registration();
    test_bidirectional_multiple_modules();

    // Cleanup
    flow_cleanup();

    // Print summary
    printf("\n╔══════════════════════════════════════════════════════════╗\n");
    printf("║                    TEST SUMMARY                          ║\n");
    printf("╚══════════════════════════════════════════════════════════╝\n");
    printf("  Total:  %d\n", tests_run);
    printf("  Passed: %d ✓\n", tests_passed);
    printf("  Failed: %d ✗\n", tests_failed);
    printf("\n");

    if (tests_failed == 0) {
        printf("🎉 ALL TESTS PASSED! 🎉\n\n");
        return 0;
    } else {
        printf("❌ SOME TESTS FAILED ❌\n\n");
        return 1;
    }
}

