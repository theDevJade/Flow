package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.parsing

class FlowLangLexer {
    private var source: String = ""
    private var position: Int = 0
    private var line: Int = 1
    private var column: Int = 1

    private val keywords = setOf(
        "if", "else", "while", "for", "function", "return",
        "true", "false", "null", "and", "or", "not",
        "var", "event", "on", "trigger"
    )

    private val current: Char get() = if (position < source.length) source[position] else '\u0000'

    fun tokenize(source: String): List<Token> {
        this.source = source
        position = 0
        line = 1
        column = 1
        val tokens = mutableListOf<Token>()

        while (position < source.length) {
            if (current.isWhitespace()) {
                if (current == '\n') {
                    tokens.add(Token(TokenType.END_OF_LINE, "\n", line, column))
                    line++
                    column = 0
                }
                position++
                column++
                continue
            }

            if (current == '#') {
                while (position < source.length && current != '\n') {
                    position++
                    column++
                }
                continue
            }

            if (current.isLetter() || current == '_') {
                val start = position
                val col = column
                while (position < source.length &&
                    (current.isLetterOrDigit() || current == '_' || current == '.')) {
                    position++
                    column++
                }

                val value = source.substring(start, position)
                val type = if (keywords.contains(value)) TokenType.KEYWORD else TokenType.IDENTIFIER
                tokens.add(Token(type, value, line, col))
                continue
            }

            if (current.isDigit()) {
                val start = position
                val col = column
                var hasDot = false
                while (position < source.length &&
                    (current.isDigit() || (current == '.' && !hasDot))) {
                    if (current == '.') hasDot = true
                    position++
                    column++
                }

                val value = source.substring(start, position)
                tokens.add(Token(TokenType.NUMBER, value, line, col))
                continue
            }

            if (current == '"' || current == '\'') {
                val quote = current
                val start = position
                val col = column
                position++
                column++
                while (position < source.length && current != quote) {
                    if (current == '\\' && position + 1 < source.length) {
                        position += 2
                        column += 2
                    } else {
                        position++
                        column++
                    }
                }

                if (position >= source.length) {
                    throw Exception("Unterminated string at $line:$col")
                }
                position++
                column++
                val raw = source.substring(start, position)
                tokens.add(Token(TokenType.STRING, raw, line, col))
                continue
            }

            when (current) {
                '(' -> tokens.add(Token(TokenType.LEFT_PAREN, "(", line, column))
                ')' -> tokens.add(Token(TokenType.RIGHT_PAREN, ")", line, column))
                '{' -> tokens.add(Token(TokenType.LEFT_BRACE, "{", line, column))
                '}' -> tokens.add(Token(TokenType.RIGHT_BRACE, "}", line, column))
                '[' -> tokens.add(Token(TokenType.LEFT_BRACKET, "[", line, column))
                ']' -> tokens.add(Token(TokenType.RIGHT_BRACKET, "]", line, column))
                ',' -> tokens.add(Token(TokenType.COMMA, ",", line, column))
                ':' -> tokens.add(Token(TokenType.COLON, ":", line, column))
                ';' -> tokens.add(Token(TokenType.END_OF_LINE, ";", line, column))
                else -> {
                    if ("+-*/=<>!&|%^".contains(current)) {
                        val start = position
                        val col = column
                        while (position < source.length && "+-*/=<>!&|%^".contains(current)) {
                            position++
                            column++
                        }

                        val op = source.substring(start, position)
                        tokens.add(Token(TokenType.OPERATOR, op, line, col))
                        continue
                    } else {
                        throw Exception("Unexpected character '$current' at $line:$column")
                    }
                }
            }

            position++
            column++
        }

        tokens.add(Token(TokenType.END_OF_FILE, "", line, column))
        return tokens
    }
}
