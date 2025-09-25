package com.thedevjade.io.flowlang

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.FlowLang
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangEngine
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlowLangFunctionExecutionTest {
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
    fun testFunctionExecutionStepByStep() {
        val ctx = FlowLangContext()

        try {

            engine.execute(
                """
                function test(a, b = 5) {
                    return a + b
                }
            """.trimIndent(), ctx
            )


            val function = engine.getFunction("test")
            assertNotNull(function, "Function should be registered")


            val result1 = function!!.invoke(arrayOf(2.0, 3.0))
            println("Function with both parameters: $result1")
            assertEquals(5.0, result1)


            val result2 = function.invoke(arrayOf(2.0))
            println("Function with one parameter: $result2")
            assertEquals(7.0, result2)

        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
            fail("Function execution failed: ${e.message}")
        }
    }
}
