package com.thedevjade.io.flowlang

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.FlowLang
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangEngine
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlowLangSimpleTypeTest {
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
    fun testBasicTypeDeclaration() {
        val ctx = FlowLangContext()

        try {
            engine.execute(
                """
                var name: String = "Hello"
            """.trimIndent(), ctx
            )

            assertEquals("Hello", engine.execute("name", ctx))
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
            fail("Parsing failed: ${e.message}")
        }
    }

    @Test
    fun testNumberTypeDeclaration() {
        val ctx = FlowLangContext()

        try {
            engine.execute(
                """
                var age: Number = 25
            """.trimIndent(), ctx
            )

            assertEquals(25.0, engine.execute("age", ctx))
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
            fail("Parsing failed: ${e.message}")
        }
    }

    @Test
    fun testBooleanTypeDeclaration() {
        val ctx = FlowLangContext()

        try {
            engine.execute(
                """
                var flag: Boolean = true
            """.trimIndent(), ctx
            )

            assertEquals(true, engine.execute("flag", ctx))
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
            fail("Parsing failed: ${e.message}")
        }
    }
}
