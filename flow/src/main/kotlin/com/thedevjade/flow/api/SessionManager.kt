package flow.api

interface SessionManager {
    fun getActiveSessions(): List<UserSession>
    fun getSession(userId: String): UserSession?
    fun startSession(userId: String): UserSession
    fun endSession(userId: String)
}

data class UserSession(val userId: String, val connectionTime: Long)
