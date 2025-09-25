package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.nodes

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangEngine
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangContext

class FunctionCallNode(val name: String, val arguments: List<FlowLangNode>) : FlowLangNode() {
    override fun execute(context: FlowLangContext): Any? {
        val engine = FlowLangEngine.getInstance()
        val args = arguments.map { it.execute(context) }.toTypedArray()


        val function = engine.getFunction(name, args.size)
            ?: engine.getFunction(name)
            ?: throw Exception("Function '$name' with ${args.size} parameters not found")

        return function.invoke(args)
    }
}
