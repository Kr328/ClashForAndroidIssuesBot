package com.github.kr328.bot.action

import com.github.kr328.bot.Mappers
import com.github.kr328.bot.model.CreateComment
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.Url

class CommentAction(private val with: String): Action {
    override suspend fun action(client: HttpClient, target: Url) {
        client.post<String>("$target/comments") {
            body = Mappers.JSON.stringify(CreateComment.serializer(), CreateComment(with))
        }
    }
}