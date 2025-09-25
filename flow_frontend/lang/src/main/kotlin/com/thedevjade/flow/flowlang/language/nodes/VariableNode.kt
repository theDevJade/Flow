package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.nodes

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangContext

class VariableNode(val name: String) : FlowLangNode() {
    override fun execute(context: FlowLangContext): Any? {
        if (name.contains(".")) {
            val parts = name.split(".")
            val var0 = context.getVariable(parts[0])
                ?: throw Exception("Variable '${parts[0]}' not found")
            var obj = var0.value

            for (i in 1 until parts.size) {
                if (obj == null) {
                    throw Exception("Cannot access '${parts[i]}' on null")
                }

                val t = obj::class.java
                val prop = t.getDeclaredField(parts[i])
                prop.isAccessible = true
                obj = prop.get(obj)
            }

            return obj
        }

        val variable = context.getVariable(name)
            ?: throw Exception("Variable '$name' not found")
        return variable.value
    }
}
