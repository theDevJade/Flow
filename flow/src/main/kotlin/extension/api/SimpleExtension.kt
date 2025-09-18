package com.thedevjade.flow.extension.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel


abstract class SimpleExtension : FlowExtension {
    protected lateinit var context: ExtensionContext
    protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    override val name: String by lazy {
        this::class.annotations.find { it is FlowExtensionAnnotation }?.let {
            (it as FlowExtensionAnnotation).name
        } ?: this::class.simpleName ?: "Unknown"
    }

    override val version: String by lazy {
        this::class.annotations.find { it is FlowExtensionAnnotation }?.let {
            (it as FlowExtensionAnnotation).version
        } ?: "1.0.0"
    }

    override val description: String by lazy {
        this::class.annotations.find { it is FlowExtensionAnnotation }?.let {
            (it as FlowExtensionAnnotation).description
        } ?: ""
    }

    override val author: String by lazy {
        this::class.annotations.find { it is FlowExtensionAnnotation }?.let {
            (it as FlowExtensionAnnotation).author
        } ?: ""
    }

    override val dependencies: List<String> = emptyList()

    override fun initialize(context: ExtensionContext) {
        this.context = context
        onInitialize()
    }

    override fun enable() {
        onEnable()
    }

    override fun disable() {
        onDisable()
    }

    override fun destroy() {
        onDestroy()
        scope.cancel()
    }


    protected open fun onInitialize() {}
    protected open fun onEnable() {}
    protected open fun onDisable() {}
    protected open fun onDestroy() {}


    protected fun log(message: String) = context.logger.info(message)
    protected fun logError(message: String, throwable: Throwable? = null) =
        context.logger.error(message, throwable)

    protected fun schedule(task: suspend () -> Unit) = scope.launch { task() }
    protected fun scheduleSync(task: () -> Unit) = scope.launch { task() }

    protected fun config(key: String, defaultValue: String = "") =
        context.config.getString(key, defaultValue)
    protected fun configInt(key: String, defaultValue: Int = 0) =
        context.config.getInt(key, defaultValue)
    protected fun configBoolean(key: String, defaultValue: Boolean = false) =
        context.config.getBoolean(key, defaultValue)
}


abstract class SimpleTriggerNode : SimpleExtension() {

    abstract suspend fun execute(): TriggerResult
}


abstract class SimpleActionNode : SimpleExtension() {

    abstract suspend fun execute(inputs: Map<String, Any?>): ActionResult
}


sealed class TriggerResult {
    object Success : TriggerResult()
    data class Error(val message: String) : TriggerResult()
    data class Skip(val reason: String) : TriggerResult()
}


sealed class ActionResult {
    data class Success(val outputs: Map<String, Any?>) : ActionResult()
    data class Error(val message: String) : ActionResult()
    data class Skip(val reason: String) : ActionResult()
}


abstract class SimpleFlowFunction : SimpleExtension() {


}


abstract class SimpleFlowEvent : SimpleExtension() {


}


abstract class SimpleFlowType : SimpleExtension() {


    abstract fun convert(value: Any?): Any?
    abstract fun validate(value: Any?): Boolean
}
