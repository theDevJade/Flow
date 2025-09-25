package flow.api.implementation

import flow.api.WebSocketProvider

class WebSocketProviderImpl : WebSocketProvider {
    // @DEPRACTED
    private val webSocket = "DUMMY_WEBSOCKET"

    override fun getWebSocket(): Any {
        return webSocket
    }
}
