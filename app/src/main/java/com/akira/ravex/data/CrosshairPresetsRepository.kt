package com.akira.ravex.data

import com.akira.ravex.model.CrosshairCategory
import com.akira.ravex.model.CrosshairPreset
import com.akira.ravex.model.CrosshairShape

object CrosshairPresetsRepository {

    private val colorsList = listOf(
        "#FF2A55", "#00F0FF", "#39FF14", "#FFD700", "#FF007F",
        "#00FFFF", "#FFFFFF", "#FF5500", "#A020F0", "#00FF7F"
    )

    val presets: List<CrosshairPreset> by lazy {
        generate200Presets()
    }

    fun getPresetById(id: String): CrosshairPreset {
        return presets.firstOrNull { it.id == id } ?: presets.first()
    }

    private fun generate200Presets(): List<CrosshairPreset> {
        val list = mutableListOf<CrosshairPreset>()

        // Core Default Presets
        list.add(
            CrosshairPreset(
                id = "preset_default_1",
                name = "RAVEX Apex Red",
                category = CrosshairCategory.TACTICAL,
                shape = CrosshairShape.CROSS,
                colorHex = "#FF2A55",
                outlineColorHex = "#000000",
                sizeDp = 24f,
                strokeWidthDp = 2.5f,
                gapDp = 4f,
                showDot = true,
                dotRadiusDp = 2f
            )
        )

        list.add(
            CrosshairPreset(
                id = "preset_default_2",
                name = "Cyberpunk Neon Cyan",
                category = CrosshairCategory.CYBERPUNK,
                shape = CrosshairShape.CIRCLE_CROSS,
                colorHex = "#00F0FF",
                outlineColorHex = "#001122",
                sizeDp = 28f,
                strokeWidthDp = 2f,
                gapDp = 6f,
                showDot = true,
                isAnimated = true
            )
        )

        list.add(
            CrosshairPreset(
                id = "preset_default_3",
                name = "Minimalist Micro Dot",
                category = CrosshairCategory.MINIMAL,
                shape = CrosshairShape.DOT,
                colorHex = "#39FF14",
                outlineColorHex = "#000000",
                sizeDp = 12f,
                strokeWidthDp = 1f,
                showDot = true,
                dotRadiusDp = 3f
            )
        )

        var idCounter = 4

        // 1. Tactical FPS Presets (45 items)
        for (i in 1..45) {
            val shape = CrosshairShape.values()[i % 5]
            val color = colorsList[i % colorsList.size]
            val gap = 2f + (i % 6) * 1.5f
            val stroke = 1.5f + (i % 4) * 0.5f
            val size = 18f + (i % 8) * 2f
            list.add(
                CrosshairPreset(
                    id = "preset_tac_$idCounter",
                    name = "Tactical Precision Alpha-$i",
                    category = CrosshairCategory.TACTICAL,
                    shape = shape,
                    colorHex = color,
                    outlineColorHex = if (i % 2 == 0) "#000000" else "#222222",
                    sizeDp = size,
                    strokeWidthDp = stroke,
                    gapDp = gap,
                    showDot = (i % 3 != 0),
                    dotRadiusDp = 1.5f + (i % 3)
                )
            )
            idCounter++
        }

        // 2. Cyberpunk / Sci-Fi Presets (45 items)
        for (i in 1..45) {
            val shape = CrosshairShape.values()[5 + (i % 5)]
            val color = colorsList[(i + 2) % colorsList.size]
            val isAnim = (i % 2 == 0)
            val rotation = (i * 15f) % 360f
            list.add(
                CrosshairPreset(
                    id = "preset_cyber_$idCounter",
                    name = "Cyber Matrix V$i",
                    category = CrosshairCategory.CYBERPUNK,
                    shape = shape,
                    colorHex = color,
                    outlineColorHex = "#050814",
                    sizeDp = 26f + (i % 6) * 2f,
                    strokeWidthDp = 2f,
                    gapDp = 3f + (i % 5),
                    isAnimated = isAnim,
                    rotationAngle = rotation,
                    shadowEffect = true
                )
            )
            idCounter++
        }

        // 3. Minimal Dot & Clean Presets (45 items)
        for (i in 1..45) {
            val color = colorsList[(i + 4) % colorsList.size]
            val dotRadius = 1f + (i % 5) * 0.8f
            list.add(
                CrosshairPreset(
                    id = "preset_min_$idCounter",
                    name = "Minimal Clean #$i",
                    category = CrosshairCategory.MINIMAL,
                    shape = if (i % 2 == 0) CrosshairShape.DOT else CrosshairShape.CIRCLE_CROSS,
                    colorHex = color,
                    outlineColorHex = "#000000",
                    sizeDp = 10f + (i % 5) * 2f,
                    strokeWidthDp = 1f,
                    gapDp = 2f,
                    showDot = true,
                    dotRadiusDp = dotRadius
                )
            )
            idCounter++
        }

        // 4. Dynamic & Animated Presets (45 items)
        for (i in 1..45) {
            val shape = CrosshairShape.values()[i % CrosshairShape.values().size]
            val color = colorsList[(i + 6) % colorsList.size]
            list.add(
                CrosshairPreset(
                    id = "preset_dyn_$idCounter",
                    name = "Dynamic Pulse #$i",
                    category = CrosshairCategory.DYNAMIC,
                    shape = shape,
                    colorHex = color,
                    outlineColorHex = "#101010",
                    sizeDp = 22f + (i % 8) * 2f,
                    strokeWidthDp = 2.5f,
                    gapDp = 4f + (i % 4),
                    isAnimated = true,
                    shadowEffect = true
                )
            )
            idCounter++
        }

        // 5. Custom / Specialty Presets (Remaining up to 210 total)
        val needed = 210 - list.size
        for (i in 1..needed) {
            val shape = CrosshairShape.values()[(i * 3) % CrosshairShape.values().size]
            val color = colorsList[(i * 2) % colorsList.size]
            list.add(
                CrosshairPreset(
                    id = "preset_custom_$idCounter",
                    name = "Pro Custom Spec-$i",
                    category = CrosshairCategory.CUSTOM,
                    shape = shape,
                    colorHex = color,
                    outlineColorHex = "#000000",
                    sizeDp = 24f,
                    strokeWidthDp = 2f,
                    gapDp = 3f,
                    isCustom = true
                )
            )
            idCounter++
        }

        return list
    }
}
