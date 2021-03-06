package com.github.kr328.bot.action

import com.github.kr328.bot.Shared
import io.ktor.client.request.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

abstract class Action<R>(private val serializer: KSerializer<R>) {
    @Serializable
    data class Query(val query: String)

    suspend fun action(): R {
        val result = Shared.HTTP.post<String>("https://api.github.com/graphql") {
            this.body = Shared.JSON.encodeToString(Query.serializer(), Query(query()))
        }

        return Shared.JSON.decodeFromString(serializer, result)
    }

    abstract fun query(): String
}