package com.thedevjade.flow.flowPlugin.commands

import com.thedevjade.flow.api.FlowCore
import com.thedevjade.flow.common.config.ConfigurationLoader
import com.thedevjade.flow.flowPlugin.flowloader.SegmentLoader
import com.thedevjade.flow.webserver.api.FlowAPI
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.text.SimpleDateFormat
import java.util.*

class FlowCommand : CommandExecutor, TabCompleter {

    private val flowInstance = FlowCore.getInstance()

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("§cYou do not have permission to use this command.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§eUsage: /flow <status|killServer|startServer|reload|database|sessions>")
            return true
        }

        when (args[0].lowercase()) {
            "status" -> {
                // @TODO: Replace with actual status reporting
                sender.sendMessage("§a[Flow] Server status:")
                sender.sendMessage(" - Users: ${flowInstance.users.getAllUsers().size}")
                sender.sendMessage(" - Active sessions: ${FlowAPI.getInstance().getAllUserSessions().size}")
            }


            "killserver" -> {
                SegmentLoader.unload()
                sender.sendMessage("§c[Flow] Killing server...")

            }

            "startserver" -> {
                SegmentLoader.load()
                sender.sendMessage("§a[Flow] Starting server...")
            }

            "reload" -> {
                ConfigurationLoader.load()
                sender.sendMessage("§e[Flow] Reloading configuration...")
            }

            "database" -> {
                if (args.size < 2) {
                    sender.sendMessage("§eUsage: /flow database <addUser|deleteUser|listUsers>")
                    return true
                }
                when (args[1].lowercase()) {
                    "adduser" -> {
                        if (args.size < 5) {
                            sender.sendMessage("§cUsage: /flow database addUser <username> <password> <email>")
                            return true
                        }

                        val username = args[2]
                        val password = args[3]
                        val email = args[4]

                        if (!username.matches(Regex("^[a-zA-Z0-9_]{3,16}\$"))) {
                            sender.sendMessage("§cInvalid username. Use 3-16 characters: letters, numbers, underscore only.")
                            return true
                        }
                        if (password.length < 6) {
                            sender.sendMessage("§cPassword too short. Must be at least 6 characters.")
                            return true
                        }
                        if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"))) {
                            sender.sendMessage("§cInvalid email format.")
                            return true
                        }

                        suspend {
                            flowInstance.users.createUser(username, password, email)
                        }
                        sender.sendMessage("§a[Flow] Added user $username with email $email.")
                    }

                    "deleteuser" -> {
                        if (args.size < 3) {
                            sender.sendMessage("§cUsage: /flow database deleteUser <username>")
                            return true
                        }
                        val username = args[2]
                        val user = flowInstance.users.getUserByUsername(username)
                        if (user == null) {
                            sender.sendMessage("§c[Flow] Invalid username.")
                            return true
                        }
                        suspend {
                            flowInstance.users.deleteUser(user.id)
                        }
                        sender.sendMessage("§c[Flow] Deleted user $username")
                    }

                    "listusers" -> {
                        val users = flowInstance.users.getAllUsers()
                        sender.sendMessage("§a[Flow] Listing all users...")
                        users.forEach {
                            sender.sendMessage(" - [${it.id}] ${it.username} <${it.email}>")
                        }
                    }
                }
            }


            "sessions" -> {
                if (args.size < 2) {
                    sender.sendMessage("§eUsage: /flow sessions <listSessions|killAllSessions|kill>")
                    return true
                }
                when (args[1].lowercase()) {
                    "listsessions" -> {
                        val sessions =
                            FlowAPI.getInstance().getAllUserSessions()
                        sessions.forEach { s ->
                            val date = Date(s.connectedAt)
                            val format = SimpleDateFormat("yyyy.MM.dd HH:mm")
                            sender.sendMessage(" - [${s.sessionId}] User: ${s.userId}, Started: ${format.format(date)}")
                        }
                    }

                    "killallsessions" -> {
                        suspend {
                            val sessions =
                                FlowAPI.getInstance().getAllUserSessions()
                            sessions.forEach { s ->
                                FlowCore.getInstance().users.removeSession(s.sessionId)
                            }
                        }
                        sender.sendMessage("§c[Flow] Killed all sessions.")
                    }

                    "kill" -> {
                        if (args.size < 3) {
                            sender.sendMessage("§cUsage: /flow sessions kill <session_id>")
                            return true
                        }
                        val sessionId = args[2]
                        suspend {
                            flowInstance.users.removeSession(sessionId)
                        }
                        sender.sendMessage("§c[Flow] Killed session $sessionId (if it exists)")
                    }
                }
            }

            else -> sender.sendMessage("§cUnknown subcommand.")
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (!sender.isOp) return mutableListOf()

        return when (args.size) {
            1 -> listOf("status", "killServer", "startServer", "reload", "database", "sessions")
                .filter { it.startsWith(args[0], true) }.toMutableList()

            2 -> when (args[0].lowercase()) {
                "database" -> listOf("addUser", "deleteUser", "listUsers")
                    .filter { it.startsWith(args[1], true) }.toMutableList()

                "sessions" -> listOf("listSessions", "killAllSessions", "kill")
                    .filter { it.startsWith(args[1], true) }.toMutableList()

                else -> mutableListOf()
            }

            3 -> when {
                args[0].equals("database", true) && args[1].equals("deleteUser", true) -> {
                    flowInstance.users.getAllUsers().map { it.username }.toMutableList()
                }

                args[0].equals("sessions", true) && args[1].equals("kill", true) -> {
                    FlowAPI.getInstance().getAllUserSessions().map { it.sessionId }.toMutableList()
                }

                else -> mutableListOf()
            }

            4 -> if (args[0].equals("database", true) && args[1].equals("addUser", true)) {
                listOf("<password>").toMutableList()
            } else mutableListOf()

            5 -> if (args[0].equals("database", true) && args[1].equals("addUser", true)) {
                listOf("<email>").toMutableList()
            } else mutableListOf()

            else -> mutableListOf()
        }
    }
}
