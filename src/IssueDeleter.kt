package com.github.kr328.bot

import com.github.kr328.bot.action.DeleteIssues
import com.github.kr328.bot.action.QueryIssues
import com.github.kr328.bot.model.QueryIssuesResult
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.timer

object IssueDeleter {
    private val logger = LoggerFactory.getLogger(IssueDeleter::class.java)
    private val timerChannel = Channel<Unit>(Channel.CONFLATED)
    private val timer = timer(daemon = true, period = Duration.ofHours(1).toMillis(), startAt = Date()) {
        timerChannel.offer(Unit)
    }

    suspend fun exec() {
        try {
            while (true) {
                timerChannel.receive()

                try {
                    var hasNext = true
                    var endCursor: String? = null
                    val issues = mutableListOf<QueryIssuesResult.Issue>()

                    while (hasNext) {
                        val r = QueryIssues(
                            Constants.REPOSITORY_OWNER,
                            Constants.REPOSITORY_NAME,
                            listOf("invalid"),
                            endCursor
                        ).action()

                        val i = r.data.user.repository.issues

                        issues.addAll(i.nodes)

                        hasNext = i.pageInfo.hasNextPage
                        endCursor = i.pageInfo.endCursor
                    }

                    issues.filter {
                        Duration.between(
                            Instant.now(),
                            Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(it.createdAt))
                        ).abs().toDays() >= 5
                    }.map {
                        logger.info("Delete ${it.title}")

                        it.id
                    }.forEach {
                        DeleteIssues(it).action()
                    }
                } catch (e: Exception) {
                    logger.warn("Auto delete issues failure", e)
                }
            }
        } finally {
            timer.cancel()
        }
    }
}