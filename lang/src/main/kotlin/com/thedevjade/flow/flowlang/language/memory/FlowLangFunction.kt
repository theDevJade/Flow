package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangEngine

class FlowLangFunction(
    val name: String,
    val implementation: (Array<Any?>) -> Any?,
    val parameters: Array<FlowLangParameter>
) {
    fun invoke(args: Array<Any?>): Any? {
        val required = parameters.count { !it.isOptional }
        if (args.size < required) {
            throw Exception("Function '$name' requires at least $required arguments, but got ${args.size}")
        }
        if (args.size > parameters.size) {
            throw Exception("Function '$name' accepts at most ${parameters.size} arguments, but got ${args.size}")
        }

        val newArgs = if (args.size < parameters.size) {
            val expandedArgs = arrayOfNulls<Any?>(parameters.size)
            System.arraycopy(args, 0, expandedArgs, 0, args.size)
            for (i in args.size until parameters.size) {
                expandedArgs[i] = parameters[i].defaultValue
            }
            expandedArgs
        } else {
            args
        }

        for (i in newArgs.indices) {
            val param = parameters[i]
            val type = FlowLangEngine.getInstance().getType(param.typeName)
                ?: throw Exception("Unknown type '${param.typeName}' for parameter '${param.name}'")

            try {
                newArgs[i] = type.convert(newArgs[i])
            } catch (ex: Exception) {
                throw Exception("Cannot convert argument '${newArgs[i]}' to '${param.typeName}': ${ex.message}")
            }
        }

        return implementation(newArgs)
    }
}
