package com.akira.ravex.ai

import android.content.Context

enum class AiProvider(val id: String, val displayName: String, val defaultEndpoint: String) {
    GROQ("groq", "Groq API", "https://api.groq.com/openai/v1"),
    OPENROUTER("openrouter", "OpenRouter API", "https://openrouter.ai/api/v1"),
    GEMINI("gemini", "Google Gemini API", "https://generativelanguage.googleapis.com/v1beta"),
    CUSTOM_OPENAI("custom_openai", "Custom OpenAI API", "https://api.openai.com/v1");

    companion object {
        fun fromId(id: String): AiProvider = values().firstOrNull { it.id == id } ?: GROQ
    }
}

enum class AiFeatureModule(val key: String, val displayName: String) {
    COPILOT("copilot", "Gaming Copilot"),
    TROUBLESHOOTER("troubleshooter", "AI Troubleshooter"),
    GAME_GUIDE("game_guide", "Game Research & Guides"),
    VOICE_COMMAND("voice_command", "Voice Command Interpreter"),
    LAG_DIAGNOSIS("lag_diagnosis", "Smart Lag Analyzer"),
    CAMERA_VISION("camera_vision", "AI Camera Visual Setup"),
    SESSION_COACH("session_coach", "Session Coach")
}

data class AiModelInfo(
    val id: String,
    val name: String,
    val providerId: String,
    val supportsVision: Boolean = false,
    val contextWindow: Int = 4096,
    val description: String = ""
)

data class AiChatMessage(
    val role: String, // "user", "assistant", "system"
    val content: String,
    val imageBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class AiResponse(
    val text: String,
    val provider: AiProvider,
    val modelUsed: String,
    val tokensUsed: Int = 0,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

interface AiProviderAdapter {
    val provider: AiProvider
    suspend fun testConnection(context: Context): Boolean
    suspend fun discoverModels(context: Context): List<AiModelInfo>
    suspend fun generateCompletion(
        context: Context,
        modelId: String,
        messages: List<AiChatMessage>,
        systemPrompt: String? = null
    ): AiResponse
}
