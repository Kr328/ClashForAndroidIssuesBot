package com.github.kr328.bot

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.features.*
import java.lang.Exception

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
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

