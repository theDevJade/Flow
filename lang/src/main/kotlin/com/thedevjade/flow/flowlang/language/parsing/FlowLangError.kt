package com.thedevjade.flow.flowlang.language.parsing

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.parsing.Token
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.parsing.TokenType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FlowLangError(
    val type: ErrorType,
    val message: String,
    val token: Token?,
    val line: Int,
    val column: Int,
    val sourceLine: String? = null,
    val suggestions: List<String> = emptyList(),
    val context: String? = null
) {
    override fun toString(): String {
        val sb = StringBuilder()
        sb.appendLine("${type.name}: $message")
        sb.appendLine("Location: $line:$column")

        if (sourceLine != null) {
            sb.appendLine("Code: $sourceLine")
            if (column > 0) {
                sb.appendLine("     ${" ".repeat(column - 1)}^")
            }
        }

        if (context != null) {
            sb.appendLine("Context: $context")
        }

        if (suggestions.isNotEmpty()) {
            sb.appendLine("Suggestions:")
            suggestions.forEach { suggestion ->
                sb.appendLine("  • $suggestion")
            }
        }

        return sb.toString()
    }


    fun toJson(): String {
        return Json.encodeToString(this)
    }


    fun toJsonPretty(): String {
        return Json { prettyPrint = true }.encodeToString(this)
    }

    companion object {

        fun fromJson(json: String): FlowLangError {
            return Json.decodeFromString(json)
        }


        fun toJson(errors: List<FlowLangError>): String {
            return Json.encodeToString(errors)
        }


        fun toJsonPretty(errors: List<FlowLangError>): String {
            return Json { prettyPrint = true }.encodeToString(errors)
        }


        fun fromJsonList(json: String): List<FlowLangError> {
            return Json.decodeFromString(json)
        }
    }
}


@Serializable
enum class ErrorType {
    SYNTAX_ERROR,
    LEXICAL_ERROR,
    RUNTIME_ERROR,
    TYPE_ERROR,
    UNDEFINED_VARIABLE,
    UNDEFINED_FUNCTION,
    UNDEFINED_CLASS,
    ARGUMENT_MISMATCH,
    UNTERMINATED_STRING,
    UNEXPECTED_TOKEN,
    MISSING_TOKEN,
    INVALID_OPERATION,
    DIVISION_BY_ZERO,
    INDEX_OUT_OF_BOUNDS,
    NULL_REFERENCE,
    INVALID_CAST,
    RECURSION_LIMIT_EXCEEDED,
    MEMORY_LIMIT_EXCEEDED
}


class ErrorCollector {
    private val errors = mutableListOf<FlowLangError>()
    private val warnings = mutableListOf<FlowLangError>()

    fun addError(error: FlowLangError) {
        errors.add(error)
    }

    fun addWarning(warning: FlowLangError) {
        warnings.add(warning)
    }

    fun hasErrors(): Boolean = errors.isNotEmpty()

    fun hasWarnings(): Boolean = warnings.isNotEmpty()

    fun getErrors(): List<FlowLangError> = errors.toList()

    fun getWarnings(): List<FlowLangError> = warnings.toList()

    fun getAllIssues(): List<FlowLangError> = (errors + warnings).sortedWith(
        compareBy<FlowLangError> { it.line }.thenBy { it.column }
    )

    fun clear() {
        errors.clear()
        warnings.clear()
    }

    fun getErrorSummary(): String {
        if (errors.isEmpty() && warnings.isEmpty()) {
            return "No errors or warnings found."
        }

        val sb = StringBuilder()
        if (errors.isNotEmpty()) {
            sb.appendLine("Errors (${errors.size}):")
            errors.forEach { error ->
                sb.appendLine("  ${error.line}:${error.column} - ${error.message}")
            }
        }

        if (warnings.isNotEmpty()) {
            sb.appendLine("Warnings (${warnings.size}):")
            warnings.forEach { warning ->
                sb.appendLine("  ${warning.line}:${warning.column} - ${warning.message}")
            }
        }

        return sb.toString()
    }
}


class ErrorSuggestionEngine {

