package flow.api.implementation

import flow.api.*

class FlowApiImpl : FlowApi {
    override val sessionManager: SessionManager = SessionManagerImpl()
    override val commandRegistry: CommandRegistry = CommandRegistryImpl()
    override val fileSystemAccess: FileSystemAccess = FileSystemAccessImpl()
    override val webSocketProvider: WebSocketProvider = WebSocketProviderImpl()
}
