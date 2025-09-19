package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.parsing

enum class TokenType {
    IDENTIFIER,
    NUMBER,
    STRING,
    OPERATOR,
    KEYWORD,
    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACE,
    RIGHT_BRACE,
    LEFT_BRACKET,
    RIGHT_BRACKET,
    COMMA,
    COLON,
    END_OF_LINE,
    END_OF_FILE
}

data class Token(
    val type: TokenType,
    val value: String,
    val line: Int,
    val column: Int
) {
    override fun toString(): String = "$type($value) at $line:$column"
}
