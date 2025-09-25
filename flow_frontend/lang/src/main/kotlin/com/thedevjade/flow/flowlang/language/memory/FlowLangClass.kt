package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.nodes.BinaryOpNode


class FlowLangClass(
    val name: String,
    val superClassName: String? = null,
    val properties: MutableMap<String, FlowLangProperty> = mutableMapOf(),
    val methods: MutableMap<String, FlowLangMethod> = mutableMapOf(),
    val constructor: FlowLangConstructor? = null
) {

    fun createInstance(args: Array<Any?>): FlowLangInstance {
        val instance = FlowLangInstance(this)


        properties.values.forEach { prop ->
            if (!prop.isReadOnly) {
                instance.setProperty(prop.name, prop.defaultValue)
            } else {

                instance.properties[prop.name] = prop.defaultValue
            }
        }


        if (constructor != null) {


        }

        return instance
    }


    fun getMethod(name: String): FlowLangMethod? = methods[name]


    fun getProperty(name: String): FlowLangProperty? = properties[name]


    fun addMethod(method: FlowLangMethod) {
        methods[method.name] = method
    }


    fun addProperty(property: FlowLangProperty) {
        properties[property.name] = property
    }


    fun hasSuperClass(): Boolean = superClassName != null


    fun getClassInfo(): String {
        val props = properties.keys.joinToString(", ")
        val methods = methods.keys.joinToString(", ")
        val extends = if (superClassName != null) " extends $superClassName" else ""
        return "class $name$extends { properties: [$props], methods: [$methods] }"
    }
}


class FlowLangProperty(
    val name: String,
    val typeName: String,
    val visibility: Visibility = Visibility.PUBLIC,
    val defaultValue: Any? = null,
    val isReadOnly: Boolean = false
) {
    enum class Visibility {
        PUBLIC, PRIVATE, PROTECTED
    }
}


class FlowLangMethod(
    val name: String,
    val parameters: Array<FlowLangParameter>,
    val body: BinaryOpNode.FlowLangScript,
    val visibility: Visibility = Visibility.PUBLIC,
    val isStatic: Boolean = false
) {
    enum class Visibility {
        PUBLIC, PRIVATE, PROTECTED
    }
}


class FlowLangConstructor(
    val parameters: Array<FlowLangParameter>,
    val body: BinaryOpNode.FlowLangScript
)


class FlowLangInstance(
    val classDef: FlowLangClass
) {
    internal val properties = mutableMapOf<String, Any?>()


    fun getProperty(name: String): Any? {
        return properties[name]
    }


    fun setProperty(name: String, value: Any?) {
        val prop = classDef.getProperty(name)
        if (prop != null && prop.isReadOnly) {
            throw Exception("Cannot modify read-only property '$name'")
        }
        properties[name] = value
    }


    fun callMethod(name: String, args: Array<Any?>): Any? {
        val method = classDef.getMethod(name)
        if (method == null) {
            throw Exception("Method '$name' not found in class '${classDef.name}'")
        }

        if (args.size != method.parameters.size) {
            throw Exception("Method '$name' requires ${method.parameters.size} parameters, but got ${args.size}")
        }


        val context = FlowLangContext()


        context.setVariable("this", this)
        context.setVariable("__this", this)


        properties.forEach { (name, value) ->
            context.setVariable(name, value)
        }


        for (i in args.indices) {
            context.setVariable(method.parameters[i].name, args[i])
        }


        val executor = com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangExecutor()
        val result = executor.executeScript(method.body, context)


        properties.forEach { (name, _) ->
            val variable = context.getVariable(name)
            if (variable != null) {
                properties[name] = variable.value
            }
        }

        return result
    }


    fun getPropertyNames(): Set<String> = properties.keys


    fun hasProperty(name: String): Boolean = properties.containsKey(name)


    fun getInstanceInfo(): String {
        val props = properties.entries.joinToString(", ") { (k, v) -> "$k=$v" }
        return "Instance of ${classDef.name} { $props }"
    }
}
