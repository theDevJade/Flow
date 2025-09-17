package com.thedevjade.io.flowlang.language.nodes

import com.thedevjade.io.flowlang.language.FlowLangEngine
import com.thedevjade.io.flowlang.language.memory.FlowLangContext

class FunctionCallNode(val name: String, val arguments: List<FlowLangNode>) : FlowLangNode() {
    override fun execute(context: FlowLangContext): Any? {
        val function = FlowLangEngine.getInstance().getFunction(name)
            ?: throw Exception("Function '$name' not found")

        val args = arguments.map { it.execute(context) }.toTypedArray()
        return function.invoke(args)
    }
}
