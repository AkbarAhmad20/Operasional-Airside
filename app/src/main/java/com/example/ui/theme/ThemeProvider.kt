package com.example.ui.theme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

val Context.garbarataPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "garbarata_ops_preferences"
)

enum class ThemeMode(
    val title: String,
    val shortLabel: String,
    val description: String
) {
    AUTO_APRON(
        title = "Otomatis Apron (Cahaya & Shift)",
        shortLabel = "Auto Apron",
        description = "Beralih otomatis ke Mode Terang saat terik/siang & Mode Gelap saat malam/redup menggunakan sensor cahaya atau jadwal apron."
    ),
    AUTO_SYSTEM(
        title = "Otomatis Sistem",
        shortLabel = "Auto Sistem",
        description = "Mengikuti pengaturan tampilan gelap atau terang perangkat Android Anda."
    ),
    LIGHT(
        title = "Mode Terang (Daylight Tarmac)",
        shortLabel = "Terang",
        description = "Kontras tinggi khusus penggunaan luar ruangan di bawah sinar matahari apron."
    ),
    DARK(
        title = "Mode Gelap (Airside Night Ops)",
        shortLabel = "Gelap",
        description = "Tampilan rendah silau untuk shift malam dan operasi di dalam garbarata."
    );

    companion object {
        fun fromStored(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: AUTO_APRON
    }
}

class ThemePreferencesRepository(private val context: Context) {
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val themeModeFlow: Flow<ThemeMode> = context.garbarataPreferencesDataStore.data.map { prefs ->
        ThemeMode.fromStored(prefs[themeModeKey])
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.garbarataPreferencesDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }
}

@Immutable
data class ThemeControllerState(
    val selectedMode: ThemeMode = ThemeMode.AUTO_APRON,
    val isDarkActive: Boolean = true,
    val ambientLux: Float? = null,
    val activeReason: String = "Shift Malam Apron",
    val onSelectMode: (ThemeMode) -> Unit = {}
)

val LocalThemeController = staticCompositionLocalOf { ThemeControllerState() }

/**
 * Adaptive Theme Provider designed specifically for airport ground staff.
 * Supports:
 * 1. AUTO_APRON: Uses real Android ambient light sensor (Sensor.TYPE_LIGHT) with hysteresis
 *    (>= 220 lux -> Daylight Light, <= 70 lux -> Night Dark) and falls back automatically to
 *    local apron shift schedule (06:00-17:59 Light, 18:00-05:59 Dark) if no lux reading is available.
 * 2. AUTO_SYSTEM: Follows Android OS dark/light mode automatically.
 * 3. LIGHT: Forced High-Contrast Daylight Tarmac mode.
 * 4. DARK: Forced Airside Night Ops mode.
 */
@Composable
fun AdaptiveThemeProvider(
    selectedMode: ThemeMode,
    onSelectMode: (ThemeMode) -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    var ambientLux by remember { mutableStateOf<Float?>(null) }
    var sensorDarkDecision by remember { mutableStateOf<Boolean?>(null) }
    var currentHour by remember { mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }

    // Periodic schedule check every 60 seconds for automatic day/night apron transitions
    LaunchedEffect(selectedMode) {
        if (selectedMode == ThemeMode.AUTO_APRON) {
            while (true) {
                currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                delay(60_000L)
            }
        }
    }

    // Real hardware ambient light sensor registration when AUTO_APRON is selected
    DisposableEffect(context, selectedMode) {
        if (selectedMode != ThemeMode.AUTO_APRON) {
            ambientLux = null
            sensorDarkDecision = null
            onDispose { }
        } else {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
            if (sensorManager == null || lightSensor == null) {
                ambientLux = null
                sensorDarkDecision = null
                onDispose { }
            } else {
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        val lux = event?.values?.firstOrNull() ?: return
                        if (lux.isNaN() || lux < 0f) return
                        ambientLux = lux
                        // Hysteresis thresholds prevent flickering when walking near bridge doorways
                        sensorDarkDecision = when {
                            lux >= 220f -> false // Bright apron daylight -> Light mode
                            lux <= 70f -> true   // Dim aerobridge / night apron -> Dark mode
                            else -> sensorDarkDecision ?: (lux < 140f)
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
                onDispose {
                    sensorManager.unregisterListener(listener)
                }
            }
        }
    }

    val scheduleIsDark = currentHour < 6 || currentHour >= 18

    val (resolvedDark, reasonLabel) = when (selectedMode) {
        ThemeMode.LIGHT -> false to "Dipilih manual: Daylight Terang"
        ThemeMode.DARK -> true to "Dipilih manual: Airside Gelap"
        ThemeMode.AUTO_SYSTEM -> systemDark to if (systemDark) {
            "Otomatis Sistem: Mode Gelap"
        } else {
            "Otomatis Sistem: Mode Terang"
        }
        ThemeMode.AUTO_APRON -> {
            val lux = ambientLux
            val sensorDecision = sensorDarkDecision
            if (lux != null && sensorDecision != null) {
                sensorDecision to if (sensorDecision) {
                    "Sensor Cahaya (${lux.toInt()} lux) · Mode Gelap"
                } else {
                    "Sensor Cahaya (${lux.toInt()} lux) · Mode Terang"
                }
            } else {
                scheduleIsDark to if (scheduleIsDark) {
                    "Jadwal Apron Malam (18:00–06:00) · Mode Gelap"
                } else {
                    "Jadwal Apron Siang (06:00–18:00) · Mode Terang"
                }
            }
        }
    }

    val controllerState = remember(selectedMode, resolvedDark, ambientLux, reasonLabel, onSelectMode) {
        ThemeControllerState(
            selectedMode = selectedMode,
            isDarkActive = resolvedDark,
            ambientLux = ambientLux,
            activeReason = reasonLabel,
            onSelectMode = onSelectMode
        )
    }

    CompositionLocalProvider(LocalThemeController provides controllerState) {
        GarbarataOpsTheme(darkTheme = resolvedDark, content = content)
    }
}
