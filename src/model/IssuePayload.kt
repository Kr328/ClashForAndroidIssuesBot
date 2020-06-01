package model

import kotlinx.serialization.Serializable

@Serializable
data class IssuePayload(
    val action: String,
    val issue: Issue,
    val label: Label? = null
)