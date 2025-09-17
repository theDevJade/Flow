package com.thedevjade.io.flowlang.language

import com.thedevjade.io.flowlang.GlobalHooks
import com.thedevjade.io.flowlang.language.memory.FlowLangContext
import com.thedevjade.io.flowlang.language.memory.FlowLangEvent
import com.thedevjade.io.flowlang.language.memory.FlowLangFunction
import com.thedevjade.io.flowlang.language.memory.FlowLangParameter
import com.thedevjade.io.flowlang.language.memory.FlowLangType
import com.thedevjade.io.flowlang.language.memory.FlowLangVariable
import com.thedevjade.io.flowlang.language.parsing.FlowLangParser
import com.thedevjade.io.flowlang.language.types.Vector3
import java.util.concurrent.ConcurrentHashMap

/**
 * The core FlowLang language system
 */
class FlowLangEngine private constructor() {
    private val functions = ConcurrentHashMap<String, FlowLangFunction>()
    private val types = ConcurrentHashMap<String, FlowLangType>()
    private val events = ConcurrentHashMap<String, FlowLangEvent>()
    private val globalVariables = ConcurrentHashMap<String, FlowLangVariable>()

    companion object {
        @Volatile
        private var instance: FlowLangEngine? = null

        /**
         * Singleton instance of the FlowLang engine
         */
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

        registerFunction(FlowLangFunction("random", { args ->
            val min = (args[0] as? Number)?.toDouble() ?: 0.0
            val max = (args[1] as? Number)?.toDouble() ?: 1.0
            Math.random() * (max - min) + min
        }, arrayOf(
            FlowLangParameter("min", "number"),
            FlowLangParameter("max", "number")
        )))
    }

    fun flush() {
        events.values.forEach { it.clearHandlers() }
    }

    fun registerType(type: FlowLangType) {
        types[type.name] = type
    }

    fun registerFunction(function: FlowLangFunction) {
        functions[function.name] = function
    }

    fun registerEvent(event: FlowLangEvent) {
        events[event.name] = event
    }

    /**
     * Executes a FlowLang script
     */
    fun execute(scriptText: String, context: FlowLangContext? = null): Any? {
        val ctx = context ?: FlowLangContext()

        val parser = FlowLangParser()
        val script = parser.parse(scriptText)

        val executor = FlowLangExecutor()
        return executor.executeScript(script, ctx)
    }

    fun getFunction(name: String): FlowLangFunction? = functions[name]

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
}
