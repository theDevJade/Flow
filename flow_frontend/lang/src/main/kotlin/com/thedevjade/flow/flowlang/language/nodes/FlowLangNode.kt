package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.nodes

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangContext

abstract class FlowLangNode {
    abstract fun execute(context: FlowLangContext): Any?
}
