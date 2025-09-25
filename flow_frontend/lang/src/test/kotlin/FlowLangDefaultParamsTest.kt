package com.thedevjade.io.flowlang

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.FlowLang
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangEngine
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlowLangDefaultParamsTest {
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
    fun testBasicDefaultParameters() {
        val ctx = FlowLangContext()

        try {

            engine.execute(
                """
                function test(a, b) {
                    return a + b
                }
            """.trimIndent(), ctx
            )

            assertEquals(5.0, engine.execute("test(2, 3)", ctx))
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
            fail("Basic function failed: ${e.message}")
        }
    }

    @Test
    fun testDefaultParameterParsing() {
        val ctx = FlowLangContext()

        try {

            engine.execute(
                """
                function test(a, b = 10) {
                    return a + b
                }
            """.trimIndent(), ctx
            )

            assertEquals(12.0, engine.execute("test(2)", ctx))
            assertEquals(5.0, engine.execute("test(2, 3)", ctx))
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
            fail("Default parameter parsing failed: ${e.message}")
        }
    }
}
