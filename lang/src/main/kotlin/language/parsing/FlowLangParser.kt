package com.thedevjade.io.flowlang.language.parsing

import com.thedevjade.io.flowlang.language.nodes.*
import java.util.regex.Pattern

/**
 * Parser for the FlowLang language
 */
class FlowLangParser {
    private var tokens: List<Token> = emptyList()
    private var position: Int = 0

    private val current: Token? get() = if (position < tokens.size) tokens[position] else null

    fun parse(sourceUnprocessed: String): BinaryOpNode.FlowLangScript {
        val source = Preprocessor().process(sourceUnprocessed)
        val lexer = FlowLangLexer()
        tokens = lexer.tokenize(source)
        position = 0

        val statements = mutableListOf<FlowLangNode>()

        while (current?.type == TokenType.END_OF_LINE) position++

        while (current?.type != TokenType.END_OF_FILE) {
            statements.add(parseStatement())
            while (current?.type == TokenType.END_OF_LINE) position++
        }

        return BinaryOpNode.FlowLangScript(BinaryOpNode.BlockNode(statements))
    }

    private fun parseStatement(): FlowLangNode {
        if (current?.type == TokenType.KEYWORD) {
            return when (current!!.value) {
                "if" -> parseIfStatement()
                "while" -> parseWhileStatement()
                "for" -> parseForStatement()
                "function" -> parseFunctionDefinition()
                "return" -> parseReturnStatement()
                "var" -> parseVariableDeclaration()
                "on" -> parseEventHandler()
                else -> parseExpressionStatement()
            }
        }

        if (current?.type == TokenType.LEFT_BRACE) {
            return parseBlock()
        }

        return parseExpressionStatement()
    }

    private fun parseIfStatement(): FlowLangNode {
        expect(TokenType.KEYWORD, "if")
        expect(TokenType.LEFT_PAREN)
        val cond = parseExpression()
        expect(TokenType.RIGHT_PAREN)
        val thenB = parseStatement()
        var elseB: FlowLangNode? = null
        if (current?.type == TokenType.KEYWORD && current!!.value == "else") {
            position++
            elseB = parseStatement()
        }

        return BinaryOpNode.IfNode(cond, thenB, elseB)
    }

    private fun parseWhileStatement(): FlowLangNode {
        expect(TokenType.KEYWORD, "while")
        expect(TokenType.LEFT_PAREN)
        val cond = parseExpression()
        expect(TokenType.RIGHT_PAREN)
        val body = parseStatement()
        return BinaryOpNode.WhileNode(cond, body)
    }

    private fun parseForStatement(): FlowLangNode {
        expect(TokenType.KEYWORD, "for")
        expect(TokenType.LEFT_PAREN)

        val init = if (current?.type == TokenType.KEYWORD && current!!.value == "var") {
            parseVariableDeclaration().also { expect(TokenType.END_OF_LINE) }
        } else {
            parseExpressionStatement()
        }

        val cond = parseExpression()
        expect(TokenType.END_OF_LINE)
        val incr = parseExpression()
        expect(TokenType.RIGHT_PAREN)
        val body = parseStatement()
        return BinaryOpNode.ForNode(init, cond, incr, body)
    }

    private fun parseFunctionDefinition(): FlowLangNode {
        expect(TokenType.KEYWORD, "function")
        val name = expect(TokenType.IDENTIFIER).value
        expect(TokenType.LEFT_PAREN)
        val parms = mutableListOf<String>()
        if (current?.type != TokenType.RIGHT_PAREN) {
            do {
                parms.add(expect(TokenType.IDENTIFIER).value)
                if (current?.type == TokenType.COMMA) position++ else break
            } while (true)
        }

        expect(TokenType.RIGHT_PAREN)
        val body = parseStatement()
        return BinaryOpNode.FunctionDefNode(name, parms, body)
    }

    private fun parseReturnStatement(): FlowLangNode {
        expect(TokenType.KEYWORD, "return")
        val value = if (current?.type == TokenType.END_OF_LINE) {
            LiteralNode(null)
        } else {
            parseExpression()
        }
        return BinaryOpNode.ReturnNode(value)
    }

    private fun parseVariableDeclaration(): FlowLangNode {
        expect(TokenType.KEYWORD, "var")
        val name = expect(TokenType.IDENTIFIER).value
        val value = if (current?.type == TokenType.OPERATOR && current!!.value == "=") {
            position++
            parseExpression()
        } else {
            LiteralNode(null)
        }

        return AssignmentNode(name, value)
    }

    private fun parseEventHandler(): FlowLangNode {
        expect(TokenType.KEYWORD, "on")
        val ev = expect(TokenType.IDENTIFIER).value
        val body = parseStatement()
        return BinaryOpNode.EventHandlerNode(ev, body)
    }

    private fun parseBlock(): FlowLangNode {
        expect(TokenType.LEFT_BRACE)
        val stmts = mutableListOf<FlowLangNode>()

        while (current?.type == TokenType.END_OF_LINE) position++

        while (current?.type != TokenType.RIGHT_BRACE && current?.type != TokenType.END_OF_FILE) {
            stmts.add(parseStatement())
            while (current?.type == TokenType.END_OF_LINE) position++
        }

        expect(TokenType.RIGHT_BRACE)
        return BinaryOpNode.BlockNode(stmts)
    }

    private fun parseExpressionStatement(): FlowLangNode {
        val expr = parseExpression()
        if (current?.type == TokenType.END_OF_LINE) position++
        return expr
    }

    private fun parseExpression(): FlowLangNode = parseAssignment()

