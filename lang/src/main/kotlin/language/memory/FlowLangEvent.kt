package com.thedevjade.io.flowlang.language.memory

import com.thedevjade.io.flowlang.GlobalHooks
import com.thedevjade.io.flowlang.language.FlowLangExecutor
import com.thedevjade.io.flowlang.language.nodes.BinaryOpNode
import java.util.concurrent.CopyOnWriteArrayList

class FlowLangEvent(
    val name: String,
    val parameters: Array<FlowLangParameter>
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
}
