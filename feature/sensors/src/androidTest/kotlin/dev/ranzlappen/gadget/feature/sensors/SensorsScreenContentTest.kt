package dev.ranzlappen.gadget.feature.sensors

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the stateless sensor-explorer screens (v3 B5):
 * the grouped list (and its copy button) and a detail screen with live
 * values. Hilt-free. Runs via `:feature:sensors:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class SensorsScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val accel = previewSensor(0, Sensor.TYPE_ACCELEROMETER, "Accelerometer", SensorGroup.Motion, "m/s²", listOf("X", "Y", "Z"))
    private val pressure = previewSensor(1, Sensor.TYPE_PRESSURE, "Pressure", SensorGroup.Environment, "hPa", listOf("Pressure"))

    @Test
    fun listGroupsSensorsAndOpensTappedOne() {
        var opened = -1
        composeTestRule.setContent {
            GadgetTestTheme {
                SensorsScreenContent(sensors = listOf(accel, pressure), onOpenSensor = { opened = it }, onCopyList = {})
            }
        }
        composeTestRule.onNodeWithText("Motion").assertIsDisplayed()
        composeTestRule.onNodeWithText("Environment").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pressure").performScrollTo().performClick()
        assertEquals(1, opened)
    }

    @Test
    fun copyListPutsEverySensorInTheReport() {
        var report = ""
        composeTestRule.setContent {
            GadgetTestTheme {
                SensorsScreenContent(sensors = listOf(accel, pressure), onOpenSensor = {}, onCopyList = { report = it })
            }
        }
        composeTestRule.onNodeWithText("Copy sensor list").performScrollTo().performClick()
        assertTrue(report.contains("Accelerometer"))
        assertTrue(report.contains("Pressure"))
    }

    @Test
    fun detailShowsEachAxisWithItsValue() {
        val live = SensorLive().next(floatArrayOf(1f, 2f, 9.81f), SensorManager.SENSOR_STATUS_ACCURACY_HIGH, 1L)
        composeTestRule.setContent {
            GadgetTestTheme {
                SensorDetailContent(info = accel, live = live, permissionGranted = true, onRequestPermission = {}, onCopy = {})
            }
        }
        composeTestRule.onNodeWithText("Z").assertIsDisplayed()
        composeTestRule.onNodeWithText("9.810 m/s²").assertIsDisplayed()
    }
}
