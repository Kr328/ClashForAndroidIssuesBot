package action

import com.github.kr328.bot.action.Action
import com.github.kr328.bot.model.QueryLabelResult

class QueryLabel(
    private val repoOwner: String,
    private val repoName: String,
    private val name: String
) : Action<QueryLabelResult>(QueryLabelResult.serializer()) {
    override fun query(): String {
        return """
            query { 
              repository(owner: "$repoOwner", name: "$repoName") {
                label(name: "$name") {
                  id
                }
              }
            }
        """.trimIndent()
    }
}