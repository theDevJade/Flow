package flow.api.implementation

import flow.api.SessionManager
import flow.api.UserSession
import java.util.concurrent.ConcurrentHashMap

class SessionManagerImpl : SessionManager {
    private val sessions = ConcurrentHashMap<String, UserSession>()

    override fun getActiveSessions(): List<UserSession> {
        return sessions.values.toList()
    }

    override fun getSession(userId: String): UserSession? {
        return sessions[userId]
    }

    override fun startSession(userId: String): UserSession {
        val newSession = UserSession(userId, System.currentTimeMillis())
        sessions[userId] = newSession
        return newSession
    }

    override fun endSession(userId: String) {
        sessions.remove(userId)
    }
}
