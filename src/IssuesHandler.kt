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
import kotlinx.coroutines.selects.select
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import model.IssuePayload
import model.Label
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
    private val OUT_OF_DATE_ISSUE_COMMENT = """
        This issue has no updated feedback and may have been fixed in latest release
        此 Issue 没有后续且可能已经在最新版本中得到修复
    """.trimIndent()

    private val pendingCloseInvalidIssues = Channel<String>(Channel.UNLIMITED)
    private val pendingCloseOutOfDateIssues = Channel<String>(Channel.UNLIMITED)
    private val pendingLabelIssues = Channel<String>(Channel.UNLIMITED)

    init {
        val client = HttpClient(Apache) {
            expectSuccess = false
        }
        val json = Json(JsonConfiguration.Stable)

        GlobalScope.launch {
            while (isActive) {
                select<Unit> {
                    pendingLabelIssues.onReceive {
                        client.post<String>("$it/labels") {
                            headers["Authorization"] = "Bearer $API_SECRET"

                            contentType(ContentType.Application.Json)

                            body = json.stringify(AddLabel.serializer(), AddLabel(listOf("invalid")))
                        }
                    }
                    pendingCloseInvalidIssues.onReceive {
                        client.post<String>("$it/comments") {
                            headers["Authorization"] = "Bearer $API_SECRET"

                            contentType(ContentType.Application.Json)

                            body = json.stringify(CreateComment.serializer(), CreateComment(INVALID_ISSUE_COMMENT))
                        }

                        client.patch<String>(it) {
                            headers["Authorization"] = "Bearer $API_SECRET"

                            contentType(ContentType.Application.Json)

                            body = json.stringify(CloseIssue.serializer(), CloseIssue())
                        }
                    }
                    pendingCloseOutOfDateIssues.onReceive {
                        client.post<String>("$it/comments") {
                            headers["Authorization"] = "Bearer $API_SECRET"

                            contentType(ContentType.Application.Json)

                            body = json.stringify(CreateComment.serializer(), CreateComment(OUT_OF_DATE_ISSUE_COMMENT))
                        }

                        client.patch<String>(it) {
                            headers["Authorization"] = "Bearer $API_SECRET"

                            contentType(ContentType.Application.Json)

                            body = json.stringify(CloseIssue.serializer(), CloseIssue())
                        }
                    }
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
                val labels = payload.issue.labels.map(Label::name).toSet()

                when {
                    "invalid" in labels -> {
                        if (payload.issue.state == "open" && !payload.issue.locked) {
                            pendingCloseInvalidIssues.send(payload.issue.url)
                        }
                    }
                    "out-of-date" in labels -> {
                        if (payload.issue.state == "open" && !payload.issue.locked) {
                            pendingCloseOutOfDateIssues.send(payload.issue.url)
                        }
                    }
                    else -> {
                        if (!REGEX_ISSUE_TITLE.matches(payload.issue.title.trim()))
                            pendingLabelIssues.send(payload.issue.url)
                    }
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