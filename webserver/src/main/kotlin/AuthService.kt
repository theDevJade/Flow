package com.thedevjade.flow.webserver

import com.thedevjade.flow.common.models.FlowLogger
import com.thedevjade.flow.webserver.database.DatabaseManager
import com.thedevjade.flow.webserver.database.UsersTable
import com.thedevjade.flow.webserver.database.AuthTokensTable
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Serializable
data class User(
    val id: String,
    val username: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = 0
)

@Serializable
data class AuthToken(
    val token: String,
    val userId: String,
    val username: String,
    val issuedAt: Long,
    val expiresAt: Long
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val token: String? = null,
    val username: String? = null,
    val userId: String? = null,
    val message: String? = null,
    val expiresAt: Long? = null
)

class AuthService {

    private val tokenExpiryTime = 24 * 60 * 60 * 1000L

    fun createUser(username: String, password: String): User? {
        return transaction(DatabaseManager.getDatabase()) {

            val existingUser = UsersTable.selectAll().where { UsersTable.username eq username }.singleOrNull()
            if (existingUser != null) {
                return@transaction null
            }

            val userId = UUID.randomUUID().toString()
            val now = Instant.now()

            UsersTable.insert {
                it[UsersTable.username] = username
                it[passwordHash] = hashPassword(password)
                it[createdAt] = now
                it[updatedAt] = now
            }

            val user = User(
                id = userId,
                username = username,
                passwordHash = hashPassword(password)
            )

            debugLog("Created user: $username with ID: $userId")
            return@transaction user
        }
    }

    fun authenticate(username: String, password: String): LoginResponse {
        debugLog("Authentication attempt for username: $username")

        return transaction(DatabaseManager.getDatabase()) {
            val userRow = UsersTable.selectAll().where { UsersTable.username eq username }.singleOrNull()
            if (userRow == null) {
                debugLog("Authentication failed: User not found - $username")
                return@transaction LoginResponse(
                    success = false,
                    message = "Invalid username or password"
                )
            }

            val passwordHash = hashPassword(password)
            if (userRow[UsersTable.passwordHash] != passwordHash) {
                debugLog("Authentication failed: Invalid password for user - $username")
                return@transaction LoginResponse(
                    success = false,
                    message = "Invalid username or password"
                )
            }


            val token = generateToken()
            val expiresAt = Instant.now().plus(1, ChronoUnit.DAYS)
            val userId = userRow[UsersTable.id].value.toString()


            AuthTokensTable.deleteWhere { AuthTokensTable.userId eq userRow[UsersTable.id] }


            AuthTokensTable.insert {
                it[AuthTokensTable.token] = token
                it[AuthTokensTable.userId] = userRow[UsersTable.id]
                it[AuthTokensTable.expiresAt] = expiresAt
            }

            debugLog("Authentication successful for user: $username")
            return@transaction LoginResponse(
                success = true,
                token = token,
                username = username,
                userId = userId,
                expiresAt = expiresAt.toEpochMilli()
            )
        }
    }

    fun validateToken(token: String): AuthToken? {
        return transaction(DatabaseManager.getDatabase()) {
            val tokenRow = (AuthTokensTable innerJoin UsersTable)
                .selectAll()
                .where { AuthTokensTable.token eq token }
                .singleOrNull()

            if (tokenRow == null) {
                return@transaction null
            }

            val expiresAt = tokenRow[AuthTokensTable.expiresAt]
            if (Instant.now().isAfter(expiresAt)) {

                AuthTokensTable.deleteWhere { AuthTokensTable.token eq token }
                debugLog("Token expired and removed: $token")
                return@transaction null
            }

            return@transaction AuthToken(
                token = token,
                userId = tokenRow[UsersTable.id].value.toString(),
                username = tokenRow[UsersTable.username],
                issuedAt = tokenRow[AuthTokensTable.createdAt].toEpochMilli(),
                expiresAt = expiresAt.toEpochMilli()
            )
        }
    }

    fun getUserById(userId: String): User? {
        return transaction(DatabaseManager.getDatabase()) {
            val userRow = UsersTable.selectAll().where { UsersTable.id eq userId.toInt() }.singleOrNull()
            userRow?.let {
                User(
                    id = it[UsersTable.id].value.toString(),
                    username = it[UsersTable.username],
                    passwordHash = it[UsersTable.passwordHash],
                    createdAt = it[UsersTable.createdAt].toEpochMilli(),
                    lastLoginAt = 0
                )
            }
        }
    }

    fun getUserByUsername(username: String): User? {
        return transaction(DatabaseManager.getDatabase()) {
            val userRow = UsersTable.selectAll().where { UsersTable.username eq username }.singleOrNull()
            userRow?.let {
                User(
                    id = it[UsersTable.id].value.toString(),
                    username = it[UsersTable.username],
                    passwordHash = it[UsersTable.passwordHash],
                    createdAt = it[UsersTable.createdAt].toEpochMilli()
                )
            }
        }
    }

    fun revokeToken(token: String): Boolean {
        return transaction(DatabaseManager.getDatabase()) {
            val deleted = AuthTokensTable.deleteWhere { AuthTokensTable.token eq token }
            if (deleted > 0) {
                debugLog("Token revoked: $token")
                return@transaction true
            }
            return@transaction false
        }
    }

    fun getActiveTokensCount(): Int {
        return transaction(DatabaseManager.getDatabase()) {
            cleanExpiredTokens()
            AuthTokensTable.selectAll().count().toInt()
        }
    }

    fun getActiveUsersCount(): Int {
        return transaction(DatabaseManager.getDatabase()) {
            cleanExpiredTokens()
            AuthTokensTable.selectAll().distinctBy { it[AuthTokensTable.userId] }.count()
        }
    }

    private fun cleanExpiredTokens() {
        transaction(DatabaseManager.getDatabase()) {
            val now = Instant.now()
            val expiredCount = AuthTokensTable.deleteWhere { AuthTokensTable.expiresAt less now }
            if (expiredCount > 0) {
                debugLog("Cleaned $expiredCount expired tokens")
            }
        }
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest((password + "salt_flow_2024").toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateToken(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }


    private fun debugLog(message: String) {
        val timestamp = java.time.Instant.now().toString()
        FlowLogger.debug("[$timestamp] AUTH-DEBUG: $message")
    }
}