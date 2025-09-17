package com.thedevjade.flow.api.user

import com.thedevjade.flow.api.FlowConfig
import com.thedevjade.flow.api.events.EventManager
import com.thedevjade.flow.api.events.UserEvent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap


class UserManager(
    private val eventManager: EventManager,
    private val config: FlowConfig
) {


    private val users = ConcurrentHashMap<String, FlowUser>()
    private val userSessions = ConcurrentHashMap<String, MutableSet<String>>()
    private val sessionToUser = ConcurrentHashMap<String, String>()
    private val userMutex = Mutex()


    private val authTokens = ConcurrentHashMap<String, AuthToken>()


    suspend fun createUser(
        userId: String,
        username: String,
        email: String? = null,
        permissions: Set<UserPermission> = setOf(UserPermission.READ_GRAPHS, UserPermission.CREATE_GRAPHS)
    ): UserCreationResult = userMutex.withLock {

        if (users.containsKey(userId)) {
            return UserCreationResult.Failure("User already exists")
        }

        if (users.size >= config.maxUsers) {
            return UserCreationResult.Failure("Maximum user limit reached")
        }

        val user = FlowUser(
            id = userId,
            username = username,
            email = email,
            permissions = permissions,
            createdAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis()
        )

        users[userId] = user
        userSessions[userId] = mutableSetOf()

        eventManager.emit(UserEvent.UserConnected(userId, "", System.currentTimeMillis()))

        UserCreationResult.Success(user)
    }


    suspend fun authenticateUser(
        userId: String,
        sessionId: String,
        username: String? = null,
        authToken: String? = null
    ): AuthenticationResult = userMutex.withLock {


        if (authToken != null && !isValidAuthToken(authToken, userId)) {
            eventManager.emit(UserEvent.UserAuthenticated(userId, username ?: "unknown", System.currentTimeMillis()))
            return AuthenticationResult.Failure("Invalid authentication token")
        }


        val user = users[userId] ?: run {
            if (username == null) {
                return AuthenticationResult.Failure("User not found and no username provided")
            }

            val newUser = FlowUser(
                id = userId,
                username = username,
                permissions = setOf(UserPermission.READ_GRAPHS, UserPermission.CREATE_GRAPHS),
                createdAt = System.currentTimeMillis(),
                lastActiveAt = System.currentTimeMillis()
            )
            users[userId] = newUser
            userSessions[userId] = mutableSetOf()
            newUser
        }


        users[userId] = user.copy(lastActiveAt = System.currentTimeMillis())


        userSessions[userId]!!.add(sessionId)
        sessionToUser[sessionId] = userId

        eventManager.emit(UserEvent.UserAuthenticated(userId, user.username, System.currentTimeMillis()))

        AuthenticationResult.Success(user, createAuthToken(userId))
    }


    suspend fun removeSession(sessionId: String) = userMutex.withLock {
        val userId = sessionToUser.remove(sessionId)
        if (userId != null) {
            userSessions[userId]?.remove(sessionId)


            if (userSessions[userId]?.isEmpty() == true) {
                eventManager.emit(UserEvent.UserDisconnected(userId, sessionId, System.currentTimeMillis()))
            }
        }
    }


    fun getUser(userId: String): FlowUser? = users[userId]


    fun getUserBySession(sessionId: String): FlowUser? {
        val userId = sessionToUser[sessionId] ?: return null
        return users[userId]
    }


    fun getUserSessions(userId: String): Set<String> = userSessions[userId]?.toSet() ?: emptySet()


    fun hasPermission(userId: String, permission: UserPermission): Boolean {
        return users[userId]?.permissions?.contains(permission) ?: false
    }

    /**
     * Update user permissions
     */
    suspend fun updatePermissions(userId: String, permissions: Set<UserPermission>): Boolean = userMutex.withLock {
        val user = users[userId] ?: return false
        users[userId] = user.copy(permissions = permissions)
        eventManager.emit(UserEvent.UserPermissionsChanged(userId, permissions.map { it.name }.toSet(), System.currentTimeMillis()))
        true
    }

    /**
     * Update user profile
     */
    suspend fun updateProfile(
        userId: String,
        username: String? = null,
        email: String? = null,
        metadata: Map<String, Any>? = null
    ): Boolean = userMutex.withLock {
        val user = users[userId] ?: return false

        val changes = mutableMapOf<String, Any>()
        val updatedUser = user.copy(
            username = username ?: user.username,
            email = email ?: user.email,
            metadata = metadata ?: user.metadata
        )

        if (username != null && username != user.username) changes["username"] = username
        if (email != null && email != user.email) changes["email"] = email
        if (metadata != null && metadata != user.metadata) changes["metadata"] = metadata

        users[userId] = updatedUser

        if (changes.isNotEmpty()) {
            eventManager.emit(UserEvent.UserProfileUpdated(userId, changes, System.currentTimeMillis()))
        }

        true
    }

    /**
     * Get all users (with pagination)
     */
    fun getAllUsers(offset: Int = 0, limit: Int = 100): List<FlowUser> {
        return users.values.drop(offset).take(limit)
    }

    /**
     * Get active users (users with at least one session)
     */
    fun getActiveUsers(): List<FlowUser> {
        return userSessions.entries
            .filter { it.value.isNotEmpty() }
            .mapNotNull { users[it.key] }
    }

    /**
     * Get active user count
     */
    fun getActiveUserCount(): Int = userSessions.values.count { it.isNotEmpty() }

    /**
     * Search users by username
     */
    fun searchUsers(query: String, limit: Int = 20): List<FlowUser> {
        return users.values
            .filter { it.username.contains(query, ignoreCase = true) }
            .take(limit)
    }

    /**
     * Delete a user (and all their sessions)
     */
    suspend fun deleteUser(userId: String): Boolean = userMutex.withLock {
        val user = users.remove(userId) ?: return false

        // Remove all sessions
        userSessions[userId]?.forEach { sessionId ->
            sessionToUser.remove(sessionId)
        }
        userSessions.remove(userId)

        // Remove auth tokens
        authTokens.entries.removeAll { it.value.userId == userId }

        eventManager.emit(UserEvent.UserDisconnected(userId, "", System.currentTimeMillis()))

        true
    }

    /**
     * Create an authentication token
     */
    private fun createAuthToken(userId: String): String {
        val token = "token_${userId}_${System.currentTimeMillis()}_${(1000..9999).random()}"
        authTokens[token] = AuthToken(
            token = token,
            userId = userId,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 hours
        )
        return token
    }

    /**
     * Validate an authentication token
     */
    private fun isValidAuthToken(token: String, expectedUserId: String): Boolean {
        val authToken = authTokens[token] ?: return false
        return authToken.userId == expectedUserId &&
               authToken.expiresAt > System.currentTimeMillis()
    }

    /**
     * Clean up expired tokens
     */
    fun cleanupExpiredTokens() {
        val now = System.currentTimeMillis()
        authTokens.entries.removeAll { it.value.expiresAt <= now }
    }

    /**
     * Get user statistics
     */
    fun getUserStatistics(): UserStatistics {
        return UserStatistics(
            totalUsers = users.size,
            activeUsers = getActiveUserCount(),
            totalSessions = sessionToUser.size,
            activeTokens = authTokens.size
        )
    }

    /**
     * Dispose the user manager
     */
    fun dispose() {
        users.clear()
        userSessions.clear()
        sessionToUser.clear()
        authTokens.clear()
    }
}

