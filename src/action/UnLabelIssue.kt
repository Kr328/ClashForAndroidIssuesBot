package com.github.kr328.bot.action

import com.github.kr328.bot.model.None

class UnLabelIssue(
    private val issueId: String,
    private val labelId: String
) : Action<None>(None.serializer()) {
    override fun query(): String {
        return """
            mutation { 
              removeLabelsFromLabelable(input: {labelableId: "$issueId", labelIds: ["$labelId"]}) {
                clientMutationId
              }
            }
        """.trimIndent()
    }
}