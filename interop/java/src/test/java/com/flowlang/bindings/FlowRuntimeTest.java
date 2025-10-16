package com.flowlang.bindings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Flow Java bindings
 */
public class FlowRuntimeTest {
    
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
    public void testRuntimeCreation() {
        assertNotNull(runtime);
    }
    
    @Test
    public void testCreateIntValue() {
        try (FlowValue value = runtime.createInt(42)) {
            assertEquals(FlowValueType.INT, value.getType());
            assertEquals(42, value.asInt());
        } catch (FlowException e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }
    
    @Test
    public void testCreateFloatValue() {
        try (FlowValue value = runtime.createFloat(3.14)) {
            assertEquals(FlowValueType.FLOAT, value.getType());
            assertEquals(3.14, value.asFloat(), 0.001);
        } catch (FlowException e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }
    
    @Test
    public void testCreateStringValue() {
        try (FlowValue value = runtime.createString("Hello, Flow!")) {
            assertEquals(FlowValueType.STRING, value.getType());
            assertEquals("Hello, Flow!", value.asString());
        } catch (FlowException e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }
    
    @Test
    public void testCreateBoolValue() {
        try (FlowValue value = runtime.createBool(true)) {
            assertEquals(FlowValueType.BOOL, value.getType());
            assertTrue(value.asBool());
        } catch (FlowException e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }
    
    @Test
    public void testCreateNullValue() {
        try (FlowValue value = runtime.createNull()) {
            assertEquals(FlowValueType.NULL, value.getType());
            assertTrue(value.isNull());
        }
    }
    
    @Test
    public void testCompileSimpleFunction() {
        String source = 
            "func add(a: int, b: int) -> int {" +
            "    return a + b;" +
            "}";
        
        try (FlowModule module = runtime.compile(source, "test")) {
            assertNotNull(module);
            assertEquals("test", module.getName());
            
            FlowFunction addFunc = module.getFunction("add");
            assertNotNull(addFunc);
            assertEquals("add", addFunc.getName());
        } catch (FlowException e) {
            fail("Compilation should succeed: " + e.getMessage());
        }
    }
    
    @Test
    public void testCallFunction() {
        String source = 
            "func add(a: int, b: int) -> int {" +
            "    return a + b;" +
            "}";
        
        try (FlowModule module = runtime.compile(source, "test")) {
            try (FlowValue a = runtime.createInt(10);
                 FlowValue b = runtime.createInt(20);
                 FlowValue result = module.call(runtime, "add", a, b)) {
                
                assertEquals(30, result.asInt());
            }
        } catch (FlowException e) {
            fail("Function call should succeed: " + e.getMessage());
        }
    }
    
    @Test
    public void testMultipleFunctions() {
        String source = 
            "func add(a: int, b: int) -> int { return a + b; }" +
            "func multiply(a: int, b: int) -> int { return a * b; }";
        
        try (FlowModule module = runtime.compile(source, "math")) {
            // Test add
            try (FlowValue a = runtime.createInt(5);
                 FlowValue b = runtime.createInt(3);
                 FlowValue result = module.call(runtime, "add", a, b)) {
                assertEquals(8, result.asInt());
            }
            
            // Test multiply
            try (FlowValue a = runtime.createInt(5);
                 FlowValue b = runtime.createInt(3);
                 FlowValue result = module.call(runtime, "multiply", a, b)) {
                assertEquals(15, result.asInt());
            }
        } catch (FlowException e) {
            fail("Function calls should succeed: " + e.getMessage());
        }
    }
}

