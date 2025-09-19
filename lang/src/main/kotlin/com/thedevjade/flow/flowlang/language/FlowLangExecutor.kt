package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangContext
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.nodes.BinaryOpNode

/**
 * Executes a parsed script
 */
class FlowLangExecutor {
    fun executeScript(script: BinaryOpNode.FlowLangScript, context: FlowLangContext): Any? {
        return script.root.execute(context)
    }
}
