package flow.api.implementation

import flow.api.WebSocketProvider

class WebSocketProviderImpl : WebSocketProvider {
    // This should be replaced with your actual WebSocket implementation
    private val webSocket = "DUMMY_WEBSOCKET" 

    override fun getWebSocket(): Any {
        return webSocket
    }
}
