package com.neorefraction.htm1_aiassistant.ai

import com.neorefraction.htm1_aiassistant.BuildConfig
import com.neorefraction.htm1_aiassistant.model.ContentPart
import com.neorefraction.htm1_aiassistant.model.ImageObject
import com.neorefraction.htm1_aiassistant.model.Message
import com.neorefraction.htm1_aiassistant.model.RequestPayload
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class AiClient {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; prettyPrint = true; isLenient = true })
        }
        install(DefaultRequest) {
            headers.append("client_id", BuildConfig.CLIENT_ID)
            headers.append("client_secret", BuildConfig.CLIENT_SECRET)
        }
    }

    suspend fun fetchResponse(prompt: String, imageBase64Url: String): JsonElement = client.post(BuildConfig.BASE_URL) {
        headers { append(HttpHeaders.Accept, "application/json") }
        contentType(ContentType.Application.Json)


        val userMessage = Message(
            role = "user",
            content = listOf(
                ContentPart.Text(text = prompt),
                ContentPart.ImageUrl(image_url = ImageObject(url = imageBase64Url))
            )
        )

        val payload = RequestPayload(
            messages = listOf(userMessage),
            max_tokens = 100,
            stream = false
        )

        setBody(Json.encodeToString(RequestPayload.serializer(), payload))
    }.body()
}