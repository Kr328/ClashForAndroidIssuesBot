package com.github.kr328.bot

import com.github.kr328.bot.model.AddLabel
import com.github.kr328.bot.model.CloseIssue
import com.github.kr328.bot.model.CreateComment
import io.ktor.application.ApplicationCall
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.request.*
import io.ktor.response.respond
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import model.IssuePayload
import org.apache.commons.codec.binary.Hex
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object IssuesHandler {
    private val WEBHOOK_SECRET = System.getenv("WEBHOOK_SECRET") ?: throw Error("webhook secret not set")
    private val API_SECRET = System.getenv("API_SECRET") ?: throw Error("app id not set")
    private val REGEX_ISSUE_TITLE = Regex("\\[(BUG|Feature Request)].+")
    private val INVALID_ISSUE_COMMENT = """
        Please use Issue Template to create issue
        请务必使用 Issue Template 创建 Issue
        
        [Issue Template](https://github.com/Kr328/ClashForAndroid/issues/new/choose)
    """.trimIndent()

    private val pendingCloseIssues = Channel<String>(Channel.UNLIMITED)
    private val pendingLabelIssues = Channel<String>(Channel.UNLIMITED)

    init {
        val client = HttpClient(Apache) {
            expectSuccess = false
        }
        val json = Json(JsonConfiguration.Stable)

        GlobalScope.launch {
            while (isActive) {
                val issueUrl = pendingLabelIssues.receive()

                client.post<String>("$issueUrl/labels") {
                    headers["Authorization"] = "Bearer $API_SECRET"

                    contentType(ContentType.Application.Json)

                    body = json.stringify(AddLabel.serializer(), AddLabel(listOf("invalid")))
                }
            }
        }
        GlobalScope.launch {
            while (isActive) {
                val issueUrl = pendingCloseIssues.receive()

                client.post<String>("$issueUrl/comments") {
                    headers["Authorization"] = "Bearer $API_SECRET"

                    contentType(ContentType.Application.Json)

                    body = json.stringify(CreateComment.serializer(), CreateComment(INVALID_ISSUE_COMMENT))
                }

                client.patch<String>(issueUrl) {
                    headers["Authorization"] = "Bearer $API_SECRET"

                    contentType(ContentType.Application.Json)

                    body = json.stringify(CloseIssue.serializer(), CloseIssue())
                }
            }
        }
    }

    suspend fun handle(call: ApplicationCall) {
        if (call.request.contentType() != ContentType.Application.Json)
            return call.respond(HttpStatusCode.NotAcceptable, "Only json accepted")

        val content = call.receive<ByteArray>()
        if (!validContent(call.request, content))
            return call.respond(HttpStatusCode.BadRequest, "Invalid secret")

        val payload = withContext(Dispatchers.Default) {
            Json(JsonConfiguration.Stable.copy(ignoreUnknownKeys = true))
                .parse(IssuePayload.serializer(), String(content))
        }

        when (payload.action) {
            "opened", "reopened", "labeled" -> {
                if (payload.issue.labels.any { it.name == "invalid" }) {
                    if (payload.issue.state == "open" && !payload.issue.locked) {
                        pendingCloseIssues.send(payload.issue.url)
                    }
                }
                else {
                    if (!REGEX_ISSUE_TITLE.matches(payload.issue.title.trim()))
                        pendingLabelIssues.send(payload.issue.url)
                }
            }
        }

        call.respond(HttpStatusCode.NoContent, "")
    }

    private suspend fun validContent(request: ApplicationRequest, content: ByteArray): Boolean {
        return withContext(Dispatchers.Default) {
            val key = SecretKeySpec(WEBHOOK_SECRET.toByteArray(), "HmacSHA1")
            val mac = Mac.getInstance("HmacSHA1")

            mac.init(key)

            val digest = String(Hex().encode(mac.doFinal(content)))

            "sha1=$digest".trim() == request.headers["X-HUB-SIGNATURE"]?.trim()
        }
    }
}