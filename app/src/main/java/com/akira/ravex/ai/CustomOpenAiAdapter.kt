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

class CustomOpenAiAdapter : AiProviderAdapter {
    override val provider = AiProvider.CUSTOM_OPENAI
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
                        AiModelInfo(
                            id = id,
                            name = id,
                            providerId = provider.id,
                            supportsVision = id.contains("4o") || id.contains("vision"),
                            contextWindow = 8192
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
                errorMessage = "Custom OpenAI API Key is missing."
            )
        }
        try {
            val jsonMsgs = mutableListOf<Map<String, String>>()
            if (!systemPrompt.isNullOrBlank()) {
                jsonMsgs.add(mapOf("role" to "system", "content" to systemPrompt))
            }
            messages.forEach { jsonMsgs.add(mapOf("role" to it.role, "content" to it.content)) }

            val reqBodyMap = mapOf(
                "model" to modelId,
                "messages" to jsonMsgs
            )
            val jsonBody = gson.toJson(reqBodyMap)
            val req = Request.Builder()
                .url("${provider.defaultEndpoint}/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
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
                        errorMessage = "Custom OpenAI Error (${resp.code}): $bodyStr"
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
                errorMessage = "Custom OpenAI Exception: ${e.localizedMessage}"
            )
        }
    }
}
