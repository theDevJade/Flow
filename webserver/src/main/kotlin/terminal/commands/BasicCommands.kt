package com.thedevjade.flow.webserver.terminal.commands

import com.thedevjade.flow.webserver.terminal.TerminalCommand
import com.thedevjade.flow.webserver.terminal.TerminalContext
import com.thedevjade.flow.webserver.terminal.TerminalResult

class HelpCommand : TerminalCommand {
    override val name = "help"
    override val description = "Display help information about available commands"
    override val usage = "help [command]"
    
    override suspend fun execute(args: List<String>, context: TerminalContext): TerminalResult {
        if (args.isEmpty()) {
            return TerminalResult.Success(
                output = listOf(
                    "Flow Terminal - Available Commands:",
                    "",
                    "help              - Show this help message",
                    "help <command>    - Show help for specific command", 
                    "clear             - Clear the terminal screen",
                    "echo <text>       - Display text",
                    "pwd               - Print current workspace directory",
                    "ls                - List workspace items",
                    "cd <path>         - Change workspace directory",
                    "graph             - Graph-related commands",
                    "workspace         - Workspace management commands",
                    "flow              - Flow application commands",
                    "",
                    "Type 'help <command>' for more information about a specific command."
                )
            )
        } else {
            val commandName = args[0]
            return when (commandName) {
                "clear" -> TerminalResult.Success(
                    output = listOf("clear - Clear the terminal screen", "Usage: clear")
                )
                "echo" -> TerminalResult.Success(
                    output = listOf("echo - Display text", "Usage: echo <text>", "Example: echo Hello World")
                )
                "pwd" -> TerminalResult.Success(
                    output = listOf("pwd - Print current workspace directory", "Usage: pwd")
                )
                "ls" -> TerminalResult.Success(
                    output = listOf("ls - List workspace items", "Usage: ls [path]", "Lists graphs, files, and workspace objects")
                )
                "cd" -> TerminalResult.Success(
                    output = listOf("cd - Change workspace directory", "Usage: cd <path>", "Navigate between workspace contexts")
                )
                "graph" -> TerminalResult.Success(
                    output = listOf("graph - Graph operations", "Usage: graph <action>", "Actions: list, create, open, save, delete")
                )
                "workspace" -> TerminalResult.Success(
                    output = listOf("workspace - Workspace management", "Usage: workspace <action>", "Actions: info, switch, create, list")
                )
                "flow" -> TerminalResult.Success(
                    output = listOf("flow - Flow application commands", "Usage: flow <action>", "Actions: status, info, version")
                )
                else -> TerminalResult.Error("Unknown command: $commandName")
            }
        }
    }
}

class ClearCommand : TerminalCommand {
    override val name = "clear"
    override val description = "Clear the terminal screen"
    override val usage = "clear"
    
    override suspend fun execute(args: List<String>, context: TerminalContext): TerminalResult {
        return TerminalResult.Success(
            output = listOf(),
            data = mapOf("action" to kotlinx.serialization.json.JsonPrimitive("clear"))
        )
    }
}

class EchoCommand : TerminalCommand {
    override val name = "echo"
    override val description = "Display text"
    override val usage = "echo <text>"
    
    override suspend fun execute(args: List<String>, context: TerminalContext): TerminalResult {
        val text = args.joinToString(" ")
        return TerminalResult.Success(output = listOf(text))
    }
}

class PwdCommand : TerminalCommand {
    override val name = "pwd"
    override val description = "Print current workspace directory"
    override val usage = "pwd"
    
    override suspend fun execute(args: List<String>, context: TerminalContext): TerminalResult {
        return TerminalResult.Success(output = listOf(context.currentDirectory))
    }
}

class LsCommand : TerminalCommand {
    override val name = "ls"
    override val description = "List workspace items"
    override val usage = "ls [path]"
    
    override suspend fun execute(args: List<String>, context: TerminalContext): TerminalResult {
        val path = args.firstOrNull() ?: context.currentDirectory
        
        // Mock workspace items - this would be replaced with actual workspace querying
        val items = when (path) {
            "/" -> listOf(
                "graphs/",
                "workspaces/",
                "files/",
                "projects/",
                "settings.json"
            )
            "/graphs" -> listOf(
                "main-graph.flow",
                "test-graph.flow", 
                "prototype.flow"
            )
            "/workspaces" -> listOf(
                "default/",
                "development/",
                "production/"
            )
            "/files" -> listOf(
                "README.md",
                "config.yaml",
                "data/"
            )
            else -> listOf("(empty directory)")
        }
        
        return TerminalResult.Success(
            output = items.map { item ->
                if (item.endsWith("/")) {
                    "📁 $item"
                } else if (item.endsWith(".flow")) {
                    "🔗 $item"
                } else if (item.endsWith(".md")) {
                    "📄 $item"
                } else if (item.endsWith(".json") || item.endsWith(".yaml")) {
                    "⚙️  $item"
                } else {
                    "📄 $item"
                }
            }
        )
    }
}

class CdCommand : TerminalCommand {
    override val name = "cd"
    override val description = "Change workspace directory"
    override val usage = "cd <path>"
    
    override suspend fun execute(args: List<String>, context: TerminalContext): TerminalResult {
        if (args.isEmpty()) {
            return TerminalResult.Success(newDirectory = "/")
        }
        
        val path = args[0]
        val newPath = when {
            path.startsWith("/") -> path
            path == ".." -> {
                val parts = context.currentDirectory.split("/").filter { it.isNotEmpty() }
                if (parts.isEmpty()) "/" else "/${parts.dropLast(1).joinToString("/")}"
            }
            path == "." -> context.currentDirectory
            else -> {
                if (context.currentDirectory == "/") "/$path" else "${context.currentDirectory}/$path"
            }
        }
        
        // Validate path exists (mock validation)
        val validPaths = setOf("/", "/graphs", "/workspaces", "/files", "/projects")
        if (!validPaths.contains(newPath)) {
            return TerminalResult.Error("Directory not found: $path")
        }
        
        return TerminalResult.Success(
            output = listOf("Changed to: $newPath"),
            newDirectory = newPath
        )
    }
}