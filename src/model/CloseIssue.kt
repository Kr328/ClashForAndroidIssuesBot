package com.github.kr328.bot.model

import kotlinx.serialization.Serializable

@Serializable
data class CloseIssue(
    val state: String = "closed"
)