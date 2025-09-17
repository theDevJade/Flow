package com.thedevjade.io.flowlang.language.nodes

import com.thedevjade.io.flowlang.language.memory.FlowLangContext

class LiteralNode(private val value: Any?) : FlowLangNode() {
    override fun execute(context: FlowLangContext): Any? = value
}
