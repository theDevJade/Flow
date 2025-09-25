package com.thedevjade.io.flowlang

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.FlowLang
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangEngine
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlowLangFunctionOverloadingTest {
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
    fun testBasicFunctionOverloading() {
        val ctx = FlowLangContext()

        try {

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
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
            fail("Function overloading failed: ${e.message}")
        }
    }
}
