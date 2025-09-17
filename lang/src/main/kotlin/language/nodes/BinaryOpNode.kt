package com.thedevjade.io.flowlang.language.nodes

import com.thedevjade.io.flowlang.language.FlowLangEngine
import com.thedevjade.io.flowlang.language.memory.FlowLangContext
import com.thedevjade.io.flowlang.language.memory.FlowLangEvent
import com.thedevjade.io.flowlang.language.memory.FlowLangFunction
import com.thedevjade.io.flowlang.language.memory.FlowLangParameter
import com.thedevjade.io.flowlang.language.misc.ReturnException

class BinaryOpNode(
    val left: FlowLangNode,
    val operator: String,
    val right: FlowLangNode
) : FlowLangNode() {
    override fun execute(context: FlowLangContext): Any? {
        val leftVal = left.execute(context)

        when (operator) {
            "and" -> {
                if (!leftVal.toBoolean()) return false
                return right.execute(context).toBoolean()
            }
            "or" -> {
                if (leftVal.toBoolean()) return true
                return right.execute(context).toBoolean()
            }
        }

        val rightVal = right.execute(context)

        return when (operator) {
            "+" -> {
                if (leftVal is String || rightVal is String) {
                    val leftStr = if (leftVal is Double && leftVal == leftVal.toInt().toDouble()) {
                        leftVal.toInt().toString()
                    } else {
                        leftVal?.toString() ?: "null"
                    }
                    val rightStr = if (rightVal is Double && rightVal == rightVal.toInt().toDouble()) {
                        rightVal.toInt().toString()
                    } else {
                        rightVal?.toString() ?: "null"
                    }
                    leftStr + rightStr
                } else {
                    leftVal.toDouble() + rightVal.toDouble()
                }
            }
            "-" -> leftVal.toDouble() - rightVal.toDouble()
            "*" -> leftVal.toDouble() * rightVal.toDouble()
            "/" -> leftVal.toDouble() / rightVal.toDouble()
            "%" -> leftVal.toDouble() % rightVal.toDouble()
            "==" -> leftVal == rightVal
            "!=" -> leftVal != rightVal
            "<" -> {
                if (leftVal is Comparable<*> && rightVal is Comparable<*>) {
                    @Suppress("UNCHECKED_CAST")
                    (leftVal as Comparable<Any>).compareTo(rightVal as Any) < 0
                } else {
                    throw Exception("Cannot compare $leftVal and $rightVal")
                }
            }
            ">" -> {
                if (leftVal is Comparable<*> && rightVal is Comparable<*>) {
                    @Suppress("UNCHECKED_CAST")
                    (leftVal as Comparable<Any>).compareTo(rightVal as Any) > 0
                } else {
                    throw Exception("Cannot compare $leftVal and $rightVal")
                }
            }
            "<=" -> {
                if (leftVal is Comparable<*> && rightVal is Comparable<*>) {
                    @Suppress("UNCHECKED_CAST")
                    (leftVal as Comparable<Any>).compareTo(rightVal as Any) <= 0
                } else {
                    throw Exception("Cannot compare $leftVal and $rightVal")
                }
            }
            ">=" -> {
                if (leftVal is Comparable<*> && rightVal is Comparable<*>) {
                    @Suppress("UNCHECKED_CAST")
                    (leftVal as Comparable<Any>).compareTo(rightVal as Any) >= 0
                } else {
                    throw Exception("Cannot compare $leftVal and $rightVal")
                }
            }
            else -> throw Exception("Unknown operator: $operator")
        }
    }

    class UnaryOpNode(
        val operator: String,
        val operand: FlowLangNode
    ) : FlowLangNode() {
        override fun execute(context: FlowLangContext): Any? {
            val value = operand.execute(context)
            return when (operator) {
                "-" -> -value.toDouble()
                "!", "not" -> !value.toBoolean()
                else -> throw Exception("Unknown unary operator: $operator")
            }
        }
    }

    class BlockNode(val statements: List<FlowLangNode>) : FlowLangNode() {
        override fun execute(context: FlowLangContext): Any? {
            var result: Any? = null
            try {
                for (stmt in statements) {
                    result = stmt.execute(context)
                }
            } catch (ret: ReturnException) {
                return ret.value
            }
            return result
        }
    }

    class IfNode(
        val condition: FlowLangNode,
        val thenBranch: FlowLangNode,
        val elseBranch: FlowLangNode? = null
    ) : FlowLangNode() {
        override fun execute(context: FlowLangContext): Any? {
            val cond = condition.execute(context).toBoolean()
            return if (cond) {
                thenBranch.execute(context)
            } else {
                elseBranch?.execute(context)
            }
        }
    }

    class WhileNode(
        val condition: FlowLangNode,
        val body: FlowLangNode
    ) : FlowLangNode() {
        override fun execute(context: FlowLangContext): Any? {
            var result: Any? = null
            while (condition.execute(context).toBoolean()) {
                result = body.execute(context)
            }
            return result
        }
    }

    class ForNode(
        val initialization: FlowLangNode,
        val condition: FlowLangNode,
        val increment: FlowLangNode,
        val body: FlowLangNode
    ) : FlowLangNode() {
        override fun execute(context: FlowLangContext): Any? {
            var result: Any? = null
            initialization.execute(context)
            while (condition.execute(context).toBoolean()) {
                result = body.execute(context)
                increment.execute(context)
            }
            return result
        }
    }

    class FunctionDefNode(
        val name: String,
        val parameters: List<String>,
        val body: FlowLangNode
    ) : FlowLangNode() {
        override fun execute(context: FlowLangContext): Any? {
            val sparams = parameters.map { FlowLangParameter(it, "object") }.toTypedArray()
            val impl: (Array<Any?>) -> Any? = { args ->
                val fnCtx = context.createChildContext()
                for (i in parameters.indices) {
                    fnCtx.setVariable(parameters[i], if (i < args.size) args[i] else null)
                }
                body.execute(fnCtx)
            }
            FlowLangEngine.getInstance().registerFunction(FlowLangFunction(name, impl, sparams))
            return null
        }
    }

    class ReturnNode(val value: FlowLangNode) : FlowLangNode() {
        override fun execute(context: FlowLangContext): Any? {
            val result = value.execute(context)
            throw ReturnException(result)
        }
    }

    class EventHandlerNode(
        val eventName: String,
        val body: FlowLangNode
    ) : FlowLangNode() {
        override fun execute(context: FlowLangContext): Any? {
            val event = FlowLangEngine.getInstance().getEvent(eventName)
                ?: throw Exception("Event '$eventName' not found")

            val script = FlowLangScript(body)
            event.registerHandler(script, context)
            return null
        }
    }

    class FlowLangScript(val root: FlowLangNode)
}

// Extension functions for type conversion
private fun Any?.toBoolean(): Boolean = when (this) {
    is Boolean -> this
    is Number -> this.toDouble() != 0.0
    is String -> this.isNotEmpty()
    null -> false
    else -> true
}

private fun Any?.toDouble(): Double = when (this) {
    is Number -> this.toDouble()
    is String -> this.toDoubleOrNull() ?: 0.0
    is Boolean -> if (this) 1.0 else 0.0
    null -> 0.0
    else -> 0.0
}
