package com.akira.ravex.data

import android.content.Context
import android.content.SharedPreferences
import com.akira.ravex.model.CrosshairPreset
import com.akira.ravex.model.GameProfile
import org.json.JSONArray
import org.json.JSONObject

class RavexPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ravex_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE_CROSSHAIR_ID = "active_crosshair_id"
        private const val KEY_HUD_ENABLED = "hud_enabled"
        private const val KEY_CROSSHAIR_ENABLED = "crosshair_enabled"
        private const val KEY_THERMAL_GUARD_ENABLED = "thermal_guard_enabled"
        private const val KEY_FPS_ENABLED = "fps_enabled"
        private const val KEY_GAME_MODE = "game_mode" // BALANCED, PERFORMANCE, ULTRA
        private const val KEY_GAME_PROFILES_JSON = "game_profiles_json"
        private const val KEY_CUSTOM_PRESETS_JSON = "custom_presets_json"

        // AI Provider Settings
        private const val KEY_DEFAULT_PROVIDER = "default_ai_provider"
        private const val KEY_DEFAULT_MODEL = "default_ai_model"
        private const val KEY_FALLBACK_ENABLED = "fallback_enabled"
        private const val KEY_FALLBACK_PROVIDER = "fallback_provider"
        private const val KEY_HUD_ACCENT_COLOR = "hud_accent_color"
        private const val KEY_HUD_OPACITY = "hud_opacity"
        private const val KEY_HUD_SIZE = "hud_size"
    }

    var activeCrosshairId: String
        get() = prefs.getString(KEY_ACTIVE_CROSSHAIR_ID, "preset_default_1") ?: "preset_default_1"
        set(value) = prefs.edit().putString(KEY_ACTIVE_CROSSHAIR_ID, value).apply()

    var isHudEnabled: Boolean
        get() = prefs.getBoolean(KEY_HUD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HUD_ENABLED, value).apply()

    var isCrosshairEnabled: Boolean
        get() = prefs.getBoolean(KEY_CROSSHAIR_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CROSSHAIR_ENABLED, value).apply()

    var isThermalGuardEnabled: Boolean
        get() = prefs.getBoolean(KEY_THERMAL_GUARD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_THERMAL_GUARD_ENABLED, value).apply()

    var isFpsEnabled: Boolean
        get() = prefs.getBoolean(KEY_FPS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_FPS_ENABLED, value).apply()

    var gameMode: String
        get() = prefs.getString(KEY_GAME_MODE, "PERFORMANCE") ?: "PERFORMANCE"
        set(value) = prefs.edit().putString(KEY_GAME_MODE, value).apply()

    var defaultProvider: String
        get() = prefs.getString(KEY_DEFAULT_PROVIDER, "groq") ?: "groq"
        set(value) = prefs.edit().putString(KEY_DEFAULT_PROVIDER, value).apply()

    var defaultModel: String
        get() = prefs.getString(KEY_DEFAULT_MODEL, "llama3-70b-8192") ?: "llama3-70b-8192"
        set(value) = prefs.edit().putString(KEY_DEFAULT_MODEL, value).apply()

    var isFallbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_FALLBACK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_FALLBACK_ENABLED, value).apply()

    var fallbackProvider: String
        get() = prefs.getString(KEY_FALLBACK_PROVIDER, "openrouter") ?: "openrouter"
        set(value) = prefs.edit().putString(KEY_FALLBACK_PROVIDER, value).apply()

    var hudAccentColorHex: String
        get() = prefs.getString(KEY_HUD_ACCENT_COLOR, "#00E5FF") ?: "#00E5FF"
        set(value) = prefs.edit().putString(KEY_HUD_ACCENT_COLOR, value).apply()

    var hudOpacityFloat: Float
        get() = prefs.getFloat(KEY_HUD_OPACITY, 0.95f)
        set(value) = prefs.edit().putFloat(KEY_HUD_OPACITY, value).apply()

    var hudSizeScale: Float
        get() = prefs.getFloat(KEY_HUD_SIZE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_HUD_SIZE, value).apply()

    fun getFeatureProvider(featureKey: String): String {
        return prefs.getString("feat_prov_$featureKey", defaultProvider) ?: defaultProvider
    }

    fun setFeatureProvider(featureKey: String, providerId: String) {
        prefs.edit().putString("feat_prov_$featureKey", providerId).apply()
    }

    fun getFeatureModel(featureKey: String): String {
        return prefs.getString("feat_mod_$featureKey", "") ?: ""
    }

    fun setFeatureModel(featureKey: String, modelId: String) {
        prefs.edit().putString("feat_mod_$featureKey", modelId).apply()
    }

    fun saveCachedModels(providerId: String, jsonModels: String) {
        prefs.edit().putString("cached_models_$providerId", jsonModels).apply()
    }

    fun getCachedModels(providerId: String): String {
        return prefs.getString("cached_models_$providerId", "") ?: ""
    }

    fun getGameProfiles(): Map<String, GameProfile> {
        val jsonStr = prefs.getString(KEY_GAME_PROFILES_JSON, null) ?: return emptyMap()
        val map = mutableMapOf<String, GameProfile>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val pkg = obj.getString("packageName")
                val name = obj.getString("gameName")
                val crosshairId = obj.optString("assignedCrosshairId", "preset_default_1")
                val targetFps = obj.optInt("targetFps", 60)
                val notes = obj.optString("customNotes", "")
                map[pkg] = GameProfile(pkg, name, crosshairId, targetFps, notes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    fun saveGameProfile(profile: GameProfile) {
        val current = getGameProfiles().toMutableMap()
        current[profile.packageName] = profile
        val jsonArray = JSONArray()
        current.values.forEach { p ->
            val obj = JSONObject()
            obj.put("packageName", p.packageName)
            obj.put("gameName", p.gameName)
            obj.put("assignedCrosshairId", p.assignedCrosshairId)
            obj.put("targetFps", p.targetFps)
            obj.put("customNotes", p.customNotes)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_GAME_PROFILES_JSON, jsonArray.toString()).apply()
    }

    fun getCustomPresets(): List<CrosshairPreset> {
        val jsonStr = prefs.getString(KEY_CUSTOM_PRESETS_JSON, null) ?: return emptyList()
        val list = mutableListOf<CrosshairPreset>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CrosshairPreset(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        category = com.akira.ravex.model.CrosshairCategory.CUSTOM,
                        shape = com.akira.ravex.model.CrosshairShape.valueOf(obj.optString("shape", "CROSS")),
                        colorHex = obj.optString("colorHex", "#FF2A55"),
                        outlineColorHex = obj.optString("outlineColorHex", "#000000"),
                        sizeDp = obj.optDouble("sizeDp", 24.0).toFloat(),
                        strokeWidthDp = obj.optDouble("strokeWidthDp", 2.5).toFloat(),
                        gapDp = obj.optDouble("gapDp", 4.0).toFloat(),
                        opacity = obj.optDouble("opacity", 1.0).toFloat(),
                        dotRadiusDp = obj.optDouble("dotRadiusDp", 2.0).toFloat(),
                        showDot = obj.optBoolean("showDot", true),
                        isAnimated = obj.optBoolean("isAnimated", false),
                        rotationAngle = obj.optDouble("rotationAngle", 0.0).toFloat(),
                        shadowEffect = obj.optBoolean("shadowEffect", true),
                        isCustom = true
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveCustomPreset(preset: CrosshairPreset) {
        val current = getCustomPresets().filter { it.id != preset.id }.toMutableList()
        current.add(preset)
        val array = JSONArray()
        current.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("shape", p.shape.name)
            obj.put("colorHex", p.colorHex)
            obj.put("outlineColorHex", p.outlineColorHex)
            obj.put("sizeDp", p.sizeDp)
            obj.put("strokeWidthDp", p.strokeWidthDp)
            obj.put("gapDp", p.gapDp)
            obj.put("opacity", p.opacity)
            obj.put("dotRadiusDp", p.dotRadiusDp)
            obj.put("showDot", p.showDot)
            obj.put("isAnimated", p.isAnimated)
            obj.put("rotationAngle", p.rotationAngle)
            obj.put("shadowEffect", p.shadowEffect)
            array.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_PRESETS_JSON, array.toString()).apply()
    }
}
