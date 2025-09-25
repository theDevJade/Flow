package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.parsing

import com.thedevjade.flow.flowlang.language.parsing.*
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.nodes.*
import java.util.regex.Pattern


class FlowLangParser {
    private var tokens: List<Token> = emptyList()
    private var position: Int = 0
    private var source: String = ""
    private var errorCollector: ErrorCollector = ErrorCollector()
    private var suggestionEngine: ErrorSuggestionEngine = ErrorSuggestionEngine()

    private val current: Token? get() = if (position < tokens.size) tokens[position] else null

    fun parse(sourceUnprocessed: String): BinaryOpNode.FlowLangScript {
        this.source = Preprocessor().process(sourceUnprocessed)
        val lexer = FlowLangLexer()
        tokens = lexer.tokenize(this.source, errorCollector)
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
                "class" -> parseClassDefinition()
                "new" -> parseNewExpression()
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
        val parms = mutableListOf<Pair<String, String?>>()
        val defaultValues = mutableMapOf<String, FlowLangNode>()

        if (current?.type != TokenType.RIGHT_PAREN) {
            do {
                val paramName = expect(TokenType.IDENTIFIER).value
                var paramType: String? = null


                if (current?.type == TokenType.COLON) {
                    position++
                    paramType = expect(TokenType.IDENTIFIER).value
                }


                var defaultValue: FlowLangNode? = null
                if (current?.type == TokenType.OPERATOR && current!!.value == "=") {
                    position++
                    defaultValue = parseExpression()
                }

                parms.add(Pair(paramName, paramType))
                if (defaultValue != null) {
                    defaultValues[paramName] = defaultValue
                }

                if (current?.type == TokenType.COMMA) position++ else break
            } while (true)
        }

        expect(TokenType.RIGHT_PAREN)
        val body = parseStatement()
        return BinaryOpNode.FunctionDefNode(name, parms, body, defaultValues)
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

        var typeName: String? = null
        if (current?.type == TokenType.COLON) {
            position++
            typeName = expect(TokenType.IDENTIFIER).value
        }

        val value = if (current?.type == TokenType.OPERATOR && current!!.value == "=") {
            position++
            parseExpression()
        } else {
            LiteralNode(null)
        }

        return AssignmentNode(name, value, typeName)
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
            } else if (expr is BinaryOpNode.PropertyAccessNode) {
                return BinaryOpNode.PropertyAssignmentNode(expr.objectNode, expr.propertyName, value)
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
            (current!!.value == "==" || current!!.value == "!=")
        ) {
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
                    current!!.value == "<=" || current!!.value == ">=")
        ) {
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
            (current!!.value == "+" || current!!.value == "-")
        ) {
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
            (current!!.value == "*" || current!!.value == "/" || current!!.value == "%")
        ) {
            val op = current!!.value
            position++
            val right = parseUnary()
            expr = BinaryOpNode(expr, op, right)
        }
        return expr
    }

    private fun parseUnary(): FlowLangNode {
        if (current?.type == TokenType.OPERATOR &&
            (current!!.value == "-" || current!!.value == "!")
        ) {
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

                "new" -> parseNewExpression()

                "this" -> {
                    position++
                    val thisNode = BinaryOpNode.ThisNode()


                    if (current?.type == TokenType.OPERATOR && current!!.value == ".") {
                        position++
                        val propertyName = expect(TokenType.IDENTIFIER).value

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
                            return BinaryOpNode.MethodCallNode(thisNode, propertyName, args)
                        } else {

                            return BinaryOpNode.PropertyAccessNode(thisNode, propertyName)
                        }
                    }

                    thisNode
                }

                else -> throw SyntaxException("Unexpected token: ${current?.value}", current!!)
            }
        }

        if (current?.type == TokenType.IDENTIFIER) {
            val name = current!!.value
            position++


            if (current?.type == TokenType.OPERATOR && current!!.value == ".") {
                position++
                val propertyName = expect(TokenType.IDENTIFIER).value

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
                    return BinaryOpNode.MethodCallNode(VariableNode(name), propertyName, args)
                } else {

                    return BinaryOpNode.PropertyAccessNode(VariableNode(name), propertyName)
                }
            }

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

    private fun parseClassDefinition(): FlowLangNode {
        expect(TokenType.KEYWORD, "class")
        val className = expect(TokenType.IDENTIFIER).value

        var superClassName: String? = null
        if (current?.type == TokenType.KEYWORD && current!!.value == "extends") {
            position++
            superClassName = expect(TokenType.IDENTIFIER).value
        }

        expect(TokenType.LEFT_BRACE)

        val properties = mutableListOf<FlowLangNode>()
        val methods = mutableListOf<FlowLangNode>()
        val constructor: FlowLangNode? = null

        while (current?.type == TokenType.END_OF_LINE) position++

        while (current?.type != TokenType.RIGHT_BRACE && current?.type != TokenType.END_OF_FILE) {
            when {
                current?.type == TokenType.KEYWORD && current!!.value == "var" -> {
                    properties.add(parseClassProperty())
                }

                current?.type == TokenType.KEYWORD && current!!.value == "function" -> {
                    methods.add(parseClassMethod())
                }

                current?.type == TokenType.KEYWORD && current!!.value == "constructor" -> {

                    position++
                    throw Exception("Constructor parsing not yet implemented")
                }

                else -> {
                    throw Exception("Unexpected token in class definition: ${current?.value}")
                }
            }
            while (current?.type == TokenType.END_OF_LINE) position++
        }

        expect(TokenType.RIGHT_BRACE)
        return BinaryOpNode.ClassDefNode(className, superClassName, properties, methods, constructor)
    }

    private fun parseClassProperty(): FlowLangNode {
        expect(TokenType.KEYWORD, "var")
        val name = expect(TokenType.IDENTIFIER).value

        var typeName = "object"
        if (current?.type == TokenType.COLON) {
            position++
            typeName = expect(TokenType.IDENTIFIER).value
        }

        val value = if (current?.type == TokenType.OPERATOR && current!!.value == "=") {
            position++
            parseExpression()
        } else {
            LiteralNode(null)
        }

        return BinaryOpNode.ClassPropertyNode(name, typeName, value)
    }

    private fun parseClassMethod(): FlowLangNode {
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
        return BinaryOpNode.ClassMethodNode(name, parms, body)
    }

    private fun parseNewExpression(): FlowLangNode {
        expect(TokenType.KEYWORD, "new")
        val className = expect(TokenType.IDENTIFIER).value
        expect(TokenType.LEFT_PAREN)
        val args = mutableListOf<FlowLangNode>()
        if (current?.type != TokenType.RIGHT_PAREN) {
            do {
                args.add(parseExpression())
                if (current?.type == TokenType.COMMA) position++ else break
            } while (true)
        }
        expect(TokenType.RIGHT_PAREN)
        return BinaryOpNode.NewNode(className, args)
    }

    private fun expect(type: TokenType, value: String? = null): Token {
        if (current == null) {
            val lastToken = if (tokens.isNotEmpty()) tokens.last() else Token(TokenType.END_OF_FILE, "", 0, 0)
            val error = FlowLangError(
                type = ErrorType.MISSING_TOKEN,
                message = "Unexpected end of file",
                token = lastToken,
                line = lastToken.line,
                column = lastToken.column,
                sourceLine = getSourceLine(lastToken.line),
                suggestions = listOf(
                    "Check for missing closing parenthesis, brace, or bracket",
                    "Ensure all statements are properly terminated",
                    "Verify the code is complete"
                )
            )
            errorCollector.addError(error)
            throw EnhancedSyntaxException(
                "Unexpected end of file",
                lastToken,
                source,
                error.suggestions,
                ErrorType.MISSING_TOKEN
            )
        }

        val typeMismatch = current!!.type != type
        val valueMismatch = value != null && current!!.value != value

        if (typeMismatch || valueMismatch) {
            val expected = value ?: type.toString()
            val suggestions = suggestionEngine.getSuggestions(
                FlowLangError(
                    type = ErrorType.UNEXPECTED_TOKEN,
                    message = "Expected $expected, found '${current!!.value}'",
                    token = current!!,
                    line = current!!.line,
                    column = current!!.column,
                    sourceLine = getSourceLine(current!!.line)
                ),
                source
            )

            val error = FlowLangError(
                type = ErrorType.UNEXPECTED_TOKEN,
                message = "Expected $expected, found '${current!!.value}'",
                token = current!!,
                line = current!!.line,
                column = current!!.column,
                sourceLine = getSourceLine(current!!.line),
                suggestions = suggestions
            )
            errorCollector.addError(error)
            throw EnhancedSyntaxException(
                "Expected $expected, found '${current!!.value}'",
                current!!,
                source,
                suggestions,
                ErrorType.UNEXPECTED_TOKEN
            )
        }

        val token = current!!
        position++
        return token
    }

    private fun getSourceLine(lineNumber: Int): String? {
        return source.lines().getOrNull(lineNumber - 1)
    }

    fun getErrors(): List<FlowLangError> = errorCollector.getErrors()

    fun getWarnings(): List<FlowLangError> = errorCollector.getWarnings()

    fun getAllIssues(): List<FlowLangError> = errorCollector.getAllIssues()

    fun hasErrors(): Boolean = errorCollector.hasErrors()

    fun hasWarnings(): Boolean = errorCollector.hasWarnings()

    fun getErrorSummary(): String = errorCollector.getErrorSummary()
}
