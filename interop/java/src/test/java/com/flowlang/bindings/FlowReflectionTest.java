package com.flowlang.bindings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Flow Java bindings reflection API
 */
public class FlowReflectionTest {
    
    private FlowRuntime runtime;
    
    @BeforeEach
    public void setUp() throws FlowException {
        runtime = new FlowRuntime();
    }
    
    @AfterEach
    public void tearDown() {
        if (runtime != null) {
            runtime.close();
        }
    }
    
    @Test
    public void testFunctionCount() throws FlowException {
        String code = """
            func add(a: int, b: int) -> int {
                return a + b;
            }
            
            func subtract(x: int, y: int) -> int {
                return x - y;
            }
            
            func multiply(m: int, n: int) -> int {
                return m * n;
            }
            """;
        
        FlowModule module = runtime.compile(code, "test");
        int count = module.getFunctionCount();
        
        assertEquals(3, count, "Should have 3 functions");
        module.close();
    }
    
    @Test
    public void testListFunctions() throws FlowException {
        String code = """
            func greet(name: string) -> string {
                return "Hello, " + name;
            }
            
            func square(x: int) -> int {
                return x * x;
            }
            
            func is_positive(n: int) -> bool {
                return n > 0;
            }
            """;
        
        FlowModule module = runtime.compile(code, "test");
        String[] functions = module.listFunctions();
        
        assertNotNull(functions);
        assertEquals(3, functions.length, "Should have 3 function names");
        
        // Check all functions are present
        boolean hasGreet = false, hasSquare = false, hasIsPositive = false;
        for (String func : functions) {
            if ("greet".equals(func)) hasGreet = true;
            if ("square".equals(func)) hasSquare = true;
            if ("is_positive".equals(func)) hasIsPositive = true;
        }
        
        assertTrue(hasGreet, "Should include 'greet'");
        assertTrue(hasSquare, "Should include 'square'");
        assertTrue(hasIsPositive, "Should include 'is_positive'");
        
        module.close();
    }
    
    @Test
    public void testFunctionInfoWithParameters() throws FlowException {
        String code = """
            func add(a: int, b: int) -> int {
                return a + b;
            }
            """;
        
        FlowModule module = runtime.compile(code, "test");
        FlowModule.FunctionInfo info = module.getFunctionInfo("add");
        
        assertNotNull(info);
        assertEquals("add", info.getName());
        assertEquals("int", info.getReturnType());
        assertEquals(2, info.getParameters().length);
        
        assertEquals("a", info.getParameters()[0].getName());
        assertEquals("int", info.getParameters()[0].getType());
        assertEquals("b", info.getParameters()[1].getName());
        assertEquals("int", info.getParameters()[1].getType());
        
        module.close();
    }
    
    @Test
    public void testFunctionInfoStringParameter() throws FlowException {
        String code = """
            func greet(name: string) -> string {
                return "Hello, " + name;
            }
            """;
        
        FlowModule module = runtime.compile(code, "test");
        FlowModule.FunctionInfo info = module.getFunctionInfo("greet");
        
        assertEquals("greet", info.getName());
        assertEquals("string", info.getReturnType());
        assertEquals(1, info.getParameters().length);
        assertEquals("name", info.getParameters()[0].getName());
        assertEquals("string", info.getParameters()[0].getType());
        
        module.close();
    }
    
    @Test
    public void testFunctionInfoNoParameters() throws FlowException {
        String code = """
            func get_pi() -> float {
                return 3.14159;
            }
            """;
        
        FlowModule module = runtime.compile(code, "test");
        FlowModule.FunctionInfo info = module.getFunctionInfo("get_pi");
        
        assertEquals("get_pi", info.getName());
        assertEquals("float", info.getReturnType());
        assertEquals(0, info.getParameters().length);
        
        module.close();
    }
    
    @Test
    public void testFunctionInfoNonexistent() throws FlowException {
        String code = """
            func test() -> int {
                return 42;
            }
            """;
        
        FlowModule module = runtime.compile(code, "test");
        
        assertThrows(FlowException.class, () -> {
            module.getFunctionInfo("nonexistent");
        }, "Should throw FlowException for non-existent function");
        
        module.close();
    }
    
    @Test
    public void testInspectFunctions() throws FlowException {
        String code = """
            func add(a: int, b: int) -> int {
                return a + b;
            }
            
            func greet(name: string) -> string {
                return "Hello";
            }
            """;
        
        FlowModule module = runtime.compile(code, "test");
        String inspection = module.inspect();
        
        assertNotNull(inspection);
        assertTrue(inspection.contains("add(a: int, b: int) -> int"), "Should contain 'add' signature");
        assertTrue(inspection.contains("greet(name: string) -> string"), "Should contain 'greet' signature");
        assertTrue(inspection.contains("2 function(s)"), "Should mention 2 functions");
        
        module.close();
    }
    
