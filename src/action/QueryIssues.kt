package com.github.kr328.bot.action

import com.github.kr328.bot.model.QueryIssuesResult

class QueryIssues(
    private val user: String,
    private val repository: String,
    private val labels: List<String>,
    private val after: String?
) : Action<QueryIssuesResult>(QueryIssuesResult.serializer()) {
    override fun query(): String {
        val labelsString = labels.joinToString(separator = "\",\"", prefix = "[\"", postfix = "\"]")

        return if ( after == null ) {
            """
            query {
              user(login: "$user") {
                repository(name: "$repository") {
                  issues(first: 100, labels: $labelsString) {
                    nodes {
                      id
                      title
                      createdAt
                    }
                    pageInfo {
                      hasNextPage
                      endCursor
                    }
                  }
                }
              }
            }
        """.trimIndent()
        } else {
            """
            query {
              user(login: "$user") {
                repository(name: "$repository") {
                  issues(first: 100, after: "$after", labels: $labelsString) {
                    nodes {
                      id
                      title
                      createdAt
                    }
                    pageInfo {
                      hasNextPage
                      endCursor
                    }
                  }
                }
              }
            }
        """.trimIndent()
        }
    }
}