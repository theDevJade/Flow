package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang

import com.thedevjade.flow.flowlang.ConsoleHook
import com.thedevjade.flow.flowlang.LoggingHook
import java.io.File


object FlowLang {

    var started: Boolean = false
        private set

    val assemblies: MutableList<Class<*>> = mutableListOf()


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
            watcher = FlowLangWatcher(this.configuration!!.flowLangPath)
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
