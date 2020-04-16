package com.github.kr328.bot.action

import io.ktor.client.HttpClient
import io.ktor.http.Url

interface Action {
    suspend fun action(client: HttpClient, target: Url)
}