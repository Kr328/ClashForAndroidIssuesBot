package com.github.kr328.bot.action

import com.github.kr328.bot.model.None

class DeleteIssues(
    private val issueId: String
) : Action<None>(None.serializer()) {
    override fun query(): String {
        return """
            mutation {
              deleteIssue(input: {issueId: "$issueId"}) {
                clientMutationId
              }
            }
            """.trimIndent()
    }
}