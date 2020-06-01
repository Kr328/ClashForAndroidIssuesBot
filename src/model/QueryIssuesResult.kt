package com.github.kr328.bot.model

import kotlinx.serialization.Serializable

@Serializable
data class QueryIssuesResult(val data: Data) {
    @Serializable
    data class Data(val user: User)

    @Serializable
    data class User(val repository: Repository)

    @Serializable
    data class Repository(val issues: Issues)

    @Serializable
    data class Issues(val nodes: List<Issue>, val pageInfo: PageInfo)

    @Serializable
    data class PageInfo(val endCursor: String, val hasNextPage: Boolean)

    @Serializable
    data class Issue(val id: String, val title: String, val createdAt: String)
}