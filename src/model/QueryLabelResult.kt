package com.github.kr328.bot.model

import kotlinx.serialization.Serializable

@Serializable
data class QueryLabelResult(val data: Data) {
    @Serializable
    data class Data(val repository: Repository)

    @Serializable
    data class Repository(val label: Label)

    @Serializable
    data class Label(val id: String)
}