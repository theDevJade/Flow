package com.thedevjade.flow.api.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass


class EventManager(private val scope: CoroutineScope) {


    val eventFlows = ConcurrentHashMap<KClass<*>, MutableSharedFlow<Any>>()


    val eventListeners = ConcurrentHashMap<KClass<*>, MutableList<EventListener<*>>>()


    private var totalEventsEmitted = 0L
    private var totalEventsProcessed = 0L


    suspend fun <T : Any> emit(event: T) {
        val eventType = event::class
        totalEventsEmitted++


        val flow = eventFlows.getOrPut(eventType) {
            MutableSharedFlow<Any>(replay = 0, extraBufferCapacity = 1000)
        }


        flow.tryEmit(event)


        eventListeners[eventType]?.forEach { listener ->
            if (listener.priority == EventPriority.HIGH) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    (listener as EventListener<T>).onEvent(event)
                    totalEventsProcessed++
                } catch (e: Exception) {

                    System.err.println("Error processing high-priority event listener: ${e.message}")
                }
            }
        }


        scope.launch {
            eventListeners[eventType]?.forEach { listener ->
                if (listener.priority != EventPriority.HIGH) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        (listener as EventListener<T>).onEvent(event)
                        totalEventsProcessed++
                    } catch (e: Exception) {
                        System.err.println("Error processing event listener: ${e.message}")
                    }
                }
            }
        }
    }


    inline fun <reified T : Any> subscribe(): SharedFlow<T> {
        val eventType = T::class
        val flow = eventFlows.getOrPut(eventType) {
            MutableSharedFlow<Any>(replay = 0, extraBufferCapacity = 1000)
        }

        @Suppress("UNCHECKED_CAST")
        return flow.asSharedFlow() as SharedFlow<T>
    }


    inline fun <reified T : Any> addEventListener(
        listener: EventListener<T>
    ): EventSubscription {
        val eventType = T::class
        val listeners = eventListeners.getOrPut(eventType) { mutableListOf() }


        val insertIndex = listeners.indexOfFirst { it.priority.ordinal > listener.priority.ordinal }
        if (insertIndex >= 0) {
            listeners.add(insertIndex, listener)
        } else {
            listeners.add(listener)
        }

        return EventSubscription(eventType, listener) {
            removeEventListener<T>(listener)
        }
    }


    inline fun <reified T : Any> removeEventListener(listener: EventListener<T>) {
        val eventType = T::class
        eventListeners[eventType]?.remove(listener)
    }


    fun getMetrics(): EventMetrics {
        return EventMetrics(
            totalEventsEmitted = totalEventsEmitted,
            totalEventsProcessed = totalEventsProcessed,
            activeEventTypes = eventFlows.keys.size,
            totalListeners = eventListeners.values.sumOf { it.size }
        )
    }


    fun dispose() {
        eventFlows.clear()
        eventListeners.clear()
    }
}


interface EventListener<T : Any> {
    val priority: EventPriority get() = EventPriority.NORMAL

    suspend fun onEvent(event: T)
}


enum class EventPriority {
    HIGH,
    NORMAL,
    LOW
}


class EventSubscription(
    val eventType: KClass<*>,
    val listener: EventListener<*>,
    private val unsubscriber: () -> Unit
) {
    fun unsubscribe() = unsubscriber()
}


data class EventMetrics(
    val totalEventsEmitted: Long,
    val totalEventsProcessed: Long,
    val activeEventTypes: Int,
    val totalListeners: Int
)


sealed class UserEvent {
    data class UserConnected(val userId: String, val sessionId: String, val timestamp: Long) : UserEvent()
    data class UserDisconnected(val userId: String, val sessionId: String, val timestamp: Long) : UserEvent()
    data class UserAuthenticated(val userId: String, val username: String, val timestamp: Long) : UserEvent()
    data class UserProfileUpdated(val userId: String, val changes: Map<String, Any>, val timestamp: Long) : UserEvent()
    data class UserPermissionsChanged(val userId: String, val permissions: Set<String>, val timestamp: Long) :
        UserEvent()
}


sealed class GraphEvent {
    data class GraphCreated(val graphId: String, val userId: String, val timestamp: Long) : GraphEvent()
    data class GraphDeleted(val graphId: String, val userId: String, val timestamp: Long) : GraphEvent()
    data class GraphUpdated(val graphId: String, val userId: String, val updateType: String, val timestamp: Long) :
        GraphEvent()

    data class GraphShared(
        val graphId: String,
        val ownerId: String,
        val sharedWithUserId: String,
        val timestamp: Long
    ) : GraphEvent()

    data class GraphCollaborationStarted(val graphId: String, val users: Set<String>, val timestamp: Long) :
        GraphEvent()

    data class NodeAdded(val graphId: String, val nodeId: String, val userId: String, val timestamp: Long) :
        GraphEvent()

    data class NodeUpdated(val graphId: String, val nodeId: String, val userId: String, val timestamp: Long) :
        GraphEvent()

    data class NodeDeleted(val graphId: String, val nodeId: String, val userId: String, val timestamp: Long) :
        GraphEvent()

    data class ConnectionAdded(val graphId: String, val connectionId: String, val userId: String, val timestamp: Long) :
        GraphEvent()

    data class ConnectionDeleted(
        val graphId: String,
        val connectionId: String,
        val userId: String,
        val timestamp: Long
    ) : GraphEvent()
}


sealed class WebSocketEvent {
    data class ConnectionOpened(val sessionId: String, val userId: String?, val timestamp: Long) : WebSocketEvent()
    data class ConnectionClosed(val sessionId: String, val userId: String?, val timestamp: Long) : WebSocketEvent()
    data class MessageReceived(
        val sessionId: String,
        val messageType: String,
        val messageSize: Int,
        val timestamp: Long
    ) : WebSocketEvent()

    data class MessageSent(val sessionId: String, val messageType: String, val messageSize: Int, val timestamp: Long) :
        WebSocketEvent()

    data class ConnectionError(val sessionId: String, val error: String, val timestamp: Long) : WebSocketEvent()
    data class RateLimitExceeded(val sessionId: String, val userId: String?, val timestamp: Long) : WebSocketEvent()
}


sealed class SystemEvent {
    data class SystemStarted(val timestamp: Long) : SystemEvent()
    data class SystemShutdown(val timestamp: Long) : SystemEvent()
    data class SystemError(val error: String, val timestamp: Long) : SystemEvent()
    data class MetricsSnapshot(val metrics: Map<String, Any>, val timestamp: Long) : SystemEvent()
    data class HealthCheckPerformed(val status: String, val timestamp: Long) : SystemEvent()
}