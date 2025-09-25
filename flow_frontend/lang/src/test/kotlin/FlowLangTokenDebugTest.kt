package com.thedevjade.io.flowlang

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.parsing.FlowLangLexer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlowLangTokenDebugTest {
    @Test
    fun testDotTokenization() {
        val code = "obj.method()"
        val lexer = FlowLangLexer()
        val tokens = lexer.tokenize(code)

        println("Tokens for '$code':")
        tokens.forEach { token ->
            println("  ${token.type}: '${token.value}'")
        }

        // Should have: IDENTIFIER(obj), OPERATOR(.), IDENTIFIER(method), LEFT_PAREN, RIGHT_PAREN, END_OF_FILE
        assertEquals(6, tokens.size)
        assertEquals("obj", tokens[0].value)
        assertEquals(".", tokens[1].value)
        assertEquals("method", tokens[2].value)
        assertEquals("(", tokens[3].value)
        assertEquals(")", tokens[4].value)
    }
}
