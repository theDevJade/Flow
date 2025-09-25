package com.thedevjade.io.flowlang

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.FlowLang
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangEngine
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlowLangFunctionRegistrationTest {
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
    fun testFunctionRegistration() {
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


            assertEquals(2, function!!.parameters.size, "Function should have 2 parameters")


            assertTrue(function.parameters[1].isOptional, "Second parameter should be optional")

            println("Function registered successfully with ${function.parameters.size} parameters")
            function.parameters.forEachIndexed { index, param ->
                println("  Parameter $index: ${param.name} (${param.typeName}) - Optional: ${param.isOptional}")
            }

        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
            fail("Function registration failed: ${e.message}")
        }
    }
}
