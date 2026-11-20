package com.carstenkeller.videocutter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.carstenkeller.videocutter.extract.ExtractDurationPreset
import com.carstenkeller.videocutter.extract.ExtractFormat

/**
 * Längen-/Formatauswahl für den Schnellausschnitt, direkt auf der
 * Hauptseite statt in einem Dialog (Nutzerwunsch). Die Position des
 * Ausschnitts wird nicht hier, sondern per Zieh-Fenster direkt auf der
 * Zeitleiste festgelegt (siehe `TimelineView`/`ExtractWindow`).
 */
@Composable
fun ExtractControls(
    videoDurationUs: Long,
    preset: ExtractDurationPreset?,
    customDurationSeconds: Float,
    format: ExtractFormat,
    onPresetSelected: (ExtractDurationPreset?) -> Unit,
    onCustomDurationChange: (Float) -> Unit,
    onFormatSelected: (ExtractFormat) -> Unit,
    onCancel: () -> Unit,
    onExtract: () -> Unit,
) {
    val videoDurationSeconds = (videoDurationUs / 1_000_000f).coerceAtLeast(0.2f)
    val longPresetSeconds = ExtractDurationPreset.LONG.seconds.toFloat()
    val customAvailable = videoDurationSeconds > longPresetSeconds
    val allowsGif = preset?.allowsGif ?: false

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
    ) {
        Text("Ausschnitt extrahieren", style = MaterialTheme.typography.titleMedium)
        Text(
            "Fenster auf der Zeitleiste verschieben, dann extrahieren.",
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(8.dp))
        Text("Länge", style = MaterialTheme.typography.labelLarge)
        Row {
            ExtractDurationPreset.entries.forEach { option ->
                FilterChip(
                    modifier = Modifier.padding(end = 8.dp),
                    selected = preset == option,
                    enabled = option.seconds <= videoDurationSeconds,
                    onClick = { onPresetSelected(option) },
                    label = { Text("${option.seconds}s") },
                )
            }
            FilterChip(
                selected = preset == null,
                enabled = customAvailable,
                onClick = { onPresetSelected(null) },
                label = { Text("Länger…") },
            )
        }

        if (preset == null) {
            Spacer(Modifier.height(8.dp))
            Text("Dauer: %.1fs".format(customDurationSeconds), style = MaterialTheme.typography.bodySmall)
            Slider(
                value = customDurationSeconds,
                onValueChange = onCustomDurationChange,
                valueRange = longPresetSeconds..videoDurationSeconds.coerceAtLeast(longPresetSeconds + 0.01f),
            )
        }

        Spacer(Modifier.height(12.dp))
        Text("Format", style = MaterialTheme.typography.labelLarge)
        Row {
            ExtractFormat.entries.forEach { option ->
                val enabled = option != ExtractFormat.GIF || allowsGif
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = enabled) { onFormatSelected(option) },
                ) {
                    RadioButton(selected = format == option, onClick = { onFormatSelected(option) }, enabled = enabled)
                    Text(option.label)
                }
            }
        }
        if (!allowsGif) {
            val lengthLabel = preset?.let { "${it.seconds}s" } ?: "benutzerdefinierter Länge"
            Text(
                "GIF ist bei $lengthLabel nicht verfügbar (Datei würde zu groß/langsam werden).",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Abbrechen") }
            TextButton(onClick = onExtract) { Text("Extrahieren") }
        }
    }
}