    private fun parseAssignment(): FlowLangNode {
        val expr = parseLogicalOr()
        if (current?.type == TokenType.OPERATOR && current!!.value == "=") {
            position++
            val value = parseAssignment()
            if (expr is VariableNode) {
                return AssignmentNode(expr.name, value)
            }
            throw Exception("Invalid assignment target")
        }
        return expr
    }

    private fun parseLogicalOr(): FlowLangNode {
        var expr = parseLogicalAnd()
        while (current?.type == TokenType.KEYWORD && current!!.value == "or") {
            val op = current!!.value
            position++
            val right = parseLogicalAnd()
            expr = BinaryOpNode(expr, op, right)
        }
        return expr
    }

    private fun parseLogicalAnd(): FlowLangNode {
        var expr = parseEquality()
        while (current?.type == TokenType.KEYWORD && current!!.value == "and") {
            val op = current!!.value
            position++
            val right = parseEquality()
            expr = BinaryOpNode(expr, op, right)
        }
        return expr
    }

    private fun parseEquality(): FlowLangNode {
        var expr = parseComparison()
        while (current?.type == TokenType.OPERATOR &&
            (current!!.value == "==" || current!!.value == "!=")) {
            val op = current!!.value
            position++
            val right = parseComparison()
            expr = BinaryOpNode(expr, op, right)
        }
        return expr
    }

    private fun parseComparison(): FlowLangNode {
        var expr = parseAddition()
        while (current?.type == TokenType.OPERATOR &&
            (current!!.value == "<" || current!!.value == ">" ||
                current!!.value == "<=" || current!!.value == ">=")) {
            val op = current!!.value
            position++
            val right = parseAddition()
            expr = BinaryOpNode(expr, op, right)
        }
        return expr
    }

    private fun parseAddition(): FlowLangNode {
        var expr = parseMultiplication()
        while (current?.type == TokenType.OPERATOR &&
            (current!!.value == "+" || current!!.value == "-")) {
            val op = current!!.value
            position++
            val right = parseMultiplication()
            expr = BinaryOpNode(expr, op, right)
        }
        return expr
    }

    private fun parseMultiplication(): FlowLangNode {
        var expr = parseUnary()
        while (current?.type == TokenType.OPERATOR &&
            (current!!.value == "*" || current!!.value == "/" || current!!.value == "%")) {
            val op = current!!.value
            position++
            val right = parseUnary()
            expr = BinaryOpNode(expr, op, right)
        }
        return expr
    }

    private fun parseUnary(): FlowLangNode {
        if (current?.type == TokenType.OPERATOR &&
            (current!!.value == "-" || current!!.value == "!")) {
            val op = current!!.value
            position++
            val right = parseUnary()
            return BinaryOpNode.UnaryOpNode(op, right)
        }

        if (current?.type == TokenType.KEYWORD && current!!.value == "not") {
            position++
            val right = parseUnary()
            return BinaryOpNode.UnaryOpNode("not", right)
        }

        return parsePrimary()
    }

    private fun parsePrimary(): FlowLangNode {
        if (current?.type == TokenType.NUMBER) {
            val value = current!!.value.toDouble()
            position++
            return LiteralNode(value)
        }

        if (current?.type == TokenType.STRING) {
            val raw = current!!.value
            position++
            val s = raw.substring(1, raw.length - 1)
            val unescaped = Pattern.compile("\\\\(.)").matcher(s).replaceAll { matchResult ->
                when (matchResult.group(1)) {
                    "n" -> "\n"
                    "t" -> "\t"
                    "r" -> "\r"
                    "\\" -> "\\"
                    "\"" -> "\""
                    "'" -> "'"
                    else -> matchResult.group(1)
                }
            }
            return LiteralNode(unescaped)
        }

        if (current?.type == TokenType.KEYWORD) {
            return when (current!!.value) {
                "true" -> {
                    position++
                    LiteralNode(true)
                }
                "false" -> {
                    position++
                    LiteralNode(false)
                }
                "null" -> {
                    position++
                    LiteralNode(null)
                }
                else -> parseExpressionStatement()
            }
        }

        if (current?.type == TokenType.IDENTIFIER) {
            val name = current!!.value
            position++
            if (current?.type == TokenType.LEFT_PAREN) {
                position++
                val args = mutableListOf<FlowLangNode>()
                if (current?.type != TokenType.RIGHT_PAREN) {
                    do {
                        args.add(parseExpression())
                        if (current?.type == TokenType.COMMA) position++ else break
                    } while (true)
                }

                expect(TokenType.RIGHT_PAREN)
                return FunctionCallNode(name, args)
            }

            return VariableNode(name)
        }

        if (current?.type == TokenType.LEFT_PAREN) {
            position++
            val expr = parseExpression()
            expect(TokenType.RIGHT_PAREN)
            return expr
        }

        throw Exception("Unexpected token: $current")
    }

    private fun expect(type: TokenType, value: String? = null): Token {
        if (current == null) {
            val lastToken = if (tokens.isNotEmpty()) tokens.last() else Token(TokenType.END_OF_FILE, "", 0, 0)
            throw SyntaxException("Unexpected end of file", lastToken)
        }

        val typeMismatch = current!!.type != type
        val valueMismatch = value != null && current!!.value != value

        if (typeMismatch || valueMismatch) {
            val expected = value ?: type.toString()
            throw SyntaxException(
                "Expected $expected, found '${current!!.value}'",
                current!!
            )
        }

        val token = current!!
        position++
        return token
    }
}
