package com.github.kr328.bot

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    routing {
        post("/") {
            when (call.request.headers["X-GITHUB-EVENT"]?.toLowerCase()) {
                "issues" -> {
                    try {
                        IssuesHandler.handle(call)
                    }
                    catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "process failure")
                    }
                }
                "integration_installation", "installation", "ping" -> {
                    call.respond(HttpStatusCode.NoContent, "")
                }
                else -> call.respond(HttpStatusCode.NotFound, "")
            }
        }
    }
}

