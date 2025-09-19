package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory

class FlowLangParameter(
    val name: String,
    val typeName: String,
    val isOptional: Boolean = false,
    val defaultValue: Any? = null
) {
    constructor(name: String, typeName: String) : this(name, typeName, false, null)
    constructor(name: String, typeName: String, defaultValue: Any?) : this(name, typeName, true, defaultValue)
}
