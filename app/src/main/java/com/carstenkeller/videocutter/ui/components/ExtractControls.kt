package com.carstenkeller.videocutter.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carstenkeller.videocutter.R
import com.carstenkeller.videocutter.extract.ExtractDurationPreset
import com.carstenkeller.videocutter.extract.ExtractFormat

/**
 * Längen-/Formatauswahl für den Schnellausschnitt, direkt auf der
 * Hauptseite statt in einem Dialog (Nutzerwunsch). Die Position des
 * Ausschnitts wird nicht hier, sondern per Zieh-Fenster direkt auf der
 * Zeitleiste festgelegt (siehe `TimelineView`/`ExtractWindow`).
 *
 * Bewusst eine einzige, horizontal scrollbare Zeile ohne Überschriften
 * oder Erklärtext (Nutzerfeedback: "nimmt die Hälfte des Bildschirms
 * ein... keinen Erklärtext und keine zwei Zeilen für Überschriften") -
 * nur bei benutzerdefinierter Länge kommt eine zweite, ebenso knappe
 * Zeile für den Schieberegler dazu.
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

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExtractDurationPreset.entries.forEach { option ->
                FilterChip(
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
                label = { Text("…") },
            )

            Spacer(modifier = Modifier.width(4.dp))

            ExtractFormat.entries.forEach { option ->
                val enabled = option != ExtractFormat.GIF || allowsGif
                FilterChip(
                    selected = format == option,
                    enabled = enabled,
                    onClick = { onFormatSelected(option) },
                    label = { Text(option.label) },
                )
            }

            IconActionButton(icon = R.drawable.ic_close, label = "Abbrechen", onClick = onCancel)
            IconActionButton(icon = R.drawable.ic_check, label = "Extrahieren", onClick = onExtract)
        }

        if (preset == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("%.1fs".format(customDurationSeconds), style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = customDurationSeconds,
                    onValueChange = onCustomDurationChange,
                    valueRange = longPresetSeconds..videoDurationSeconds.coerceAtLeast(longPresetSeconds + 0.01f),
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
            }
        }
    }
}
