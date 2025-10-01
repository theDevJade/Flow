package com.thedevjade.flow.extension.api

import kotlin.reflect.KClass


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TerminalCommand(
    val name: String,
    val description: String = "",
    val usage: String = "",
    val aliases: Array<String> = [],
    val permission: String = "",
    val async: Boolean = false
)


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GraphNode(
    val name: String,
    val category: String = "General",
    val description: String = "",
    val icon: String = "",
    val color: String = "#4A90E2",
    val nodeType: NodeType = NodeType.ACTION,
    val inputs: Array<GraphPort> = [],
    val outputs: Array<GraphPort> = []
)


enum class NodeType {
    TRIGGER,
    ACTION
}


@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class GraphPort(
    val name: String,
    val type: String = "any",
    val description: String = "",
    val required: Boolean = true,
    val defaultValue: String = ""
)


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlowLangFunction(
    val name: String,
    val description: String = "",
    val category: String = "General",
    val async: Boolean = false
)


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlowLangEvent(
    val name: String,
    val description: String = "",
    val category: String = "General"
)


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlowLangSyntax(
    val name: String,
    val pattern: String,
    val description: String = "",
    val priority: Int = 0
)


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlowLangType(
    val name: String,
    val description: String = "",
    val category: String = "General",
    val javaType: KClass<*> = Any::class
)


@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlowLangParameter(
    val name: String,
    val type: String = "any",
    val description: String = "",
    val required: Boolean = true,
    val defaultValue: String = ""
)


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExtensionLifecycle(
    val phase: LifecyclePhase
)

enum class LifecyclePhase {
    INITIALIZE,
    ENABLE,
    DISABLE,
    DESTROY
}


@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Inject(
    val required: Boolean = true
)


@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigProperty(
    val key: String,
    val defaultValue: String = "",
    val description: String = "",
    val type: ConfigType = ConfigType.STRING
)

enum class ConfigType {
    STRING,
    INT,
    BOOLEAN,
    DOUBLE,
    LIST
}


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlowExtensionAnnotation(
    val name: String,
    val version: String = "1.0.0",
    val description: String = "",
    val author: String = ""
)


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TriggerNode(
    val name: String,
    val category: String = "Triggers",
    val description: String = "",
    val icon: String = "play_circle",
    val color: String = "#4CAF50"
)


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ActionNode(
    val name: String,
    val category: String = "Actions",
    val description: String = "",
    val icon: String = "settings",
    val color: String = "#2196F3"
)


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlowFunction(
    val name: String = "",
    val description: String = "",
    val category: String = "General"
)


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlowEvent(
    val name: String,
    val description: String = "",
    val category: String = "Events"
)


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlowType(
    val name: String,
    val description: String = "",
    val category: String = "Types"
)


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Command(
    val name: String,
    val description: String = "",
    val usage: String = "",
    val aliases: Array<String> = []
)


@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Config(
    val key: String,
    val defaultValue: String = "",
    val description: String = ""
)


@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Port(
    val name: String = "",
    val description: String = "",
    val type: String = "any",
    val required: Boolean = true,
    val defaultValue: String = ""
)


@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Output(
    val name: String = "",
    val description: String = "",
    val type: String = "any"
)
