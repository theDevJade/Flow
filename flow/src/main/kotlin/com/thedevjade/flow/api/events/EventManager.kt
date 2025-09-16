package com.thedevjade.flow.api.events

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Comprehensive event management system for the Flow application.
 * Provides type-safe event emission and subscription with support for
 * filtering, error handling, and async processing.
 */
class EventManager(private val scope: CoroutineScope) {

    // Event flows for different event types
    val eventFlows = ConcurrentHashMap<KClass<*>, MutableSharedFlow<Any>>()

    // Event listeners with priority support
    val eventListeners = ConcurrentHashMap<KClass<*>, MutableList<EventListener<*>>>()

    // Metrics
    private var totalEventsEmitted = 0L
    private var totalEventsProcessed = 0L

    /**
     * Emit an event of the specified type
     */
    suspend fun <T : Any> emit(event: T) {
        val eventType = event::class
        totalEventsEmitted++

        // Get or create flow for this event type
        val flow = eventFlows.getOrPut(eventType) {
            MutableSharedFlow<Any>(replay = 0, extraBufferCapacity = 1000)
        }

        // Emit the event
        flow.tryEmit(event)

        // Process listeners synchronously for high-priority listeners
        eventListeners[eventType]?.forEach { listener ->
            if (listener.priority == EventPriority.HIGH) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    (listener as EventListener<T>).onEvent(event)
                    totalEventsProcessed++
                } catch (e: Exception) {
                    // Log error but don't fail the entire event chain
                    System.err.println("Error processing high-priority event listener: ${e.message}")
                }
            }
        }

        // Process normal and low priority listeners asynchronously
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

    /**
     * Subscribe to events of a specific type
     */
    inline fun <reified T : Any> subscribe(): SharedFlow<T> {
        val eventType = T::class
        val flow = eventFlows.getOrPut(eventType) {
            MutableSharedFlow<Any>(replay = 0, extraBufferCapacity = 1000)
        }

        @Suppress("UNCHECKED_CAST")
        return flow.asSharedFlow() as SharedFlow<T>
    }

    /**
     * Register an event listener
     */
    inline fun <reified T : Any> addEventListener(
        listener: EventListener<T>
    ): EventSubscription {
        val eventType = T::class
        val listeners = eventListeners.getOrPut(eventType) { mutableListOf() }

        // Insert listener based on priority (high priority first)
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

    /**
     * Remove an event listener
     */
    inline fun <reified T : Any> removeEventListener(listener: EventListener<T>) {
        val eventType = T::class
        eventListeners[eventType]?.remove(listener)
    }

    /**
     * Get event metrics
     */
    fun getMetrics(): EventMetrics {
        return EventMetrics(
            totalEventsEmitted = totalEventsEmitted,
            totalEventsProcessed = totalEventsProcessed,
            activeEventTypes = eventFlows.keys.size,
            totalListeners = eventListeners.values.sumOf { it.size }
        )
    }

    /**
     * Dispose the event manager
     */
    fun dispose() {
        eventFlows.clear()
        eventListeners.clear()
    }
}

/**
 * Event listener interface with priority support
 */
interface EventListener<T : Any> {
    val priority: EventPriority get() = EventPriority.NORMAL

    suspend fun onEvent(event: T)
}

/**
 * Event priority levels
 */
enum class EventPriority {
    HIGH,    // Processed synchronously
    NORMAL,  // Processed asynchronously
    LOW      // Processed asynchronously with lower priority
}

/**
 * Event subscription handle
 */
class EventSubscription(
    val eventType: KClass<*>,
    val listener: EventListener<*>,
    private val unsubscriber: () -> Unit
) {
    fun unsubscribe() = unsubscriber()
}

/**
 * Event metrics
 */
data class EventMetrics(
    val totalEventsEmitted: Long,
    val totalEventsProcessed: Long,
    val activeEventTypes: Int,
    val totalListeners: Int
)

// Common event types for the Flow system

/**
 * User-related events
 */
sealed class UserEvent {
    data class UserConnected(val userId: String, val sessionId: String, val timestamp: Long) : UserEvent()
    data class UserDisconnected(val userId: String, val sessionId: String, val timestamp: Long) : UserEvent()
    data class UserAuthenticated(val userId: String, val username: String, val timestamp: Long) : UserEvent()
    data class UserProfileUpdated(val userId: String, val changes: Map<String, Any>, val timestamp: Long) : UserEvent()
    data class UserPermissionsChanged(val userId: String, val permissions: Set<String>, val timestamp: Long) : UserEvent()
}

/**
 * Graph-related events
 */
sealed class GraphEvent {
    data class GraphCreated(val graphId: String, val userId: String, val timestamp: Long) : GraphEvent()
    data class GraphDeleted(val graphId: String, val userId: String, val timestamp: Long) : GraphEvent()
    data class GraphUpdated(val graphId: String, val userId: String, val updateType: String, val timestamp: Long) : GraphEvent()
    data class GraphShared(val graphId: String, val ownerId: String, val sharedWithUserId: String, val timestamp: Long) : GraphEvent()
    data class GraphCollaborationStarted(val graphId: String, val users: Set<String>, val timestamp: Long) : GraphEvent()
    data class NodeAdded(val graphId: String, val nodeId: String, val userId: String, val timestamp: Long) : GraphEvent()
    data class NodeUpdated(val graphId: String, val nodeId: String, val userId: String, val timestamp: Long) : GraphEvent()
    data class NodeDeleted(val graphId: String, val nodeId: String, val userId: String, val timestamp: Long) : GraphEvent()
    data class ConnectionAdded(val graphId: String, val connectionId: String, val userId: String, val timestamp: Long) : GraphEvent()
    data class ConnectionDeleted(val graphId: String, val connectionId: String, val userId: String, val timestamp: Long) : GraphEvent()
}

/**
 * WebSocket-related events
 */
sealed class WebSocketEvent {
    data class ConnectionOpened(val sessionId: String, val userId: String?, val timestamp: Long) : WebSocketEvent()
    data class ConnectionClosed(val sessionId: String, val userId: String?, val timestamp: Long) : WebSocketEvent()
    data class MessageReceived(val sessionId: String, val messageType: String, val messageSize: Int, val timestamp: Long) : WebSocketEvent()
    data class MessageSent(val sessionId: String, val messageType: String, val messageSize: Int, val timestamp: Long) : WebSocketEvent()
    data class ConnectionError(val sessionId: String, val error: String, val timestamp: Long) : WebSocketEvent()
    data class RateLimitExceeded(val sessionId: String, val userId: String?, val timestamp: Long) : WebSocketEvent()
}

/**
 * System-related events
 */
sealed class SystemEvent {
    data class SystemStarted(val timestamp: Long) : SystemEvent()
    data class SystemShutdown(val timestamp: Long) : SystemEvent()
    data class SystemError(val error: String, val timestamp: Long) : SystemEvent()
    data class MetricsSnapshot(val metrics: Map<String, Any>, val timestamp: Long) : SystemEvent()
    data class HealthCheckPerformed(val status: String, val timestamp: Long) : SystemEvent()
}