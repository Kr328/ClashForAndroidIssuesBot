package com.github.kr328.bot

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.IssuePayload
import org.slf4j.LoggerFactory

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    val logger = LoggerFactory.getLogger("Main")

    routing {
        post("/") {
            when (call.request.headers["X-GITHUB-EVENT"]?.toLowerCase()) {
                "issues" -> {
                    try {
                        if (call.request.contentType() != ContentType.Application.Json)
                            return@post call.respond(HttpStatusCode.NotAcceptable, "Only json accepted")

                        val content = call.receive<ByteArray>()
                        if (!IssueUtils.validContent(call.request, content))
                            return@post call.respond(HttpStatusCode.BadRequest, "Invalid secret")

                        val payload = withContext(Dispatchers.Default) {
                            Shared.JSON.decodeFromString(IssuePayload.serializer(), String(content))
                        }

                        when (payload.action) {
                            "opened" -> {
                                IssueLabeler.handleOpen(payload.issue.nodeId, payload.issue.title)
                            }
                            "reopened" -> {
                                IssueLabeler.handleReopen(payload.issue.nodeId, payload.issue.title)
                            }
                            "labeled" -> {
                                if (payload.issue.state == "open") {
                                    IssueCloser.handleLabeled(
                                        payload.issue.nodeId,
                                        payload.label?.name
                                            ?: return@post call.respond(
                                                HttpStatusCode.BadRequest,
                                                "Labeled label not found"
                                            )
                                    )
                                }
                            }
                        }

                        call.respond(HttpStatusCode.NoContent, "")
                    } catch (e: Exception) {
                        logger.warn("Process webhook failure", e)

                        call.respond(HttpStatusCode.BadRequest, "process failure")
                    }
                }
                "ping" -> {
                    call.respond(HttpStatusCode.NoContent, "")
                }
                else -> call.respond(HttpStatusCode.NotFound, "")
            }
        }
    }

    launch {
        IssueDeleter.exec()
    }
}

