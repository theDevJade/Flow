package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.nodes

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangEngine
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.*
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.misc.ReturnException
import java.util.Locale.getDefault

class BinaryOpNode(
    val left: FlowLangNode,
    val operator: String,
    val right: FlowLangNode
) : FlowLangNode() {
    override fun execute(context: FlowLangContext): Any {
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
        override fun execute(context: FlowLangContext): Any {
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
        val parameters: List<Pair<String, String?>>,
        val body: FlowLangNode,
        val defaultValues: Map<String, FlowLangNode> = emptyMap()
    ) : FlowLangNode() {
        override fun execute(context: FlowLangContext): Any? {
            val sparams = parameters.map { (name, type) ->
                val defaultValue = defaultValues[name]?.execute(context)
                FlowLangParameter.create(name, type ?: "object", defaultValues.containsKey(name), defaultValue)
            }.toTypedArray()

            val impl: (Array<Any?>) -> Any? = { args ->
                val fnCtx = context.createChildContext()


                for ((paramName, defaultValue) in defaultValues) {
                    fnCtx.setVariable(paramName, defaultValue.execute(fnCtx))
                }


                for (i in parameters.indices) {
                    val (paramName, _) = parameters[i]
                    if (i < args.size) {
                        fnCtx.setVariable(paramName, args[i])
                    }
                }

                body.execute(fnCtx)
            }
            FlowLangEngine.getInstance().registerFunction(FlowLangFunction(name, impl, sparams))
            return null
        }
    }

    class ReturnNode(val value: FlowLangNode) : FlowLangNode() {
        override fun execute(context: FlowLangContext): Any {
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

    class ClassDefNode(
        val className: String,
        val superClassName: String?,
        val properties: List<FlowLangNode>,
        val methods: List<FlowLangNode>,
        val constructor: FlowLangNode?
    ) : FlowLangNode() {
        override fun execute(context: FlowLangContext): Any? {

            val flowLangClass = FlowLangClass(className, superClassName)


            properties.forEach { prop ->
                if (prop is ClassPropertyNode) {
                    val property = FlowLangProperty(
                        prop.name,
                        prop.typeName,
                        FlowLangProperty.Visibility.PUBLIC,
                        prop.defaultValue.execute(context)
                    )
                    flowLangClass.addProperty(property)
                }
            }


            methods.forEach { method ->
                if (method is ClassMethodNode) {

                    val flowLangParams = method.parameters.map { paramName ->
                        FlowLangParameter(paramName, "object")
                    }.toTypedArray()


                    val scriptBody = if (method.body is FlowLangScript) {
                        method.body
                    } else {
                        FlowLangScript(method.body)
                    }

                    val flowLangMethod = FlowLangMethod(
                        method.name,
                        flowLangParams,
                        scriptBody
                    )
                    flowLangClass.addMethod(flowLangMethod)
                }
            }


            val engine = FlowLangEngine.getInstance()
            engine.registerClass(flowLangClass)

            context.setVariable("__class_$className", this)
            return null
        }
    }

    class ClassPropertyNode(
        val name: String,
        val typeName: String,
        val defaultValue: FlowLangNode
    ) : FlowLangNode() {
        override fun execute(context: FlowLangContext): Any? {

            return null
        }
    }

    class ClassMethodNode(
        val name: String,
        val parameters: List<String>,
        val body: FlowLangNode
    ) : FlowLangNode() {
        override fun execute(context: FlowLangContext): Any? {

            return null
        }
    }

    class NewNode(
        val className: String,
        val arguments: List<FlowLangNode>
    ) : FlowLangNode() {
        override fun execute(context: FlowLangContext): Any {
            val engine = FlowLangEngine.getInstance()
            val args = arguments.map { it.execute(context) }.toTypedArray()
            return engine.createInstance(className, args) ?: "Instance of $className"
        }
    }

    class MethodCallNode(
        val objectNode: FlowLangNode,
        val methodName: String,
        val arguments: List<FlowLangNode>
    ) : FlowLangNode() {
        override fun execute(context: FlowLangContext): Any? {
            val obj = objectNode.execute(context)
            if (obj is FlowLangInstance) {
                val args = arguments.map { it.execute(context) }.toTypedArray()
                return obj.callMethod(methodName, args)
            }
            throw Exception("Cannot call method '$methodName' on non-object value")
        }
    }

    class PropertyAccessNode(
        val objectNode: FlowLangNode,
        val propertyName: String
    ) : FlowLangNode() {
        override fun execute(context: FlowLangContext): Any? {
            val obj = objectNode.execute(context)
            if (obj is FlowLangInstance) {
                return obj.getProperty(propertyName)
            }


            if (obj != null) {
                val clazz = obj::class.java
                try {
                    val field = clazz.getDeclaredField(propertyName)
                    field.isAccessible = true
                    return field.get(obj)
                } catch (e: Exception) {

                    try {
                        val getterName = "get${
                            propertyName.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(
                                    getDefault()
                                ) else it.toString()
                            }
                        }"
                        val method = clazz.getMethod(getterName)
                        return method.invoke(obj)
                    } catch (e2: Exception) {

                        try {
                            val propertyNameCapitalized =
                                propertyName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }
                            val method = clazz.getMethod("get$propertyNameCapitalized")
                            return method.invoke(obj)
                        } catch (e3: Exception) {

                        }
                    }
                }
            }

            throw Exception("Cannot access property '$propertyName' on non-object value")
        }
    }

    class PropertyAssignmentNode(
        val objectNode: FlowLangNode,
        val propertyName: String,
        val value: FlowLangNode
    ) : FlowLangNode() {
        override fun execute(context: FlowLangContext): Any? {
            val obj = objectNode.execute(context)
            val newValue = value.execute(context)

            if (obj is FlowLangInstance) {
                obj.setProperty(propertyName, newValue)
                return newValue
            }


            if (obj != null) {
                val clazz = obj::class.java
                try {
                    val field = clazz.getDeclaredField(propertyName)
                    field.isAccessible = true
                    field.set(obj, newValue)
                    return newValue
                } catch (e: Exception) {

                    try {
                        val setterName = "set${
                            propertyName.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(
                                    getDefault()
                                ) else it.toString()
                            }
                        }"
                        val method = clazz.getMethod(setterName, newValue!!::class.java)
                        method.invoke(obj, newValue)
                        return newValue
                    } catch (e2: Exception) {

                        try {
                            val propertyNameCapitalized =
                                propertyName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }
                            val method = clazz.getMethod("set$propertyNameCapitalized", newValue!!::class.java)
                            method.invoke(obj, newValue)
                            return newValue
                        } catch (e3: Exception) {

                        }
                    }
                }
            }

            throw Exception("Cannot assign to property '$propertyName' on non-object value")
        }
    }

    class ThisNode : FlowLangNode() {
        override fun execute(context: FlowLangContext): Any? {

            val thisVariable = context.getVariable("__this")
            return thisVariable?.value
        }
    }

    class FlowLangScript(val root: FlowLangNode)
}


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
