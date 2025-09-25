package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory

class FlowLangType(
    val name: String,
    val kotlinType: Class<*>,
    private val fromStringLiteral: ((String) -> Any?)? = null
) {
    private val stringConverter: (String) -> Any? = fromStringLiteral ?: when (kotlinType) {
        String::class.java -> { s -> s }
        Double::class.java -> { s -> s.toDouble() }
        Boolean::class.java -> { s -> s.toBoolean() }
        else -> { value ->
            try {
                kotlinType.cast(value)
            } catch (e: Exception) {
                value
            }
        }
    }

    fun convert(value: Any?): Any? {
        if (value == null) return null
        if (kotlinType.isInstance(value)) return value
        if (value is String) return stringConverter(value)
        if (kotlinType == Double::class.java && value is Number) {
            return value.toDouble()
        }
        return try {
            kotlinType.cast(value)
        } catch (e: Exception) {
            value
        }
    }
}
