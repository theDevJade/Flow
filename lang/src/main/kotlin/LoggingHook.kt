package com.thedevjade.io.flowlang

/**
 * Interface for logging hooks in FlowLang
 */
interface LoggingHook {
    fun info(message: String)
    fun error(message: String)
    fun warn(message: String)
    fun debug(message: String)
}

/**
 * Console implementation of LoggingHook
 */
class ConsoleHook : LoggingHook {
    override fun info(message: String) = println("[INFO] $message")
    override fun error(message: String) = println("[ERROR] $message")
    override fun warn(message: String) = println("[WARN] $message")
    override fun debug(message: String) = println("[DEBUG] $message")
}
