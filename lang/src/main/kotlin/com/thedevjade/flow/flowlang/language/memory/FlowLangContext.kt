package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangEngine
import java.util.concurrent.ConcurrentHashMap

class FlowLangContext(private val parent: FlowLangContext? = null) {
    private val variables = ConcurrentHashMap<String, FlowLangVariable>()

    fun getVariable(name: String): FlowLangVariable? {
        return variables[name] ?: parent?.getVariable(name) ?: FlowLangEngine.Companion.getInstance().getGlobalVariable(name)
    }

    fun setVariable(name: String, value: Any?) {
        variables[name]?.let { variable ->
            variable.value = value
        } ?: run {
            variables[name] = FlowLangVariable(name, value)
        }
    }

    fun createChildContext(): FlowLangContext {
        return FlowLangContext(this)
    }
}