    @Test
    public void testBooleanReturnTypes() throws FlowException {
        String code = """
            func is_positive(x: int) -> bool {
                return x > 0;
            }
            
            func is_even(n: int) -> bool {
                return n % 2 == 0;
            }
            """;
        
        FlowModule module = runtime.compile(code, "test");
        FlowModule.FunctionInfo info = module.getFunctionInfo("is_positive");
        
        assertEquals("bool", info.getReturnType());
        assertEquals(2, module.getFunctionCount());
        
        module.close();
    }
    
    @Test
    public void testLargeModule() throws FlowException {
        StringBuilder codeBuilder = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            codeBuilder.append("func func").append(i)
                       .append("(x: int) -> int { return x * ").append(i)
                       .append("; }\n");
        }
        
        FlowModule module = runtime.compile(codeBuilder.toString(), "large");
        int count = module.getFunctionCount();
        
        assertEquals(20, count, "Should have 20 functions");
        
        String[] functions = module.listFunctions();
        assertEquals(20, functions.length, "Should list 20 functions");
        
        FlowModule.FunctionInfo info0 = module.getFunctionInfo("func0");
        assertEquals("func0", info0.getName());
        
        FlowModule.FunctionInfo info19 = module.getFunctionInfo("func19");
        assertEquals("func19", info19.getName());
        
        module.close();
    }
    
    @Test
    public void testFunctionsStillCallableAfterReflection() throws FlowException {
        String code = """
            func add(a: int, b: int) -> int {
                return a + b;
            }
            
            func multiply(x: int, y: int) -> int {
                return x * y;
            }
            """;
        
        FlowModule module = runtime.compile(code, "test");
        
        // Do reflection
        int count = module.getFunctionCount();
        assertEquals(2, count);
        
        FlowModule.FunctionInfo info = module.getFunctionInfo("add");
        assertEquals("add", info.getName());
        
        // Now call the functions to ensure reflection didn't break them
        try (FlowValue a = runtime.createInt(10);
             FlowValue b = runtime.createInt(20);
             FlowValue result = module.call(runtime, "add", a, b)) {
            assertEquals(30, result.asInt(), "add(10, 20) should return 30");
        }
        
        try (FlowValue x = runtime.createInt(7);
             FlowValue y = runtime.createInt(6);
             FlowValue result = module.call(runtime, "multiply", x, y)) {
            assertEquals(42, result.asInt(), "multiply(7, 6) should return 42");
        }
        
        module.close();
    }
    
    @Test
    public void testMixedParameterTypes() throws FlowException {
        String code = """
            func process(a: int, b: float, c: string, d: bool) -> string {
                return "processed";
            }
            """;
        
        FlowModule module = runtime.compile(code, "test");
        FlowModule.FunctionInfo info = module.getFunctionInfo("process");
        
        assertEquals(4, info.getParameters().length);
        assertEquals("int", info.getParameters()[0].getType());
        assertEquals("float", info.getParameters()[1].getType());
        assertEquals("string", info.getParameters()[2].getType());
        assertEquals("bool", info.getParameters()[3].getType());
        
        module.close();
    }
    
    @Test
    public void testEmptyModule() throws FlowException {
        String code = "// Just a comment";
        
        FlowModule module = runtime.compile(code, "empty");
        int count = module.getFunctionCount();
        
        assertEquals(0, count, "Empty module should have 0 functions");
        
        String[] functions = module.listFunctions();
        assertNotNull(functions);
        assertEquals(0, functions.length, "Empty module should list 0 functions");
        
        String inspection = module.inspect();
        assertTrue(inspection.toLowerCase().contains("no functions"), "Should mention no functions");
        
        module.close();
    }
    
    @Test
    public void testFloatReturnType() throws FlowException {
        String code = """
            func calculate_pi() -> float {
                return 3.14159;
            }
            """;
        
        FlowModule module = runtime.compile(code, "test");
        FlowModule.FunctionInfo info = module.getFunctionInfo("calculate_pi");
        
        assertEquals("float", info.getReturnType());
        assertEquals(0, info.getParameters().length);
        
        module.close();
    }
    
    @Test
    public void testMultipleStringFunctions() throws FlowException {
        String code = """
            func concat(a: string, b: string) -> string {
                return a + b;
            }
            
            func repeat(s: string, n: int) -> string {
                return s;
            }
            """;
        
        FlowModule module = runtime.compile(code, "test");
        
        FlowModule.FunctionInfo info1 = module.getFunctionInfo("concat");
        assertEquals(2, info1.getParameters().length);
        assertEquals("string", info1.getParameters()[0].getType());
        assertEquals("string", info1.getParameters()[1].getType());
        
        FlowModule.FunctionInfo info2 = module.getFunctionInfo("repeat");
        assertEquals(2, info2.getParameters().length);
        assertEquals("string", info2.getParameters()[0].getType());
        assertEquals("int", info2.getParameters()[1].getType());
        
        module.close();
    }
}

