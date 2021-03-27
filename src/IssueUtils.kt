package com.github.kr328.bot

import io.ktor.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.codec.binary.Hex
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object IssueUtils {
    private val WEBHOOK_SECRET = System.getenv("WEBHOOK_SECRET") ?: throw Error("webhook secret not set")

    suspend fun validContent(request: ApplicationRequest, content: ByteArray): Boolean {
        return withContext(Dispatchers.Default) {
            val key = SecretKeySpec(WEBHOOK_SECRET.toByteArray(), "HmacSHA1")
            val mac = Mac.getInstance("HmacSHA1")

            mac.init(key)

            val digest = String(Hex().encode(mac.doFinal(content)))

            "sha1=$digest".trim() == request.headers["X-HUB-SIGNATURE"]?.trim()
        }
    }
}