/**
 * Flow user data model
 */
data class FlowUser(
    val id: String,
    val username: String,
    val email: String? = null,
    val permissions: Set<UserPermission> = emptySet(),
    val metadata: Map<String, Any> = emptyMap(),
    val createdAt: Long,
    val lastActiveAt: Long
)

/**
 * User permissions
 */
enum class UserPermission {
    READ_GRAPHS,
    CREATE_GRAPHS,
    EDIT_GRAPHS,
    DELETE_GRAPHS,
    SHARE_GRAPHS,
    MANAGE_USERS,
    SYSTEM_ADMIN,
    VIEW_ANALYTICS
}

/**
 * Authentication token
 */
data class AuthToken(
    val token: String,
    val userId: String,
    val createdAt: Long,
    val expiresAt: Long
)

/**
 * User creation result
 */
sealed class UserCreationResult {
    data class Success(val user: FlowUser) : UserCreationResult()
    data class Failure(val error: String) : UserCreationResult()
}

/**
 * Authentication result
 */
sealed class AuthenticationResult {
    data class Success(val user: FlowUser, val token: String) : AuthenticationResult()
    data class Failure(val error: String) : AuthenticationResult()
}

/**
 * User statistics
 */
data class UserStatistics(
    val totalUsers: Int,
    val activeUsers: Int,
    val totalSessions: Int,
    val activeTokens: Int
)