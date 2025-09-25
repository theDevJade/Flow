package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language

import com.thedevjade.flow.flowlang.language.parsing.ErrorCollector
import com.thedevjade.flow.flowlang.language.parsing.ErrorType
import com.thedevjade.flow.flowlang.language.parsing.FlowLangError
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangContext
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.nodes.BinaryOpNode


class FlowLangExecutor {
    private val errorCollector = ErrorCollector()

    fun executeScript(script: BinaryOpNode.FlowLangScript, context: FlowLangContext): Any? {
        try {
            return script.root.execute(context)
        } catch (e: Exception) {
            val error = createRuntimeError(e, context)
            errorCollector.addError(error)
            throw RuntimeException("Runtime error: ${error.message}\n$error")
        }
    }

    fun executeScriptWithErrorReporting(
        script: BinaryOpNode.FlowLangScript,
        context: FlowLangContext
    ): Pair<Any?, List<FlowLangError>> {
        try {
            val result = script.root.execute(context)
            return Pair(result, errorCollector.getAllIssues())
        } catch (e: Exception) {
            val error = createRuntimeError(e, context)
            errorCollector.addError(error)
            return Pair(null, listOf(error))
        }
    }

    private fun createRuntimeError(exception: Exception, context: FlowLangContext): FlowLangError {
        val message = exception.message ?: "Unknown runtime error"
        val errorType = determineErrorType(exception)

        return FlowLangError(
            type = errorType,
            message = message,
            token = null,
            line = 0,
            column = 0,
            context = "Runtime execution context",
            suggestions = getRuntimeErrorSuggestions(errorType, message)
        )
    }

    private fun determineErrorType(exception: Exception): ErrorType {
        return when {
            exception.message?.contains("division by zero", ignoreCase = true) == true -> ErrorType.DIVISION_BY_ZERO
            exception.message?.contains("null", ignoreCase = true) == true -> ErrorType.NULL_REFERENCE
            exception.message?.contains("index", ignoreCase = true) == true -> ErrorType.INDEX_OUT_OF_BOUNDS
            exception.message?.contains("type", ignoreCase = true) == true -> ErrorType.TYPE_ERROR
            exception.message?.contains("recursion", ignoreCase = true) == true -> ErrorType.RECURSION_LIMIT_EXCEEDED
            exception.message?.contains("memory", ignoreCase = true) == true -> ErrorType.MEMORY_LIMIT_EXCEEDED
            else -> ErrorType.RUNTIME_ERROR
        }
    }

    private fun getRuntimeErrorSuggestions(errorType: ErrorType, message: String): List<String> {
        return when (errorType) {
            ErrorType.DIVISION_BY_ZERO -> listOf(
                "Add a check to ensure the divisor is not zero",
                "Use conditional logic: if (divisor != 0) { ... }",
                "Consider using a default value when division by zero occurs"
            )

            ErrorType.NULL_REFERENCE -> listOf(
                "Check if the variable is initialized before use",
                "Add null checks: if (variable != null) { ... }",
                "Initialize variables with default values"
            )

            ErrorType.INDEX_OUT_OF_BOUNDS -> listOf(
                "Check the array/list size before accessing elements",
                "Use bounds checking: if (index < array.length) { ... }",
                "Verify the index is within valid range"
            )

            ErrorType.TYPE_ERROR -> listOf(
                "Check the types of variables and expressions",
                "Use type annotations to specify expected types",
                "Verify type compatibility in operations"
            )

            ErrorType.RECURSION_LIMIT_EXCEEDED -> listOf(
                "Check for infinite recursion in function calls",
                "Add base cases to recursive functions",
                "Consider using iterative approaches instead"
            )

            ErrorType.MEMORY_LIMIT_EXCEEDED -> listOf(
                "Check for memory leaks in loops",
                "Optimize data structures and algorithms",
                "Consider processing data in smaller chunks"
            )

            else -> listOf(
                "Review the code around the error location",
                "Check variable values and function calls",
                "Verify all dependencies are properly initialized"
            )
        }
    }

    fun getErrors(): List<FlowLangError> = errorCollector.getErrors()

    fun getWarnings(): List<FlowLangError> = errorCollector.getWarnings()

    fun getAllIssues(): List<FlowLangError> = errorCollector.getAllIssues()

    fun hasErrors(): Boolean = errorCollector.hasErrors()

    fun hasWarnings(): Boolean = errorCollector.hasWarnings()

    fun getErrorSummary(): String = errorCollector.getErrorSummary()
}
