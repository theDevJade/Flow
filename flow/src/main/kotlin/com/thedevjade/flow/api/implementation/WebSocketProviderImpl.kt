package flow.api.implementation

import flow.api.WebSocketProvider

class WebSocketProviderImpl : WebSocketProvider {
    // @TODO
    private val webSocket = "DUMMY_WEBSOCKET"

    override fun getWebSocket(): Any {
        return webSocket
    }
}
