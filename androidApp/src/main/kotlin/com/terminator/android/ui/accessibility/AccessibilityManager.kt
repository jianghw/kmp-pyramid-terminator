package com.terminator.android.ui.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

enum class FontScalePreset(val label: String, val scale: Float) {
    SMALL("小", 0.85f),
    DEFAULT("标准", 1.0f),
    LARGE("大", 1.15f),
    EXTRA_LARGE("特大", 1.3f),
    HUGE("超大", 1.5f)
}

@Stable
class AccessibilityPreferences {
    var fontScale by mutableFloatStateOf(FontScalePreset.DEFAULT.scale)
        private set

    var isVoiceAnnouncementEnabled by mutableStateOf(true)
        private set

    var isHighContrastMode by mutableStateOf(false)
        private set

    var isGuideCompleted by mutableStateOf(false)
        private set

    fun updateFontScale(scale: Float) {
        fontScale = scale.coerceIn(0.8f, 2.0f)
    }

    fun setFontScalePreset(preset: FontScalePreset) {
        fontScale = preset.scale
    }

    fun toggleVoiceAnnouncement() {
        isVoiceAnnouncementEnabled = !isVoiceAnnouncementEnabled
    }

    fun setVoiceAnnouncement(enabled: Boolean) {
        isVoiceAnnouncementEnabled = enabled
    }

    fun setHighContrast(enabled: Boolean) {
        isHighContrastMode = enabled
    }

    fun markGuideCompleted() {
        isGuideCompleted = true
    }

    fun resetGuide() {
        isGuideCompleted = false
    }

    fun scaledSp(baseSp: TextUnit): TextUnit {
        return (baseSp.value * fontScale).sp
    }
}

@Composable
fun ProvideAccessibilityDensity(
    preferences: AccessibilityPreferences,
    content: @Composable () -> Unit
) {
    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * preferences.fontScale
    )
    androidx.compose.runtime.CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        content = content
    )
}
