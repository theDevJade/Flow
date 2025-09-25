package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language

import com.thedevjade.flow.flowlang.language.parsing.EnhancedSyntaxException
import com.thedevjade.flow.flowlang.language.parsing.FlowLangError
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.GlobalHooks
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.*
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.parsing.FlowLangParser
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.types.Vector3
import java.util.concurrent.ConcurrentHashMap


class FlowLangEngine private constructor() {
    private val functions = ConcurrentHashMap<String, MutableList<FlowLangFunction>>()
    private val types = ConcurrentHashMap<String, FlowLangType>()
    private val events = ConcurrentHashMap<String, FlowLangEvent>()
    private val globalVariables = ConcurrentHashMap<String, FlowLangVariable>()
    private val classes = ConcurrentHashMap<String, FlowLangClass>()

    companion object {
        @Volatile
        private var instance: FlowLangEngine? = null


        fun getInstance(): FlowLangEngine {
            return instance ?: synchronized(this) {
                instance ?: FlowLangEngine().also { instance = it }
            }
        }
    }

    init {
        registerType(FlowLangType("object", Any::class.java))
        registerType(FlowLangType("number", Double::class.java))
        registerType(FlowLangType("text", String::class.java))
        registerType(FlowLangType("boolean", Boolean::class.java))
        registerType(FlowLangType("list", List::class.java))

        registerType(FlowLangType("Vector3", Vector3::class.java))

        registerFunction(FlowLangFunction("print", { args ->
            GlobalHooks.loggingHook.error(args[0]?.toString() ?: "null")
            null
        }, arrayOf(FlowLangParameter("message", "text"))))

        registerFunction(
            FlowLangFunction(
                "random", { args ->
                    val min = (args[0] as? Number)?.toDouble() ?: 0.0
                    val max = (args[1] as? Number)?.toDouble() ?: 1.0
                    Math.random() * (max - min) + min
                }, arrayOf(
                    FlowLangParameter("min", "number"),
                    FlowLangParameter("max", "number")
                )
            )
        )


        registerFunction(FlowLangFunction("listEvents", { args ->
            listEvents().joinToString(", ")
        }, arrayOf()))

        registerFunction(FlowLangFunction("getEventInfo", { args ->
            val eventName = args[0]?.toString()
            if (eventName != null) {
                getEventInfo(eventName) ?: "Event '$eventName' not found"
            } else {
                "Event name required"
            }
        }, arrayOf(FlowLangParameter("eventName", "text"))))

        registerFunction(FlowLangFunction("getEventParameters", { args ->
            val eventName = args[0]?.toString()
            if (eventName != null) {
                val params = getEventParameters(eventName)
                if (params != null) {
                    params.joinToString(", ") { param ->
                        "${param["name"]}: ${param["type"]}${if (param["optional"] == true) " (optional)" else ""}"
                    }
                } else {
                    "Event '$eventName' not found"
                }
            } else {
                "Event name required"
            }
        }, arrayOf(FlowLangParameter("eventName", "text"))))


        registerFunction(FlowLangFunction("listClasses", { args ->
            listClasses().joinToString(", ")
        }, arrayOf()))

        registerFunction(FlowLangFunction("getClassInfo", { args ->
            val className = args[0]?.toString()
            if (className != null) {
                getClassInfo(className) ?: "Class '$className' not found"
            } else {
                "Class name required"
            }
        }, arrayOf(FlowLangParameter("className", "text"))))

        registerFunction(FlowLangFunction("hasClass", { args ->
            val className = args[0]?.toString()
            if (className != null) {
                hasClass(className)
            } else {
                false
            }
        }, arrayOf(FlowLangParameter("className", "text"))))
    }

    fun flush() {
        events.values.forEach { it.clearHandlers() }
    }

    fun registerType(type: FlowLangType) {
        types[type.name] = type
    }

    fun registerFunction(function: FlowLangFunction) {
        functions.getOrPut(function.name) { mutableListOf() }.add(function)
    }

    fun registerEvent(event: FlowLangEvent) {
        events[event.name] = event
    }


    fun execute(scriptText: String, context: FlowLangContext? = null): Any? {
        val ctx = context ?: FlowLangContext()

        val parser = FlowLangParser()
        try {
            val script = parser.parse(scriptText)


            if (parser.hasErrors()) {
                val errorSummary = parser.getErrorSummary()
                throw RuntimeException("Parsing errors found:\n$errorSummary")
            }

            val executor = FlowLangExecutor()
            return executor.executeScript(script, ctx)
        } catch (e: EnhancedSyntaxException) {
            val error = e.toFlowLangError()
            throw RuntimeException("Syntax error: ${error.message}\n$error")
        }
    }


    fun executeWithErrorReporting(
        scriptText: String,
        context: FlowLangContext? = null
    ): Pair<Any?, List<FlowLangError>> {
        val ctx = context ?: FlowLangContext()

        val parser = FlowLangParser()
        try {
            val script = parser.parse(scriptText)

            val executor = FlowLangExecutor()
            val result = executor.executeScript(script, ctx)
            return Pair(result, parser.getAllIssues())
        } catch (e: EnhancedSyntaxException) {
            val error = e.toFlowLangError()
            return Pair<Any?, List<FlowLangError>>(null, listOf(error))
        }
    }

    fun getFunction(name: String): FlowLangFunction? = functions[name]?.firstOrNull()

    fun getFunction(name: String, argCount: Int): FlowLangFunction? {
        return functions[name]?.find { it.parameters.size == argCount }
    }

    fun getFunctions(name: String): List<FlowLangFunction> = functions[name] ?: emptyList()

    fun getType(name: String): FlowLangType? = types[name]

    fun getEvent(name: String): FlowLangEvent? = events[name]

    fun triggerEvent(eventName: String, vararg parameters: Any) {
        events[eventName]?.trigger(parameters)
    }

    fun getGlobalVariable(name: String): FlowLangVariable? = globalVariables[name]

    fun setGlobalVariable(name: String, value: Any?) {
        globalVariables[name]?.let { variable ->
            variable.value = value
        } ?: run {
            globalVariables[name] = FlowLangVariable(name, value)
        }
    }


    fun getAllEventInfo(): List<String> {
        return events.values.map { it.getEventInfo() }
    }


    fun getEventInfo(eventName: String): String? {
        return events[eventName]?.getEventInfo()
    }


    fun getEventParameters(eventName: String): List<Map<String, Any>>? {
        return events[eventName]?.getParameterInfo()
    }


    fun listEvents(): List<String> {
        return events.keys.toList()
    }


    fun registerEventWithParams(name: String, parameters: Array<FlowLangParameter>, description: String = "") {
        events[name] = FlowLangEvent(name, parameters, description)
    }


    fun registerClass(flowLangClass: FlowLangClass) {
        classes[flowLangClass.name] = flowLangClass
    }


    fun getClass(className: String): FlowLangClass? = classes[className]


    fun createInstance(className: String, args: Array<Any?>): FlowLangInstance? {
        val classDef = classes[className] ?: return null
        return classDef.createInstance(args)
    }


    fun listClasses(): List<String> {
        return classes.keys.toList()
    }


    fun getClassInfo(className: String): String? {
        return classes[className]?.getClassInfo()
    }


    fun hasClass(className: String): Boolean {
        return classes.containsKey(className)
    }
}
