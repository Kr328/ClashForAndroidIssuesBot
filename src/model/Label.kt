package model

import kotlinx.serialization.Serializable

@Serializable
data class Label(val id: Long, val name: String)