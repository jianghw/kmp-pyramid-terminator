package com.terminator.android.ui.accessibility

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsScreen(
    preferences: AccessibilityPreferences,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("无障碍设置") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics {
                            contentDescription = "返回上一页"
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            FontScaleSection(preferences)

            HorizontalDivider()

            VoiceAnnouncementSection(preferences)

            HorizontalDivider()

            HighContrastSection(preferences)

            HorizontalDivider()

            GuideSection(preferences)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "字体预览：当前缩放比例 ${(preferences.fontScale * 100).toInt()}%",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = (MaterialTheme.typography.headlineLarge.fontSize.value * preferences.fontScale).sp
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics {
                    contentDescription = "字体预览，当前缩放比例百分之${(preferences.fontScale * 100).toInt()}"
                }
            )

            Text(
                text = "这是一段示例文本，用于展示字体大小调整后的效果。您可以通过上方的滑块或快捷按钮来调整字体大小。",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * preferences.fontScale).sp
                )
            )
        }
    }
}

@Composable
private fun FontScaleSection(preferences: AccessibilityPreferences) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "字体大小",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics {
                contentDescription = "字体大小设置"
            }
        )

        Text(
            text = "缩放比例：${(preferences.fontScale * 100).toInt()}%",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Slider(
            value = preferences.fontScale,
            onValueChange = { preferences.updateFontScale(it) },
            valueRange = 0.8f..2.0f,
            steps = 11,
            modifier = Modifier.semantics {
                contentDescription = "字体缩放滑块，当前值${(preferences.fontScale * 100).toInt()}%"
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FontScalePreset.entries.forEach { preset ->
                FilterChip(
                    selected = preferences.fontScale == preset.scale,
                    onClick = { preferences.setFontScalePreset(preset) },
                    label = { Text(preset.label) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = "设置字体为${preset.label}，缩放比例${(preset.scale * 100).toInt()}%"
                        }
                )
            }
        }
    }
}

@Composable
private fun VoiceAnnouncementSection(preferences: AccessibilityPreferences) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "语音播报",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "任务完成、风险预警时自动语音提示",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = preferences.isVoiceAnnouncementEnabled,
            onCheckedChange = { preferences.setVoiceAnnouncement(it) },
            modifier = Modifier.semantics {
                contentDescription = if (preferences.isVoiceAnnouncementEnabled) "语音播报已开启，点击关闭" else "语音播报已关闭，点击开启"
            }
        )
    }
}

@Composable
private fun HighContrastSection(preferences: AccessibilityPreferences) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "高对比度模式",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "增强文字与背景的对比度，便于阅读",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = preferences.isHighContrastMode,
            onCheckedChange = { preferences.setHighContrast(it) },
            modifier = Modifier.semantics {
                contentDescription = if (preferences.isHighContrastMode) "高对比度模式已开启，点击关闭" else "高对比度模式已关闭，点击开启"
            }
        )
    }
}

@Composable
private fun GuideSection(preferences: AccessibilityPreferences) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "新手引导",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "重新播放应用使用引导",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = { preferences.resetGuide() },
            modifier = Modifier.semantics {
                contentDescription = "重新播放新手引导"
            }
        ) {
            Text("重播引导")
        }
    }
}
