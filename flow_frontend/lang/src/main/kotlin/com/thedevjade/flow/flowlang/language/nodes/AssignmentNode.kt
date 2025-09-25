package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.nodes

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangContext

class AssignmentNode(val name: String, val value: FlowLangNode, val typeName: String? = null) : FlowLangNode() {
    override fun execute(context: FlowLangContext): Any? {
        val result = value.execute(context)

        if (name.contains(".")) {
            val parts = name.split(".")
            val var0 = context.getVariable(parts[0])
                ?: throw Exception("Variable '${parts[0]}' not found")
            var obj = var0.value


            for (i in 1 until parts.size - 1) {
                if (obj == null) {
                    throw Exception("Cannot access '${parts[i]}' on null")
                }

                val t = obj::class.java
                val prop = t.getDeclaredField(parts[i])
                prop.isAccessible = true
                obj = prop.get(obj)
            }


            if (obj == null) {
                throw Exception("Cannot access '${parts.last()}' on null")
            }

            val t = obj::class.java
            val prop = t.getDeclaredField(parts.last())
            prop.isAccessible = true
            prop.set(obj, result)
        } else {
            context.setVariable(name, result)
        }

        return result
    }
}