    fun getSuggestions(error: FlowLangError, source: String): List<String> {
        val suggestions = mutableListOf<String>()

        when (error.type) {
            ErrorType.UNTERMINATED_STRING -> {
                suggestions.add("Add a closing quote (\") to terminate the string")
                suggestions.add("Check for escaped quotes within the string")
            }

            ErrorType.UNEXPECTED_TOKEN -> {
                val token = error.token
                if (token != null) {
                    when (token.type) {
                        TokenType.RIGHT_PAREN -> {
                            suggestions.add("Check for missing opening parenthesis (")
                            suggestions.add("Ensure all function calls have matching parentheses")
                        }

                        TokenType.RIGHT_BRACE -> {
                            suggestions.add("Check for missing opening brace {")
                            suggestions.add("Ensure all code blocks have matching braces")
                        }

                        TokenType.RIGHT_BRACKET -> {
                            suggestions.add("Check for missing opening bracket [")
                            suggestions.add("Ensure all array/list access has matching brackets")
                        }

                        TokenType.END_OF_FILE -> {
                            suggestions.add("Check for missing closing parenthesis, brace, or bracket")
                            suggestions.add("Ensure all statements are properly terminated")
                        }

                        else -> {
                            suggestions.add("Check the syntax around this token")
                            suggestions.add("Verify that all previous statements are complete")
                        }
                    }
                }
            }

            ErrorType.MISSING_TOKEN -> {
                suggestions.add("Add the missing token")
                suggestions.add("Check if you're in the middle of a statement")
            }

            ErrorType.UNDEFINED_VARIABLE -> {
                val variableName = extractVariableName(error.message)
                if (variableName != null) {
                    suggestions.add("Declare the variable with 'var $variableName'")
                    suggestions.add("Check for typos in the variable name")
                    suggestions.add("Ensure the variable is in scope")
                }
            }

            ErrorType.UNDEFINED_FUNCTION -> {
                val functionName = extractFunctionName(error.message)
                if (functionName != null) {
                    suggestions.add("Define the function with 'function $functionName(...)'")
                    suggestions.add("Check for typos in the function name")
                    suggestions.add("Verify the function is registered in the engine")
                }
            }

            ErrorType.UNDEFINED_CLASS -> {
                val className = extractClassName(error.message)
                if (className != null) {
                    suggestions.add("Define the class with 'class $className'")
                    suggestions.add("Check for typos in the class name")
                    suggestions.add("Verify the class is registered in the engine")
                }
            }

            ErrorType.ARGUMENT_MISMATCH -> {
                suggestions.add("Check the number of arguments passed to the function")
                suggestions.add("Verify argument types match the function signature")
                suggestions.add("Use default parameters if available")
            }

            ErrorType.TYPE_ERROR -> {
                suggestions.add("Check the types of variables and expressions")
                suggestions.add("Use type annotations to specify expected types")
                suggestions.add("Verify type compatibility in operations")
            }

            ErrorType.DIVISION_BY_ZERO -> {
                suggestions.add("Add a check to ensure the divisor is not zero")
                suggestions.add("Use conditional logic: if (divisor != 0) { ... }")
            }

            ErrorType.NULL_REFERENCE -> {
                suggestions.add("Check if the variable is initialized before use")
                suggestions.add("Add null checks: if (variable != null) { ... }")
            }

            else -> {
                suggestions.add("Review the code around the error location")
                suggestions.add("Check the FlowLang documentation for syntax rules")
            }
        }

        return suggestions
    }

    private fun extractVariableName(message: String): String? {
        val regex = "Variable '([^']+)' not found".toRegex()
        return regex.find(message)?.groupValues?.get(1)
    }

    private fun extractFunctionName(message: String): String? {
        val regex = "Function '([^']+)' not found".toRegex()
        return regex.find(message)?.groupValues?.get(1)
    }

    private fun extractClassName(message: String): String? {
        val regex = "Class '([^']+)' not found".toRegex()
        return regex.find(message)?.groupValues?.get(1)
    }
}


class EnhancedSyntaxException(
    message: String,
    val token: Token?,
    val source: String? = null,
    val suggestions: List<String> = emptyList(),
    val errorType: ErrorType = ErrorType.SYNTAX_ERROR
) : Exception(message) {

    fun toFlowLangError(): FlowLangError {
        return FlowLangError(
            type = errorType,
            message = message ?: "Unknown syntax error",
            token = token,
            line = token?.line ?: 0,
            column = token?.column ?: 0,
            sourceLine = source?.let { getSourceLine(it, token?.line ?: 0) } ?: null,
            suggestions = suggestions
        )
    }

    private fun getSourceLine(source: String?, lineNumber: Int): String? {
        return source?.lines()?.getOrNull(lineNumber - 1)
    }
}
