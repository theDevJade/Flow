package flow.api

interface CommandRegistry {
    fun registerCommand(command: String, handler: CommandHandler)
    fun unregisterCommand(command: String)
    fun executeCommand(command: String, args: Map<String, Any>)
}

fun interface CommandHandler {
    fun handle(args: Map<String, Any>)
}
