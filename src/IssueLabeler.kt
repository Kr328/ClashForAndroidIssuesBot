package com.github.kr328.bot

import action.QueryLabel
import com.github.kr328.bot.action.LabelIssue
import com.github.kr328.bot.action.UnLabelIssue

object IssueLabeler {
    private val REGEX_ISSUE_TITLE = Regex("\\[(BUG|Feature Request)].*\\S.*")

    suspend fun handleOpen(issueId: String, title: String) {
        if (!REGEX_ISSUE_TITLE.matches(title)) {
            val invalidId = QueryLabel(Constants.REPOSITORY_OWNER, Constants.REPOSITORY_NAME, "invalid")
                .action().data.repository.label.id
            LabelIssue(issueId, invalidId).action()
        }
    }

    suspend fun handleReopen(issueId: String, title: String) {
        val invalidId = QueryLabel(Constants.REPOSITORY_OWNER, Constants.REPOSITORY_NAME, "invalid")
            .action().data.repository.label.id

        UnLabelIssue(issueId, invalidId).action()

        if (!REGEX_ISSUE_TITLE.matches(title)) {
            LabelIssue(issueId, invalidId).action()
        }
    }
}