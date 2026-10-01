package com.akira.ravex

import org.junit.Test
import org.junit.Assert.*
import com.akira.ravex.data.CrosshairPresetsRepository
import com.akira.ravex.model.CrosshairCategory
import com.akira.ravex.util.SystemMonitorUtil

class CoreDataModelsTest {

    @Test
    fun testCrosshairPresetsCountAndIntegrity() {
        val presets = CrosshairPresetsRepository.presets
        assertTrue("Presets count should be at least 200", presets.size >= 200)

        val uniqueIds = presets.map { it.id }.toSet()
        assertEquals("All preset IDs must be unique", presets.size, uniqueIds.size)

        val defaultPreset = CrosshairPresetsRepository.getPresetById("preset_default_1")
        assertEquals("RAVEX Apex Red", defaultPreset.name)
        assertEquals(CrosshairCategory.TACTICAL, defaultPreset.category)
    }

    @Test
    fun testThermalStatusMapping() {
        val coolStatus = SystemMonitorUtil.getThermalStatus(30.0f)
        assertEquals("Cool temp should be NORMAL warning level", com.akira.ravex.model.ThermalStatus.WarningLevel.NORMAL, coolStatus.warningLevel)

        val criticalStatus = SystemMonitorUtil.getThermalStatus(46.0f)
        assertEquals("46C should trigger CRITICAL warning level", com.akira.ravex.model.ThermalStatus.WarningLevel.CRITICAL, criticalStatus.warningLevel)
    }
}
