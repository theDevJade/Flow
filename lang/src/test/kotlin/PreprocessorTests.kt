package com.thedevjade.io.flowlang

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.parsing.Preprocessor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.regex.Pattern

class PreprocessorTests {

    /**
     * Collapses every run of whitespace to a single space and trims ends,
     * so tests are agnostic to line‑break style or indentation.
     */
    private fun normalize(s: String): String =
        Pattern.compile("\\s+").matcher(s).replaceAll(" ").trim()

    @Test
    fun testSetPlusRewritesToVarAndPlus() {
        val src = "set total to price plus tax"
        val dest = Preprocessor().process(src)
        val expect = "var total = price + tax"
        assertEquals(normalize(expect), normalize(dest))
    }

    @Test
    fun testMinusTimesDividedModuloAllRewrite() {
        val src = """
            set a to x minus y
            set b to x times y
            set c to x divided by y
            set d to x modulo y
        """.trimIndent()
        val dest = Preprocessor().process(src)
        val expect = """
            var a = x - y
            var b = x * y
            var c = x / y
            var d = x % y
        """.trimIndent()
        assertEquals(normalize(expect), normalize(dest))
    }

    @Test
    fun testComparisonPhrasesRewriteCorrectly() {
        val testCases = mapOf(
            "is greater than" to ">",
            "is less than" to "<",
            "is greater than or equal to" to ">=",
            "is less than or equal to" to "<=",
            "is equal to" to "==",
            "equals" to "==",
            "is not equal to" to "!="
        )

        testCases.forEach { (phrase, symbol) ->
            val src = "if score $phrase 50 then pass = true end if"
            val dest = Preprocessor().process(src)
            assertTrue(dest.contains("score $symbol 50"), "Failed for phrase: $phrase")
        }
    }

    @Test
    fun testNoiseWordsRemoved() {
        val src = "if x > 0 then y = 1 end if"
        val dest = Preprocessor().process(src)

        assertFalse(dest.contains(" then "))
        assertFalse(dest.contains(" end "))
        assertFalse(dest.trimEnd().endsWith(" if"))
    }

    @Test
    fun testStringLiteralIsNotChanged() {
        val src = "print(\"a plus b is\", a plus b)"
        val dest = Preprocessor().process(src)
        assertTrue(dest.contains("\"a plus b is\""))
    }

    @Test
    fun testCommentLineRemains() {
        val src = "# compute modulo\nset n to x modulo 2"
        val dest = Preprocessor().process(src)
        assertTrue(dest.trimStart().startsWith("# compute modulo"))
    }

    @Test
    fun testMultipleSpacesAndNewlinesCollapseToSingleSpaces() {
        val src = "set     x  to   1\n\nset   y  to  2"
        val dest = Preprocessor().process(src)
        val expected = "var x = 1 var y = 2"
        assertEquals(expected, normalize(dest))
    }

    @Test
    fun testCustomPhraseRuleWorks() {
        val pp = Preprocessor()
        pp.registerPhraseReplacement("\\btwice\\s+as\\s+big\\b", "* 2")

        val dest = pp.process("set width to base twice as big")
        assertEquals("var width = base * 2", normalize(dest))
    }

    @Test
    fun testCustomTokenRuleWorks() {
        val pp = Preprocessor(registerDefaults = false)
        pp.registerTokenReplacement("plus", "+")
        val dest = pp.process("a plus b")
        assertEquals("a + b", normalize(dest))
    }

    @Test
    fun testCustomRemovableWorks() {
        val pp = Preprocessor(registerDefaults = true)
        pp.registerRemovableKeyword("please")
        val dest = pp.process("please set x to 1 please")
        assertEquals("var x = 1", normalize(dest))
    }

    @Test
    fun testPhraseMatchingIsCaseInsensitive() {
        val src = "IF total IS GREATER THAN 10 THEN y = 1 END IF"
        val dest = Preprocessor().process(src)
        assertTrue(dest.contains("total > 10"))
    }

    @Test
    fun testMixedExampleProducesExpectedOutput() {
        val src = """
        set n to 0
        while n is less than 3 then
            print("n =", n)
            set n to n plus 1
        end while
        """.trimIndent()

        val dest = Preprocessor().process(src)

        val expect = """
        var n = 0
        while (n < 3)
            print("n =", n)
            var n = n + 1
        """.trimIndent()

        assertEquals(normalize(expect), normalize(dest))
    }
}
