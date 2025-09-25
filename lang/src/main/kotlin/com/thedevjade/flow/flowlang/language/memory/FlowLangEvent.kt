package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.GlobalHooks
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangExecutor
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.nodes.BinaryOpNode
import java.util.concurrent.CopyOnWriteArrayList

class FlowLangEvent(
    val name: String,
    val parameters: Array<FlowLangParameter>,
    val description: String = ""
) {
    private val handlers = CopyOnWriteArrayList<Pair<BinaryOpNode.FlowLangScript, FlowLangContext>>()

    fun registerHandler(script: BinaryOpNode.FlowLangScript, context: FlowLangContext) {
        handlers.add(Pair(script, context))
    }

    fun unregisterHandler(script: BinaryOpNode.FlowLangScript, context: FlowLangContext) {
        handlers.removeAll { it.first == script && it.second == context }
    }

    fun clearHandlers() {
        handlers.clear()
    }

    fun trigger(args: Array<out Any>) {
        if (args.size != parameters.size) {
            throw Exception("Event '$name' requires ${parameters.size} parameters, but got ${args.size}")
        }

        val executor = FlowLangExecutor()

        handlers.forEach { (script, ctx) ->
            try {
                for (i in args.indices) {
                    ctx.setVariable(parameters[i].name, args[i])
                }
                executor.executeScript(script, ctx)
            } catch (ex: Exception) {
                GlobalHooks.loggingHook.error("Error in handler for '$name': ${ex.message}")
            }
        }
    }


    fun getEventInfo(): String {
        val paramInfo = parameters.joinToString(", ") { param ->
            "${param.name}: ${param.typeName}${if (param.isOptional) " (optional)" else ""}"
        }
        return "Event '$name': $paramInfo${if (description.isNotEmpty()) " - $description" else ""}"
    }


    fun getParameterInfo(): List<Map<String, Any>> {
        return parameters.map { param ->
            mapOf(
                "name" to param.name,
                "type" to param.typeName,
                "optional" to param.isOptional,
                "defaultValue" to (param.defaultValue ?: "none")
            )
        }
    }


    fun validateParameters(args: Array<out Any?>): Boolean {
        if (args.size > parameters.size) return false


        val requiredParams = parameters.count { !it.isOptional }
        if (args.size < requiredParams) return false

        for (i in args.indices) {
            val param = parameters[i]
            if (!param.isOptional && args[i] == null) return false
        }

        return true
    }
}
