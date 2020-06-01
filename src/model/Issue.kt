package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Issue(
    val id: Long,
    val url: String,
    val title: String,
    val labels: List<Label>,
    val locked: Boolean,
    val state: String,
    @SerialName("node_id") val nodeId: String,
    @SerialName("author_association") val authorAssociation: String
)