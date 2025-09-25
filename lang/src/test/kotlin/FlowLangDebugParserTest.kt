package com.thedevjade.io.flowlang

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.FlowLang
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangEngine
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.parsing.FlowLangLexer
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.parsing.FlowLangParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlowLangDebugParserTest {
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
    fun testLexerWithDefaultParams() {
        val lexer = FlowLangLexer()
        val tokens = lexer.tokenize("function test(a, b = 10) { return a + b }")

        println("Tokens:")
        tokens.forEach { token ->
            println("  ${token.type}: '${token.value}'")
        }


        val equalsToken = tokens.find { it.value == "=" }
        assertNotNull(equalsToken, "= operator should be recognized")
        assertEquals("OPERATOR", equalsToken!!.type.toString())
    }

    @Test
    fun testParserWithDefaultParams() {
        try {
            val parser = FlowLangParser()
            val ast = parser.parse("function test(a, b = 10) { return a + b }")

            println("AST parsed successfully")
            assertNotNull(ast)
        } catch (e: Exception) {
            println("Parser error: ${e.message}")
            e.printStackTrace()
            fail("Parser failed: ${e.message}")
        }
    }
}
