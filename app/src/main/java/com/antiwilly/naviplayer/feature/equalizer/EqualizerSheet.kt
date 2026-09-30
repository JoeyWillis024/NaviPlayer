package com.antiwilly.naviplayer.feature.equalizer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    onDismissRequest: () -> Unit,
    onPresetSelected: (String) -> Unit,
    onBassBoostChanged: (Int) -> Unit,
    onEqualizerEnabledChanged: (Boolean) -> Unit,
    currentPreset: String = "Flat",
    currentBassBoost: Int = 0,
    isEqualizerEnabled: Boolean = false
) {
    var selectedPreset by remember { mutableStateOf(currentPreset) }
    var bassBoostValue by remember { mutableFloatStateOf(currentBassBoost.toFloat()) }
    var equalizerEnabled by remember { mutableStateOf(isEqualizerEnabled) }

    val presets = listOf("Flat", "Bass Boost", "Rock", "Pop", "Jazz", "Electronic", "Vocal")

    ModalBottomSheet(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Audio Equalizer",
                style = MaterialTheme.typography.titleLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Equalizer", modifier = Modifier.weight(1f))
                Switch(
                    checked = equalizerEnabled,
                    onCheckedChange = {
                        equalizerEnabled = it
                        onEqualizerEnabledChanged(it)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Sound Profiles",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { preset ->
                    FilterChip(
                        selected = selectedPreset == preset,
                        onClick = {
                            selectedPreset = preset
                            equalizerEnabled = true
                            onEqualizerEnabledChanged(true)
                            onPresetSelected(preset)
                        },
                        label = { Text(preset) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bass Boost",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(bassBoostValue / 10).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = bassBoostValue,
                onValueChange = {
                    bassBoostValue = it
                    onBassBoostChanged(it.toInt())
                },
                valueRange = 0f..1000f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
