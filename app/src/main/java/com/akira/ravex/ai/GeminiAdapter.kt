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

class GeminiAdapter : AiProviderAdapter {
    override val provider = AiProvider.GEMINI
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
                .url("${provider.defaultEndpoint}/models?key=$apiKey")
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
                .url("${provider.defaultEndpoint}/models?key=$apiKey")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val body = resp.body?.string() ?: return@withContext emptyList()
                val jsonObj = gson.fromJson(body, JsonObject::class.java)
                val modelsArr = jsonObj.getAsJsonArray("models") ?: return@withContext emptyList()
                modelsArr.mapNotNull { elem ->
                    try {
                        val m = elem.asJsonObject
                        val rawName = m.get("name").asString // "models/gemini-1.5-flash"
                        val id = rawName.replace("models/", "")
                        val displayName = if (m.has("displayName")) m.get("displayName").asString else id
                        val isVision = id.contains("flash") || id.contains("pro") || id.contains("vision")
                        AiModelInfo(
                            id = id,
                            name = displayName,
                            providerId = provider.id,
                            supportsVision = isVision,
                            contextWindow = 32768
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
                errorMessage = "Google Gemini API Key is missing."
            )
        }
        try {
            val contents = mutableListOf<Map<String, Any>>()

            // System prompt as first text if present
            val promptPrefix = if (!systemPrompt.isNullOrBlank()) "System Instructions: $systemPrompt\n\n" else ""

            messages.forEach { msg ->
                val role = if (msg.role == "assistant") "model" else "user"
                val parts = mutableListOf<Map<String, Any>>()

                val textContent = if (promptPrefix.isNotEmpty() && msg == messages.first()) promptPrefix + msg.content else msg.content
                parts.add(mapOf("text" to textContent))

                if (!msg.imageBase64.isNullOrBlank()) {
                    parts.add(
                        mapOf(
                            "inlineData" to mapOf(
                                "mimeType" to "image/jpeg",
                                "data" to msg.imageBase64
                            )
                        )
                    )
                }
                contents.add(mapOf("role" to role, "parts" to parts))
            }

            val reqBodyMap = mapOf("contents" to contents)
            val jsonBody = gson.toJson(reqBodyMap)
            val targetModel = if (modelId.contains("/")) modelId else "models/$modelId"

            val req = Request.Builder()
                .url("${provider.defaultEndpoint}/$targetModel:generateContent?key=$apiKey")
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
                        errorMessage = "Gemini Error (${resp.code}): $bodyStr"
                    )
                }
                val jsonObj = gson.fromJson(bodyStr, JsonObject::class.java)
                val candidates = jsonObj.getAsJsonArray("candidates")
                val firstCand = candidates[0].asJsonObject
                val partsArr = firstCand.getAsJsonObject("content").getAsJsonArray("parts")
                val responseText = partsArr[0].asJsonObject.get("text").asString

                AiResponse(text = responseText, provider = provider, modelUsed = modelId)
            }
        } catch (e: Exception) {
            AiResponse(
                text = "",
                provider = provider,
                modelUsed = modelId,
                isSuccess = false,
                errorMessage = "Gemini Exception: ${e.localizedMessage}"
            )
        }
    }
}
