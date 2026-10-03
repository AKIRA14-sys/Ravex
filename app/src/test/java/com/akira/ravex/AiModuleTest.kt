package com.akira.ravex

import com.akira.ravex.ai.AiModelInfo
import com.akira.ravex.ai.AiProvider
import com.akira.ravex.model.GameProfile
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.*
import org.junit.Test

class AiModuleTest {

    private val gson = Gson()

    @Test
    fun testGroqModelParsing() {
        val jsonSample = """
            {
              "object": "list",
              "data": [
                {"id": "llama3-70b-8192", "object": "model"},
                {"id": "llama3-8b-8192", "object": "model"}
              ]
            }
        """.trimIndent()

        val jsonObj = gson.fromJson(jsonSample, JsonObject::class.java)
        val dataArr = jsonObj.getAsJsonArray("data")
        assertNotNull(dataArr)
        assertEquals(2, dataArr.size())

        val firstModelId = dataArr[0].asJsonObject.get("id").asString
        assertEquals("llama3-70b-8192", firstModelId)
    }

    @Test
    fun testGeminiModelParsing() {
        val jsonSample = """
            {
              "models": [
                {"name": "models/gemini-1.5-flash", "displayName": "Gemini 1.5 Flash"},
                {"name": "models/gemini-1.5-pro", "displayName": "Gemini 1.5 Pro"}
              ]
            }
        """.trimIndent()

        val jsonObj = gson.fromJson(jsonSample, JsonObject::class.java)
        val modelsArr = jsonObj.getAsJsonArray("models")
        assertNotNull(modelsArr)
        assertEquals(2, modelsArr.size())

        val rawName = modelsArr[0].asJsonObject.get("name").asString
        val cleanId = rawName.replace("models/", "")
        assertEquals("gemini-1.5-flash", cleanId)
    }

    @Test
    fun testGameProfileSerialization() {
        val profile = GameProfile(
            packageName = "com.pubg.krmobile",
            gameName = "PUBG Mobile",
            assignedCrosshairId = "preset_tps_1",
            targetFps = 90,
            customNotes = "Gyroscope 200% on 4x scope"
        )

        val json = gson.toJson(profile)
        assertTrue(json.contains("com.pubg.krmobile"))
        assertTrue(json.contains("PUBG Mobile"))

        val deserialized = gson.fromJson(json, GameProfile::class.java)
        assertEquals("com.pubg.krmobile", deserialized.packageName)
        assertEquals(90, deserialized.targetFps)
    }
}
