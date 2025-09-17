package com.thedevjade.io.flowlang

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Main FlowLang scripting language entry point
 */
object FlowLang {
    /**
     * A boolean indicative of whether FlowLang is started or not.
     */
    var started: Boolean = false
        private set

    val assemblies: MutableList<Class<*>> = mutableListOf()

    /**
     * The current configuration for FlowLang, will be null if FlowLang is not started.
     */
    var configuration: FlowLangConfiguration? = null
        private set

    var watcher: FlowLangWatcher? = null
        private set

    fun start(configuration: FlowLangConfiguration? = null) {
        if (started) return

        this.configuration = configuration ?: FlowLangConfiguration()

        val skribePath = File(this.configuration!!.flowLangPath)
        if (!skribePath.exists()) {
            skribePath.mkdirs()
        }

        GlobalHooks.loggingHook.info(File(this.configuration!!.flowLangPath).absolutePath)

        LanguageLoader.load()
        if (this.configuration!!.flowLangDirectoryWatch) {
            watcher = FlowLangWatcher()
            watcher?.watchDirectory()
        }

        started = true
    }

    fun stop() {
        if (!started) return
        configuration = null
        started = false
    }
}

data class FlowLangConfiguration(
    val loggingHook: LoggingHook = ConsoleHook(),
    var flowLangPath: String = "./flowlang",
    val flowLangDirectoryWatch: Boolean = true
)

object GlobalHooks {
    val loggingHook: LoggingHook get() = FlowLang.configuration?.loggingHook ?: ConsoleHook()
}
