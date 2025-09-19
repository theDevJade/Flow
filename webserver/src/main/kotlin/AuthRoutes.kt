package com.thedevjade.flow.webserver

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.json.*

fun Application.configureAuthRoutes(authService: AuthService) {
    routing {
        route("/auth") {
            post("/login") {
                try {
                    val loginRequest = call.receive<LoginRequest>()
                    val loginResponse = authService.authenticate(loginRequest.username, loginRequest.password)

                    if (loginResponse.success) {
                        call.respond(HttpStatusCode.OK, loginResponse)
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, loginResponse)
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, LoginResponse(
                        success = false,
                        message = "Invalid request format: ${e.message}"
                    ))
                }
            }

            post("/logout") {
                val token = call.request.header("Authorization")?.removePrefix("Bearer ")
                if (token != null) {
                    authService.revokeToken(token)
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out successfully"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "No token provided"))
                }
            }

            get("/validate") {
                val token = call.request.header("Authorization")?.removePrefix("Bearer ")
                if (token == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("valid" to false, "message" to "No token provided"))
                    return@get
                }

                val authToken = authService.validateToken(token)
                if (authToken != null) {
                    call.respond(HttpStatusCode.OK, mapOf(
                        "valid" to true,
                        "username" to authToken.username,
                        "userId" to authToken.userId,
                        "expiresAt" to authToken.expiresAt
                    ))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("valid" to false, "message" to "Invalid or expired token"))
                }
            }

            get("/stats") {
                call.respond(HttpStatusCode.OK, mapOf(
                    "activeTokens" to authService.getActiveTokensCount(),
                    "activeUsers" to authService.getActiveUsersCount(),
                    "timestamp" to System.currentTimeMillis()
                ))
            }
        }
    }
}


val authServiceKey = AttributeKey<AuthService>("authService")


fun Application.getAuthService(): AuthService {
    return attributes[authServiceKey]
}


suspend fun ApplicationCall.getAuthenticatedUser(authService: AuthService): AuthToken? {
    val token = request.header("Authorization")?.removePrefix("Bearer ")
    if (token == null) {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "No authentication token provided"))
        return null
    }

    val authToken = authService.validateToken(token)
    if (authToken == null) {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or expired token"))
        return null
    }

    return authToken
}