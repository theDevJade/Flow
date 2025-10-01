package flow.api.implementation

import flow.api.CommandHandler
import flow.api.CommandRegistry
import java.util.concurrent.ConcurrentHashMap

class CommandRegistryImpl : CommandRegistry {
    private val handlers = ConcurrentHashMap<String, CommandHandler>()

    override fun registerCommand(command: String, handler: CommandHandler) {
        handlers[command] = handler
    }

    override fun unregisterCommand(command: String) {
        handlers.remove(command)
    }

    override fun executeCommand(command: String, args: Map<String, Any>) {
        handlers[command]?.handle(args)
    }
}
