package com.thedevjade.flow.webserver

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.webjars.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

fun Application.configureRouting() {
    install(Webjars) {
        path = "/webjars"
    }
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        get("/webjars") {
            call.respondText("<script src='/webjars/jquery/jquery.js'></script>", ContentType.Text.Html)
        }
    }
}
