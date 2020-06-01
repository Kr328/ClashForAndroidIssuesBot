package com.github.kr328.bot

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

object Shared {
    private val API_SECRET = System.getenv("API_SECRET") ?: throw Error("app id not set")

    val JSON = Json(JsonConfiguration.Stable.copy(ignoreUnknownKeys = true))
    val HTTP = HttpClient(Apache) {
        this.defaultRequest {
            headers["Authorization"] = "Bearer $API_SECRET"

            contentType(ContentType.Application.Json)
        }
    }
}