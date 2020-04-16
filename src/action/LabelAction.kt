package com.github.kr328.bot.action

import com.github.kr328.bot.Mappers
import com.github.kr328.bot.model.AddLabel
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.Url

class LabelAction(private val with: String): Action {
    override suspend fun action(client: HttpClient, target: Url) {
        client.post<String>("$target/labels") {
            body = Mappers.JSON.stringify(AddLabel.serializer(), AddLabel(listOf(with)))
        }
    }
}