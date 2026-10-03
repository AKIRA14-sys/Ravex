package com.akira.ravex.ai

import android.content.Context
import com.akira.ravex.util.RavexSecurity
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OpenRouterAdapter : AiProviderAdapter {
    override val provider = AiProvider.OPENROUTER
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    override suspend fun testConnection(context: Context): Boolean = withContext(Dispatchers.IO) {
        val apiKey = RavexSecurity.getApiKey(context, provider.id)
        if (apiKey.isBlank()) return@withContext false
        try {
            val req = Request.Builder()
                .url("${provider.defaultEndpoint}/models")
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            client.newCall(req).execute().use { resp -> resp.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun discoverModels(context: Context): List<AiModelInfo> = withContext(Dispatchers.IO) {
        val apiKey = RavexSecurity.getApiKey(context, provider.id)
        if (apiKey.isBlank()) return@withContext emptyList()
        try {
            val req = Request.Builder()
                .url("${provider.defaultEndpoint}/models")
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val body = resp.body?.string() ?: return@withContext emptyList()
                val jsonObj = gson.fromJson(body, JsonObject::class.java)
                val dataArr = jsonObj.getAsJsonArray("data") ?: return@withContext emptyList()
                dataArr.mapNotNull { elem ->
                    try {
                        val m = elem.asJsonObject
                        val id = m.get("id").asString
                        val name = if (m.has("name")) m.get("name").asString else id
                        val isVision = id.contains("vision", ignoreCase = true) || id.contains("claude-3") || id.contains("gpt-4o")
                        AiModelInfo(
                            id = id,
                            name = name,
                            providerId = provider.id,
                            supportsVision = isVision,
                            contextWindow = 16384
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun generateCompletion(
        context: Context,
        modelId: String,
        messages: List<AiChatMessage>,
        systemPrompt: String?
    ): AiResponse = withContext(Dispatchers.IO) {
        val apiKey = RavexSecurity.getApiKey(context, provider.id)
        if (apiKey.isBlank()) {
            return@withContext AiResponse(
                text = "",
                provider = provider,
                modelUsed = modelId,
                isSuccess = false,
                errorMessage = "OpenRouter API Key is missing."
            )
        }
        try {
            val jsonMsgs = mutableListOf<Any>()
            if (!systemPrompt.isNullOrBlank()) {
                jsonMsgs.add(mapOf("role" to "system", "content" to systemPrompt))
            }
            messages.forEach { msg ->
                if (!msg.imageBase64.isNullOrBlank()) {
                    val contentList = listOf(
                        mapOf("type" to "text", "text" to msg.content),
                        mapOf("type" to "image_url", "image_url" to mapOf("url" to "data:image/jpeg;base64,${msg.imageBase64}"))
                    )
                    jsonMsgs.add(mapOf("role" to msg.role, "content" to contentList))
                } else {
                    jsonMsgs.add(mapOf("role" to msg.role, "content" to msg.content))
                }
            }

            val reqBodyMap = mapOf(
                "model" to modelId,
                "messages" to jsonMsgs
            )
            val jsonBody = gson.toJson(reqBodyMap)
            val req = Request.Builder()
                .url("${provider.defaultEndpoint}/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("HTTP-Referer", "https://akira.ravex.app")
                .addHeader("X-Title", "AKIRA RAVEX Gaming Command Center")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                val bodyStr = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return@withContext AiResponse(
                        text = "",
                        provider = provider,
                        modelUsed = modelId,
                        isSuccess = false,
                        errorMessage = "OpenRouter Error (${resp.code}): $bodyStr"
                    )
                }
                val jsonObj = gson.fromJson(bodyStr, JsonObject::class.java)
                val choices = jsonObj.getAsJsonArray("choices")
                val content = choices[0].asJsonObject.getAsJsonObject("message").get("content").asString
                AiResponse(text = content, provider = provider, modelUsed = modelId)
            }
        } catch (e: Exception) {
            AiResponse(
                text = "",
                provider = provider,
                modelUsed = modelId,
                isSuccess = false,
                errorMessage = "OpenRouter Network Exception: ${e.localizedMessage}"
            )
        }
    }
}
