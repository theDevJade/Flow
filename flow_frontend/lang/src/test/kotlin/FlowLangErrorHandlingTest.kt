package com.thedevjade.io.flowlang

import com.thedevjade.flow.flowlang.language.parsing.EnhancedSyntaxException
import com.thedevjade.flow.flowlang.language.parsing.ErrorType
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.FlowLang
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangEngine
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangContext
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.parsing.FlowLangParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlowLangErrorHandlingTest {
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
    fun testUnterminatedStringError() {
        val code = """
            var message = "Hello World
            print(message)
        """.trimIndent()

        val parser = FlowLangParser()

        assertThrows(EnhancedSyntaxException::class.java) {
            parser.parse(code)
        }


        try {
            parser.parse(code)
        } catch (e: EnhancedSyntaxException) {
            val error = e.toFlowLangError()
            assertEquals(ErrorType.UNTERMINATED_STRING, error.type)
            assertTrue(error.message.contains("Unterminated string"))
            assertTrue(error.suggestions.isNotEmpty())
            assertTrue(error.suggestions.any { it.contains("closing quote") })
        }
    }

    @Test
    fun testUnexpectedTokenError() {
        val code = """
            var x = 5
            if x > 3 {
                print("x is greater than 3")
            }
        """.trimIndent()

        val parser = FlowLangParser()

        assertThrows(EnhancedSyntaxException::class.java) {
            parser.parse(code)
        }

        try {
            parser.parse(code)
        } catch (e: EnhancedSyntaxException) {
            val error = e.toFlowLangError()
            assertEquals(ErrorType.UNEXPECTED_TOKEN, error.type)
            assertTrue(error.message.contains("Expected"))
            assertTrue(error.suggestions.isNotEmpty())
        }
    }

    @Test
    fun testMissingTokenError() {
        val code = """
            var x = 5
            if (x > 3 {
                print("x is greater than 3")
            }
        """.trimIndent()

        val parser = FlowLangParser()

        assertThrows(EnhancedSyntaxException::class.java) {
            parser.parse(code)
        }

        try {
            parser.parse(code)
        } catch (e: EnhancedSyntaxException) {
            val error = e.toFlowLangError()
            assertEquals(ErrorType.UNEXPECTED_TOKEN, error.type)
            assertTrue(error.message.contains("Expected"))
            assertTrue(error.suggestions.isNotEmpty())
        }
    }

    @Test
    fun testLexicalError() {
        val code = """
            var x = 5
            var y = x @ 2
            print(y)
        """.trimIndent()

        val parser = FlowLangParser()

        assertThrows(EnhancedSyntaxException::class.java) {
            parser.parse(code)
        }

        try {
            parser.parse(code)
        } catch (e: EnhancedSyntaxException) {
            val error = e.toFlowLangError()
            assertEquals(ErrorType.LEXICAL_ERROR, error.type)
            assertTrue(error.message.contains("Unexpected character"))
            assertTrue(error.suggestions.isNotEmpty())
        }
    }

    @Test
    fun testErrorReportingWithValidCode() {
        val code = """
            var x = 5
            var y = x + 3
            print(y)
        """.trimIndent()

        val result = engine.executeWithErrorReporting(code)

        assertTrue(result.first == null)
        assertTrue(result.second.isEmpty())
    }

    @Test
    fun testMultipleErrors() {
        val code = """
            var x = 5
            if x > 3 {  // Missing parenthesis
                print("x is greater than 3"
            }  // Missing closing brace
        """.trimIndent()

        val parser = FlowLangParser()

        assertThrows(EnhancedSyntaxException::class.java) {
            parser.parse(code)
        }
    }

    @Test
    fun testErrorSuggestions() {
        val code = """
            var message = "Hello World
        """.trimIndent()

        val parser = FlowLangParser()

        try {
            parser.parse(code)
        } catch (e: EnhancedSyntaxException) {
            val error = e.toFlowLangError()
            val suggestions = error.suggestions

            assertTrue(suggestions.isNotEmpty())
            assertTrue(suggestions.any { it.contains("closing quote") })
            assertTrue(suggestions.any { it.contains("escaped quotes") })
        }
    }

    @Test
    fun testErrorLocationTracking() {
        val code = """
            var x = 5
            var y = 10
            if (x > y) {
                print("x is greater")
            } else {
                print("y is greater"
            }
        """.trimIndent()

        val parser = FlowLangParser()

        try {
            parser.parse(code)
        } catch (e: EnhancedSyntaxException) {
            val error = e.toFlowLangError()
            assertTrue(error.line > 0)
            assertTrue(error.column > 0)
            assertNotNull(error.sourceLine)
        }
    }

    @Test
    fun testRuntimeErrorHandling() {
        val code = """
            var x = 5
            var y = 0
            var result = x / y
            print(result)
        """.trimIndent()


        val parser = FlowLangParser()
        val script = parser.parse(code)


        val executor = com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangExecutor()
        val result = executor.executeScript(script, FlowLangContext())


        assertTrue(result == null)
    }

    @Test
    fun testErrorSummary() {
        val code = """
            var x = 5
            if x > 3 {  // Missing parenthesis
                print("x is greater than 3"
            }
        """.trimIndent()

        val parser = FlowLangParser()

        try {
            parser.parse(code)
        } catch (e: EnhancedSyntaxException) {

            assertTrue(parser.hasErrors())
            val summary = parser.getErrorSummary()
            assertTrue(summary.contains("Errors"))
        }
    }

    @Test
    fun testFunctionCallError() {
        val code = """
            var result = unknownFunction(5, 10)
            print(result)
        """.trimIndent()

        val parser = FlowLangParser()
        val script = parser.parse(code)


        assertThrows(RuntimeException::class.java) {
            engine.execute(script.toString())
        }
    }

    @Test
    fun testClassDefinitionError() {
        val code = """
            class MyClass {
                var property = 5
                function method() {
                    return this.property
                }
            }
            
            var obj = new MyClass()
            print(obj.method())
        """.trimIndent()


        val result = engine.execute(code)

        assertTrue(result == null)
    }

    @Test
    fun testComplexErrorScenario() {
        val code = """
            function calculate(x, y) {
                if (x > 0) {
                    return x + y
                } else {
                    return x - y
                }
            }
            
            var result = calculate(10, 5)
            print(result)
        """.trimIndent()


        val result = engine.execute(code)

        assertTrue(result == null)
    }
}
