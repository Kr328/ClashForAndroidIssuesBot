package com.github.kr328.bot

import com.github.kr328.bot.action.DeleteIssues
import com.github.kr328.bot.action.QueryIssues
import com.github.kr328.bot.model.QueryIssuesResult
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter

object IssueDeleter {
    private val logger = LoggerFactory.getLogger(IssueDeleter::class.java)

    suspend fun exec() {
        while (true) {
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

                    issues.addAll(r.data.user.repository.issues.nodes)

                    hasNext = r.data.user.repository.issues.pageInfo.hasNextPage
                    endCursor = r.data.user.repository.issues.pageInfo.endCursor
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

            delay(Duration.ofDays(1))
        }
    }
}