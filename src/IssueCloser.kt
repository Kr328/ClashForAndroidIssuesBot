package com.github.kr328.bot

import com.github.kr328.bot.action.CommentClose

object IssueCloser {
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

    suspend fun handleLabeled(issueId: String, newLabel: String) {
        when (newLabel) {
            "invalid" ->
                CommentClose(issueId, COMMENT_INVALID_ISSUE).action()
            "out-of-date" ->
                CommentClose(issueId, COMMENT_OUT_OF_DATE_ISSUE).action()
            "resolved" ->
                CommentClose(issueId, COMMENT_RESOLVED_ISSUE).action()
        }
    }
}