package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.parsing

import com.thedevjade.flow.flowlang.language.parsing.EnhancedSyntaxException
import com.thedevjade.flow.flowlang.language.parsing.ErrorCollector
import com.thedevjade.flow.flowlang.language.parsing.ErrorType
import com.thedevjade.flow.flowlang.language.parsing.FlowLangError

class FlowLangLexer {
    private var source: String = ""
    private var position: Int = 0
    private var line: Int = 1
    private var column: Int = 1
    private var errorCollector: ErrorCollector? = null

    private val keywords = setOf(
        "if", "else", "while", "for", "function", "return",
        "true", "false", "null", "and", "or", "not",
        "var", "event", "on", "trigger", "class", "extends",
        "new", "this", "super", "public", "private", "protected",
        "String", "Number", "Boolean", "Object", "List", "Array"
    )

    private val current: Char get() = if (position < source.length) source[position] else '\u0000'

    fun tokenize(source: String, errorCollector: ErrorCollector? = null): List<Token> {
        this.source = source
        this.errorCollector = errorCollector
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
                    (current.isLetterOrDigit() || current == '_')
                ) {
                    position++
                    column++
                }

                val value = source.substring(start, position)
                val type = if (keywords.contains(value)) {
                    if (value in setOf("String", "Number", "Boolean", "Object", "List", "Array")) {
                        TokenType.IDENTIFIER
                    } else {
                        TokenType.KEYWORD
                    }
                } else {
                    TokenType.IDENTIFIER
                }
                tokens.add(Token(type, value, line, col))
                continue
            }

            if (current.isDigit()) {
                val start = position
                val col = column
                var hasDot = false
                while (position < source.length &&
                    (current.isDigit() || (current == '.' && !hasDot))
                ) {
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
                    val error = FlowLangError(
                        type = ErrorType.UNTERMINATED_STRING,
                        message = "Unterminated string literal",
                        token = null,
                        line = line,
                        column = col,
                        sourceLine = getCurrentLine(),
                        suggestions = listOf(
                            "Add a closing quote (\") to terminate the string",
                            "Check for escaped quotes within the string"
                        )
                    )
                    errorCollector?.addError(error)
                    throw EnhancedSyntaxException(
                        "Unterminated string at $line:$col",
                        null,
                        source,
                        error.suggestions,
                        ErrorType.UNTERMINATED_STRING
                    )
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
                    if ("+-*/=<>!&|%^.".contains(current)) {
                        val start = position
                        val col = column
                        while (position < source.length && "+-*/=<>!&|%^.".contains(current)) {
                            position++
                            column++
                        }

                        val op = source.substring(start, position)
                        tokens.add(Token(TokenType.OPERATOR, op, line, col))
                        continue
                    } else {
                        val error = FlowLangError(
                            type = ErrorType.LEXICAL_ERROR,
                            message = "Unexpected character '$current'",
                            token = null,
                            line = line,
                            column = column,
                            sourceLine = getCurrentLine(),
                            suggestions = listOf(
                                "Check for typos or invalid characters",
                                "Ensure proper syntax around this character",
                                "Verify the character is supported in FlowLang"
                            )
                        )
                        errorCollector?.addError(error)
                        throw EnhancedSyntaxException(
                            "Unexpected character '$current' at $line:$column",
                            null,
                            source,
                            error.suggestions,
                            ErrorType.LEXICAL_ERROR
                        )
                    }
                }
            }

            position++
            column++
        }

        tokens.add(Token(TokenType.END_OF_FILE, "", line, column))
        return tokens
    }

    private fun getCurrentLine(): String? {
        return source.lines().getOrNull(line - 1)
    }
}
