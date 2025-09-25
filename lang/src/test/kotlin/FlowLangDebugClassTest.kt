package com.thedevjade.io.flowlang

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangEngine
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlowLangDebugClassTest {
    private lateinit var engine: FlowLangEngine

    @BeforeEach
    fun setup() {
        val field = FlowLangEngine::class.java.getDeclaredField("instance")
        field.isAccessible = true
        field.set(null, null)
        engine = FlowLangEngine.getInstance()
    }

    @Test
    fun testClassDefinitionParsing() {

        val code = """
            class MyClass {
                var property = 5
                function method() {
                    return this.property
                }
            }
            
            var obj = new MyClass()
            print("obj created: " + obj)
            print(obj.method())
        """.trimIndent()

        try {
            val result = engine.execute(code)
            println("Result: $result")

        } catch (e: Exception) {
            println("Error: ${e.message}")
            println("Stack trace: ${e.stackTrace.take(5).joinToString("\n")}")
            throw e
        }
    }
}
