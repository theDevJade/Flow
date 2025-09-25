package com.thedevjade.io.flowlang

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.FlowLang
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangEngine
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlowLangSimpleDefaultTest {
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
    fun testSimpleDefaultParameter() {
        val ctx = FlowLangContext()

        try {

            engine.execute(
                """
                function test(a, b = 5) {
                    return a + b
                }
            """.trimIndent(), ctx
            )


            val result1 = engine.execute("test(2, 3)", ctx)
            println("test(2, 3) = $result1")
            assertEquals(5.0, result1)


            val result2 = engine.execute("test(2)", ctx)
            println("test(2) = $result2")
            assertEquals(7.0, result2)

        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
            fail("Default parameter test failed: ${e.message}")
        }
    }
}
