package com.github.kr328.bot.action

import com.github.kr328.bot.Mappers
import com.github.kr328.bot.model.CloseIssue
import io.ktor.client.HttpClient
import io.ktor.client.request.patch
import io.ktor.http.Url

class CloseAction: Action {
    override suspend fun action(client: HttpClient, target: Url) {
        client.patch<String>(target) {
            body = Mappers.JSON.stringify(CloseIssue.serializer(), CloseIssue())
        }
    }
}