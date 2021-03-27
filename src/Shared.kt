package com.github.kr328.bot

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

object Shared {
    private val API_SECRET = System.getenv("API_SECRET") ?: throw Error("app id not set")

    val JSON = Json {
        ignoreUnknownKeys = true
    }
    val HTTP = HttpClient(Apache) {
        this.defaultRequest {
            headers["Authorization"] = "Bearer $API_SECRET"

            contentType(ContentType.Application.Json)
        }
    }
}