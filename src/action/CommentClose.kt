package com.github.kr328.bot.action

import com.github.kr328.bot.model.None

class CommentClose(
    private val issueId: String,
    private val body: String
) : Action<None>(None.serializer()) {
    override fun query(): String {
        return """
            mutation { 
              addComment(input: {subjectId: "$issueId", body: "$body"}) {
                clientMutationId
              }
              closeIssue(input: {issueId: "$issueId"}) {
                clientMutationId
              }
            }
        """.trimIndent()
    }
}