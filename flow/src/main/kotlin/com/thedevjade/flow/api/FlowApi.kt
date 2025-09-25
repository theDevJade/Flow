package flow.api

interface FlowApi {
    val sessionManager: SessionManager
    val commandRegistry: CommandRegistry
    val fileSystemAccess: FileSystemAccess
    val webSocketProvider: WebSocketProvider
}
