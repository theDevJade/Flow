package com.thedevjade.flow.webserver.terminal

import com.thedevjade.flow.common.models.FlowLogger
import kotlinx.serialization.json.JsonElement

interface TerminalCommand {
    val name: String
    val description: String
    val usage: String

    suspend fun execute(args: List<String>, context: TerminalContext): TerminalResult
}

data class TerminalContext(
    val sessionId: String,
    val userId: String?,
    val currentDirectory: String,
    val pageId: String?,
    val workspaceState: MutableMap<String, Any> = mutableMapOf()
)

sealed class TerminalResult {
    data class Success(
        val output: List<String> = emptyList(),
        val newDirectory: String? = null,
        val data: Map<String, JsonElement> = emptyMap()
    ) : TerminalResult()

    data class Error(
        val message: String,
        val code: Int = 1
    ) : TerminalResult()

    data class Stream(
        val output: String,
        val stream: String = "stdout" // stdout or stderr
    ) : TerminalResult()
}

class TerminalCommandRegistry {
    private val commands = mutableMapOf<String, TerminalCommand>()

    fun register(command: TerminalCommand) {
        commands[command.name] = command
        FlowLogger.info("TerminalCommandRegistry", "Registered command: ${command.name}")
    }

    fun unregister(commandName: String) {
        commands.remove(commandName)
        FlowLogger.info("TerminalCommandRegistry", "Unregistered command: $commandName")
    }

    fun getCommand(name: String): TerminalCommand? = commands[name]

    fun getAllCommands(): Map<String, TerminalCommand> = commands.toMap()

    fun getCommandNames(): List<String> = commands.keys.toList().sorted()

    fun findCommandsStartingWith(prefix: String): List<String> {
        return commands.keys.filter { it.startsWith(prefix, ignoreCase = true) }.sorted()
    }
}