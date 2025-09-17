package com.thedevjade.io.flowlang

import com.thedevjade.io.flowlang.language.FlowLangEngine
import com.thedevjade.io.flowlang.language.memory.FlowLangContext
import com.thedevjade.io.flowlang.language.memory.FlowLangEvent
import com.thedevjade.io.flowlang.language.memory.FlowLangFunction
import com.thedevjade.io.flowlang.language.memory.FlowLangParameter
import com.thedevjade.io.flowlang.language.parsing.Preprocessor
import com.thedevjade.io.flowlang.language.types.Vector3
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class FlowLangIntegrationTest {
    private lateinit var engine: FlowLangEngine
    private lateinit var preprocessor: Preprocessor

    @BeforeEach
    fun setup() {
        // Reset the singleton instance
        val field = FlowLangEngine::class.java.getDeclaredField("instance")
        field.isAccessible = true
        field.set(null, null)
        
        engine = FlowLangEngine.getInstance()
        preprocessor = Preprocessor()
        FlowLang.start()
    }

    @AfterEach
    fun cleanup() {
        FlowLang.stop()
    }

    @Test
    fun testCompleteFlowLangWorkflow() {
        // Test 1: Basic script execution
        val basicScript = """
            print("Testing FlowLang library...")
            var x = 10
            var y = 20
            var sum = x + y
            print("Sum: " + sum)
        """.trimIndent()
        
        val output = captureOutput { engine.execute(basicScript) }
        assertTrue(output.contains("Testing FlowLang library..."))
        assertTrue(output.contains("Sum: 30"))
    }

    @Test
    fun testNaturalLanguageProcessing() {
        val naturalLanguageScript = """
            set total to 100 plus 50
            if total is greater than 100
                print("Total is high!")
            end if
            set result to 10 times 5
            print("Result: " + result)
        """.trimIndent()
        
        val processedScript = preprocessor.process(naturalLanguageScript)
        
        // Verify the preprocessing worked
        assertTrue(processedScript.contains("var total = 100 + 50"))
        assertTrue(processedScript.contains("if (total > 100)"))
        assertTrue(processedScript.contains("var result = 10 * 5"))
        
        // Execute the processed script
        val output = captureOutput { engine.execute(processedScript) }
        assertTrue(output.contains("Total is high!"))
        assertTrue(output.contains("Result: 50"))
    }

    @Test
    fun testCustomFunctionsAndTypes() {
        // Register custom function
        engine.registerFunction(FlowLangFunction("calculateArea", { args ->
            val length = (args[0] as? Number)?.toDouble() ?: 0.0
            val width = (args[1] as? Number)?.toDouble() ?: 0.0
            length * width
        }, arrayOf(
            FlowLangParameter("length", "number"),
            FlowLangParameter("width", "number")
        )))
        
        // Register custom type
        engine.registerType(com.thedevjade.io.flowlang.language.memory.FlowLangType("Rectangle", Rectangle::class.java))
        
        val script = """
            var area = calculateArea(5, 3)
            print("Area: " + area)
        """.trimIndent()
        
        val output = captureOutput { engine.execute(script) }
        assertTrue(output.contains("Area: 15"))
    }

    @Test
    fun testEventSystem() {
        // Register event
        engine.registerEvent(FlowLangEvent("dataProcessed", arrayOf(
            FlowLangParameter("data", "text"),
            FlowLangParameter("count", "number")
        )))
        
        // Set up event handler
        engine.execute("""
            on dataProcessed {
                print("Processed " + count + " items: " + data)
            }
        """.trimIndent())
        
        // Trigger event
        val output = captureOutput { 
            engine.triggerEvent("dataProcessed", "test data", 42)
        }
        assertTrue(output.contains("Processed 42 items: test data"))
    }

    @Test
    fun testComplexScript() {
        val complexScript = """
            # Complex FlowLang script demonstrating all features
            print("Starting complex script...")
            
            # Variables and arithmetic
            var x = 10
            var y = 20
            var z = x + y * 2
            print("z = " + z)
            
            # Control flow
            if (z > 30) {
                print("z is greater than 30")
                var temp = z
                while (temp > 0) {
                    print("temp = " + temp)
                    temp = temp - 10
                }
            } else {
                print("z is not greater than 30")
            }
            
            # Functions
            function factorial(n) {
                if (n <= 1) {
                    return 1
                } else {
                    return n * factorial(n - 1)
                }
            }
            
            var fact5 = factorial(5)
            print("5! = " + fact5)
            
            # String operations
            var message = "Hello " + "FlowLang" + "!"
            print(message)
            
            print("Complex script completed!")
        """.trimIndent()
        
        val output = captureOutput { engine.execute(complexScript) }
        assertTrue(output.contains("Starting complex script..."))
        assertTrue(output.contains("z = 50"))
        assertTrue(output.contains("z is greater than 30"))
        assertTrue(output.contains("5! = 120"))
        assertTrue(output.contains("Hello FlowLang!"))
        assertTrue(output.contains("Complex script completed!"))
    }

    @Test
    fun testPreprocessorCustomRules() {
        val customPreprocessor = Preprocessor(registerDefaults = false)
        
        // Add custom rules
        customPreprocessor.registerPhraseReplacement("\\bset\\s+([A-Za-z_]\\w*)\\s+to\\b", "var $1 =")
        customPreprocessor.registerPhraseReplacement("\\btwice\\s+as\\s+big\\b", "* 2")
        customPreprocessor.registerTokenReplacement("double", "* 2")
        customPreprocessor.registerRemovableKeyword("please")
        
        val input = "please set width to base twice as big"
        val output = customPreprocessor.process(input)
        
        assertEquals("var width = base * 2", output.trim())
    }

    @Test
    fun testErrorHandling() {
        // Test syntax error
        assertThrows(Exception::class.java) {
            engine.execute("var x = ") // Incomplete statement
        }
        
        // Test undefined variable
        assertThrows(Exception::class.java) {
            engine.execute("print(undefinedVariable)")
        }
        
        // Test undefined function
        assertThrows(Exception::class.java) {
            engine.execute("undefinedFunction()")
        }
    }

    @Test
    fun testContextIsolation() {
        val ctx1 = FlowLangContext()
        val ctx2 = FlowLangContext()
        
        engine.execute("var x = 10", ctx1)
        engine.execute("var x = 20", ctx2)
        
        assertEquals(10.0, engine.execute("x", ctx1))
        assertEquals(20.0, engine.execute("x", ctx2))
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

// Test data class
data class Rectangle(
    val length: Double,
    val width: Double
) {
    val area: Double get() = length * width
}
