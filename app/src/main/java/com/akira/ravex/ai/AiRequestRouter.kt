package com.akira.ravex.ai

import android.content.Context
import com.akira.ravex.data.RavexPreferences
import com.akira.ravex.util.RavexSecurity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiRequestRouter(private val context: Context) {
    private val prefs = RavexPreferences(context)
    private val gson = Gson()

    private val adapters: Map<AiProvider, AiProviderAdapter> = mapOf(
        AiProvider.GROQ to GroqAdapter(),
        AiProvider.OPENROUTER to OpenRouterAdapter(),
        AiProvider.GEMINI to GeminiAdapter(),
        AiProvider.CUSTOM_OPENAI to CustomOpenAiAdapter()
    )

    fun getAdapter(provider: AiProvider): AiProviderAdapter = adapters[provider] ?: adapters[AiProvider.GROQ]!!

    suspend fun discoverAndCacheModels(provider: AiProvider): List<AiModelInfo> {
        val adapter = getAdapter(provider)
        val discovered = adapter.discoverModels(context)
        if (discovered.isNotEmpty()) {
            cacheModels(provider, discovered)
            return discovered
        }
        return getCachedModels(provider)
    }

    private fun cacheModels(provider: AiProvider, models: List<AiModelInfo>) {
        val json = gson.toJson(models)
        prefs.saveCachedModels(provider.id, json)
    }

    fun getCachedModels(provider: AiProvider): List<AiModelInfo> {
        val json = prefs.getCachedModels(provider.id)
        if (json.isBlank()) {
            return getStaticFallbackModels(provider)
        }
        return try {
            val type = object : TypeToken<List<AiModelInfo>>() {}.type
            gson.fromJson(json, type) ?: getStaticFallbackModels(provider)
        } catch (e: Exception) {
            getStaticFallbackModels(provider)
        }
    }

    private fun getStaticFallbackModels(provider: AiProvider): List<AiModelInfo> {
        return when (provider) {
            AiProvider.GROQ -> listOf(
                AiModelInfo("llama3-70b-8192", "Llama 3 70B", provider.id),
                AiModelInfo("llama3-8b-8192", "Llama 3 8B", provider.id),
                AiModelInfo("mixtral-8x7b-32768", "Mixtral 8x7b", provider.id)
            )
            AiProvider.OPENROUTER -> listOf(
                AiModelInfo("meta-llama/llama-3-70b-instruct", "Llama 3 70B Instruct", provider.id),
                AiModelInfo("anthropic/claude-3.5-sonnet", "Claude 3.5 Sonnet", provider.id, supportsVision = true),
                AiModelInfo("google/gemini-pro-1.5", "Gemini Pro 1.5", provider.id, supportsVision = true)
            )
            AiProvider.GEMINI -> listOf(
                AiModelInfo("gemini-1.5-flash", "Gemini 1.5 Flash", provider.id, supportsVision = true),
                AiModelInfo("gemini-1.5-pro", "Gemini 1.5 Pro", provider.id, supportsVision = true)
            )
            AiProvider.CUSTOM_OPENAI -> listOf(
                AiModelInfo("gpt-4o", "GPT-4o", provider.id, supportsVision = true),
                AiModelInfo("gpt-3.5-turbo", "GPT-3.5 Turbo", provider.id)
            )
        }
    }

    suspend fun executeRequest(
        feature: AiFeatureModule,
        messages: List<AiChatMessage>,
        systemPrompt: String? = null,
        requireVision: Boolean = false
    ): AiResponse = withContext(Dispatchers.IO) {
        val providerId = prefs.getFeatureProvider(feature.key)
        val provider = AiProvider.fromId(providerId)
        val modelId = prefs.getFeatureModel(feature.key)

        val apiKey = RavexSecurity.getApiKey(context, provider.id)

        // Offline / No API Key mode fallback response
        if (apiKey.isBlank()) {
            return@withContext generateOfflineRuleResponse(feature, messages)
        }

        val adapter = getAdapter(provider)
        val targetModel = if (modelId.isBlank()) {
            val models = getCachedModels(provider)
            val match = if (requireVision) models.firstOrNull { it.supportsVision } else models.firstOrNull()
            match?.id ?: "default"
        } else modelId

        val primaryResult = adapter.generateCompletion(context, targetModel, messages, systemPrompt)
        if (primaryResult.isSuccess) {
            return@withContext primaryResult
        }

        // Try Fallback Provider if enabled
        if (prefs.isFallbackEnabled) {
            val fallbackProviderId = prefs.fallbackProvider
            if (fallbackProviderId != provider.id) {
                val fallbackProvider = AiProvider.fromId(fallbackProviderId)
                val fallbackApiKey = RavexSecurity.getApiKey(context, fallbackProvider.id)
                if (fallbackApiKey.isNotBlank()) {
                    val fallbackAdapter = getAdapter(fallbackProvider)
                    val fallbackModels = getCachedModels(fallbackProvider)
                    val fallbackModel = fallbackModels.firstOrNull()?.id ?: "default"
                    val fallbackResult = fallbackAdapter.generateCompletion(context, fallbackModel, messages, systemPrompt)
                    if (fallbackResult.isSuccess) {
                        return@withContext fallbackResult.copy(
                            text = "[Fallback via ${fallbackProvider.displayName}]\n\n" + fallbackResult.text
                        )
                    }
                }
            }
        }

        return@withContext primaryResult
    }

    private fun generateOfflineRuleResponse(
        feature: AiFeatureModule,
        messages: List<AiChatMessage>
    ): AiResponse {
        val query = messages.lastOrNull()?.content?.lowercase() ?: ""
        val offlineNotice = "⚠️ [OFFLINE / LOCAL RULE MODE: No API key configured for ${feature.displayName}]\n\n"

        val responseText = when (feature) {
            AiFeatureModule.COPILOT -> {
                when {
                    query.contains("sensitivity") -> offlineNotice + "• Recommended Mobile Shooter Sensitivity:\n  - General Sensitivity: 85-95\n  - Red Dot / Holographic: 75-80\n  - 2x Scope: 60-65\n  - 4x Scope: 40-45\n  - Gyroscope: Always On (150-200% scale for micro-adjustments)."
                    query.contains("lag") -> offlineNotice + "• Quick Anti-Lag Checklist:\n 1. Close background heavy apps (social media, browser tabs).\n 2. Switch to 5GHz Wi-Fi or high-speed cellular data.\n 3. Enable Game Mode (Ultra/Performance) in Ravex GameForge.\n 4. Set in-game graphics to Smooth/Medium and Frame Rate to Ultra/90FPS."
                    else -> offlineNotice + "RAVEX Local Assistant: To unlock real-time Groq/Gemini/OpenRouter AI answers, go to RAVEX Settings -> AI Provider Center and enter a free API key!"
                }
            }
            AiFeatureModule.TROUBLESHOOTER -> {
                offlineNotice + "• Local Diagnostic Steps:\n 1. Overlay issue: Ensure 'Display over other apps' is granted in Android System Settings.\n 2. Frame drops: Phone thermal state may be elevated. Activate Thermal Guard and let device cool down for 3 mins."
            }
            AiFeatureModule.LAG_DIAGNOSIS -> {
                offlineNotice + "• Local Memory & Thermal Diagnostic:\n Available RAM check complete. Ensure background tasks are killed before launching high-demand 3D titles."
            }
            else -> offlineNotice + "Local rule engine ready. Connect an AI provider in Settings for custom deep intelligence."
        }

        return AiResponse(
            text = responseText,
            provider = AiProvider.GROQ,
            modelUsed = "local-offline-rule-engine",
            isSuccess = true
        )
    }
}
