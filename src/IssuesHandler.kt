package com.github.kr328.bot

import com.github.kr328.bot.action.Action
import com.github.kr328.bot.action.CloseAction
import com.github.kr328.bot.action.CommentAction
import com.github.kr328.bot.action.LabelAction
import io.ktor.application.ApplicationCall
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.request.ApplicationRequest
import io.ktor.request.contentType
import io.ktor.request.receive
import io.ktor.response.respond
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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
    private val REGEX_ISSUE_TITLE = Regex("\\[(BUG|Feature Request)].*\\S.*")
    private val COMMENT_INVALID_ISSUE = """
        Please use Issue Template to create issue
        请务必使用 Issue Template 创建 Issue
        
        [Issue Template](https://github.com/Kr328/ClashForAndroid/issues/new/choose)
    """.trimIndent()
    private val COMMENT_OUT_OF_DATE_ISSUE = """
        This issue has no updated feedback and may have been fixed in latest release
        此 Issue 没有后续且可能已经在最新版本中得到修复
    """.trimIndent()
    private val COMMENT_RESOLVED_ISSUE = """
        This issue has been resolved
        此问题已得到解决
    """.trimIndent()

    private val pendingActions = Channel<Pair<String, Action>>(Channel.UNLIMITED)

    init {
        val client = HttpClient(Apache) {
            expectSuccess = false

            this.defaultRequest {
                headers["Authorization"] = "Bearer $API_SECRET"

                contentType(ContentType.Application.Json)
            }
        }

        GlobalScope.launch {
            while (isActive) {
                val action = pendingActions.receive()

                launch {
                    action.second.action(client, Url(action.first))
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
            "opened" -> {
                if (!REGEX_ISSUE_TITLE.matches(payload.issue.title.trim()))
                    pendingActions.send(payload.issue.url to LabelAction("invalid"))
            }
            "reopened" -> {
                if (!REGEX_ISSUE_TITLE.matches(payload.issue.title.trim())) {
                    if (payload.issue.labels.any { it.name == "invalid"}) {
                        pendingActions.send(payload.issue.url to CommentAction(COMMENT_INVALID_ISSUE))
                        pendingActions.send(payload.issue.url to CloseAction())
                    }
                    else {
                        pendingActions.send(payload.issue.url to LabelAction("invalid"))
                    }
                }
            }
            "labeled" -> {
                if (payload.issue.state != "open" || payload.issue.locked)
                    return

                val url = payload.issue.url
                val labels = payload.issue.labels.map(Label::name).toSet()

                when {
                    "invalid" in labels -> {
                        pendingActions.send(url to CommentAction(COMMENT_INVALID_ISSUE))
                        pendingActions.send(url to CloseAction())
                    }
                    "out-of-date" in labels -> {
                        pendingActions.send(url to CommentAction(COMMENT_OUT_OF_DATE_ISSUE))
                        pendingActions.send(url to CloseAction())
                    }
                    "resolved" in labels -> {
                        pendingActions.send(url to CommentAction(COMMENT_RESOLVED_ISSUE))
                        pendingActions.send(url to CloseAction())
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