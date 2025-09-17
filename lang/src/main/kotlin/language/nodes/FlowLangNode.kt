package com.thedevjade.io.flowlang.language.nodes

import com.thedevjade.io.flowlang.language.memory.FlowLangContext

abstract class FlowLangNode {
    abstract fun execute(context: FlowLangContext): Any?
}
