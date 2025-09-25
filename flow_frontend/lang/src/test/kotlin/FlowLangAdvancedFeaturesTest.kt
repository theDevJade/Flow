package com.thedevjade.io.flowlang

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.FlowLang
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangEngine
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class FlowLangAdvancedFeaturesTest {
    private lateinit var engine: FlowLangEngine

    @BeforeEach
    fun setup() {

        val field = FlowLangEngine::class.java.getDeclaredField("instance")
        field.isAccessible = true
        field.set(null, null)

        engine = FlowLangEngine.getInstance()
        FlowLang.start()
    }

    @Test
    fun testExplicitTypeDeclarations() {
        val ctx = FlowLangContext()


        engine.execute(
            """
            var name: String = "Hello"
            var age: Number = 25
            var isActive: Boolean = true
        """.trimIndent(), ctx
        )

        assertEquals("Hello", engine.execute("name", ctx))
        assertEquals(25.0, engine.execute("age", ctx))
        assertEquals(true, engine.execute("isActive", ctx))
    }

    @Test
    fun testFunctionOverloading() {
        val ctx = FlowLangContext()


        engine.execute(
            """
            function greet() {
                return "Hello!"
            }
            
            function greet(name) {
                return "Hello, " + name + "!"
            }
            
            function greet(name, age) {
                return "Hello, " + name + "! You are " + age + " years old."
            }
        """.trimIndent(), ctx
        )


        assertEquals("Hello!", engine.execute("greet()", ctx))
        assertEquals("Hello, Alice!", engine.execute("greet(\"Alice\")", ctx))
        assertEquals("Hello, Bob! You are 30 years old.", engine.execute("greet(\"Bob\", 30)", ctx))
    }

    @Test
    fun testDefaultParameterValues() {
        val ctx = FlowLangContext()


        engine.execute(
            """
            function createUser(name, age = 18, isActive = true) {
                return "User: " + name + ", Age: " + age + ", Active: " + isActive
            }
        """.trimIndent(), ctx
        )


        assertEquals("User: Alice, Age: 25, Active: true", engine.execute("createUser(\"Alice\", 25, true)", ctx))


        assertEquals("User: Bob, Age: 18, Active: true", engine.execute("createUser(\"Bob\")", ctx))
        assertEquals("User: Charlie, Age: 30, Active: true", engine.execute("createUser(\"Charlie\", 30)", ctx))
        assertEquals("User: David, Age: 18, Active: false", engine.execute("createUser(\"David\", 18, false)", ctx))
    }

    @Test
    fun testMixedTypeDeclarations() {
        val ctx = FlowLangContext()


        engine.execute(
            """
            var explicitString: String = "Hello"
            var implicitString = "World"
            var explicitNumber: Number = 42
            var implicitNumber = 3.14
            var explicitBoolean: Boolean = true
            var implicitBoolean = false
        """.trimIndent(), ctx
        )

        assertEquals("Hello", engine.execute("explicitString", ctx))
        assertEquals("World", engine.execute("implicitString", ctx))
        assertEquals(42.0, engine.execute("explicitNumber", ctx))
        assertEquals(3.14, engine.execute("implicitNumber", ctx))
        assertEquals(true, engine.execute("explicitBoolean", ctx))
        assertEquals(false, engine.execute("implicitBoolean", ctx))
    }

    @Test
    fun testComplexFunctionOverloading() {
        val ctx = FlowLangContext()


        engine.execute(
            """
            function calculate(a) {
                return a * a
            }
            
            function calculate(a, b) {
                return a + b
            }
            
            function calculate(a, b, operation = "add") {
                if (operation == "add") {
                    return a + b
                } else if (operation == "multiply") {
                    return a * b
                } else {
                    return a - b
                }
            }
        """.trimIndent(), ctx
        )


        assertEquals(25.0, engine.execute("calculate(5)", ctx))
        assertEquals(8.0, engine.execute("calculate(3, 5)", ctx))
        assertEquals(8.0, engine.execute("calculate(3, 5, \"add\")", ctx))
        assertEquals(15.0, engine.execute("calculate(3, 5, \"multiply\")", ctx))
        assertEquals(-2.0, engine.execute("calculate(3, 5, \"subtract\")", ctx))
    }

    @Test
    fun testTypeValidation() {
        val ctx = FlowLangContext()


        engine.execute(
            """
            var text: String = "Hello"
            var number: Number = 42
            var flag: Boolean = true
        """.trimIndent(), ctx
        )


        assertEquals("Hello", engine.execute("text", ctx))
        assertEquals(42.0, engine.execute("number", ctx))
        assertEquals(true, engine.execute("flag", ctx))
    }

    @Test
    fun testFunctionWithMixedParameterTypes() {
        val ctx = FlowLangContext()


        engine.execute(
            """
            function processData(data, count = 1, verbose = false) {
                var result = "Processing: " + data
                if (verbose) {
                    result = result + " (count: " + count + ")"
                }
                return result
            }
        """.trimIndent(), ctx
        )


        assertEquals("Processing: test", engine.execute("processData(\"test\")", ctx))
        assertEquals("Processing: test (count: 1)", engine.execute("processData(\"test\", 1, true)", ctx))
        assertEquals("Processing: data (count: 5)", engine.execute("processData(\"data\", 5, true)", ctx))
    }

    @Test
    fun testBuiltInFunctionOverloading() {

        val ctx = FlowLangContext()


        val output = captureOutput {
            engine.execute("print(\"Hello World\")", ctx)
        }
        assertTrue(output.contains("Hello World"))
    }

    @Test
    fun testErrorHandlingForOverloading() {
        val ctx = FlowLangContext()


        engine.execute(
            """
            function specific(a, b) {
                return a + b
            }
        """.trimIndent(), ctx
        )


        assertEquals(5.0, engine.execute("specific(2, 3)", ctx))


        assertThrows(Exception::class.java) {
            engine.execute("specific(2)", ctx)
        }

        assertThrows(Exception::class.java) {
            engine.execute("specific(2, 3, 4)", ctx)
        }
    }

    @Test
    fun testComplexDefaultValues() {
        val ctx = FlowLangContext()


        engine.execute(
            """
            function createMessage(greeting = "Hello", name = "World", punctuation = "!") {
                return greeting + ", " + name + punctuation
            }
        """.trimIndent(), ctx
        )


        assertEquals("Hello, World!", engine.execute("createMessage()", ctx))
        assertEquals("Hi, World!", engine.execute("createMessage(\"Hi\")", ctx))
        assertEquals("Hi, Alice!", engine.execute("createMessage(\"Hi\", \"Alice\")", ctx))
        assertEquals("Hi, Alice?", engine.execute("createMessage(\"Hi\", \"Alice\", \"?\")", ctx))
    }

    @Test
    fun testTypeInferenceWithDefaults() {
        val ctx = FlowLangContext()


        engine.execute(
            """
            function testDefaults(a = 10, b = "test", c = true) {
                return "a=" + a + ", b=" + b + ", c=" + c
            }
        """.trimIndent(), ctx
        )

        assertEquals("a=10, b=test, c=true", engine.execute("testDefaults()", ctx))
        assertEquals("a=5, b=test, c=true", engine.execute("testDefaults(5)", ctx))
        assertEquals("a=5, b=hello, c=true", engine.execute("testDefaults(5, \"hello\")", ctx))
        assertEquals("a=5, b=hello, c=false", engine.execute("testDefaults(5, \"hello\", false)", ctx))
    }

    private fun captureOutput(block: () -> Unit): String {
        val output = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(output))

        try {
            block()
        } finally {
            System.setOut(originalOut)
        }

        return output.toString()
    }
}
