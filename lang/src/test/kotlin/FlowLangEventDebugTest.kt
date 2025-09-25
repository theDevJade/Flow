package com.thedevjade.io.flowlang

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.FlowLang
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangEngine
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangContext
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangEvent
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangParameter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlowLangEventDebugTest {
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
    fun testEventHandlerRegistration() {
        val ctx = FlowLangContext()

        try {

            val event = FlowLangEvent(
                "testEvent",
                arrayOf(FlowLangParameter("message", "text")),
                "Test event"
            )
            engine.registerEvent(event)


            engine.execute(
                """
                on testEvent {
                    print("Event triggered: " + message)
                }
            """.trimIndent(), ctx
            )


            val registeredEvent = engine.getEvent("testEvent")
            assertNotNull(registeredEvent, "Event should be registered")


            val output = captureOutput {
                engine.triggerEvent("testEvent", "Hello World")
            }

            println("Captured output: '$output'")
            assertTrue(output.contains("Event triggered: Hello World"), "Event handler should have been executed")

        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
            fail("Event handler test failed: ${e.message}")
        }
    }

    private fun captureOutput(block: () -> Unit): String {
        val output = java.io.ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(java.io.PrintStream(output))

        try {
            block()
        } finally {
            System.setOut(originalOut)
        }

        return output.toString()
    }
}
