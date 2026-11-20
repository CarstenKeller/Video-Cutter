package com.carstenkeller.videocutter.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carstenkeller.videocutter.extract.ExtractDurationPreset
import com.carstenkeller.videocutter.extract.ExtractFormat

/**
 * Dialog zum Festlegen von Länge, Position und Format eines Schnellausschnitts
 * (GIF oder MP4). Steht gegen die komplette Videospur zur Verfügung (die
 * gesamte Quelldatei), nicht nur gegen den aktuell in der Timeline
 * eingestellten Trimm-Ausschnitt – [videoDurationUs] ist daher die
 * Gesamtlänge der Quelldatei, [anchorPositionUs] die Position, an der der
 * Nutzer in der Vorschau getippt hat (beide absolut innerhalb der
 * Quelldatei, in Mikrosekunden). [onConfirm] liefert Start/Ende ebenfalls
 * als absolute Position innerhalb der Quelldatei zurück.
 *
 * Länge ist entweder einer der festen Presets (2s/5s/10s) oder, darüber
 * hinaus, eine frei wählbare ("Länger…") Dauer bis zur vollen Videolänge –
 * GIF bleibt dabei bewusst auf die kurzen Presets beschränkt
 * (Dateigröße/Performance), MP4 erlaubt auch lange, frei gewählte Ausschnitte.
 */
@Composable
fun QuickExtractDialog(
    videoDurationUs: Long,
    anchorPositionUs: Long,
    onDismiss: () -> Unit,
    onConfirm: (windowStartUs: Long, windowEndUs: Long, format: ExtractFormat) -> Unit,
) {
    val videoDurationSeconds = (videoDurationUs / 1_000_000f).coerceAtLeast(0.2f)
    val longPresetSeconds = ExtractDurationPreset.LONG.seconds.toFloat()
    val customAvailable = videoDurationSeconds > longPresetSeconds

    fun maxStartSeconds(durationSeconds: Float) =
        (videoDurationSeconds - durationSeconds).coerceAtLeast(0f)

    var preset by remember { mutableStateOf<ExtractDurationPreset?>(ExtractDurationPreset.MEDIUM) }
    var customDurationSeconds by remember {
        mutableStateOf(longPresetSeconds.coerceAtMost(videoDurationSeconds))
    }
    var format by remember { mutableStateOf(ExtractFormat.GIF) }

    val durationSeconds = preset?.seconds?.toFloat() ?: customDurationSeconds
    val allowsGif = preset?.allowsGif ?: false

    var startSeconds by remember {
        mutableStateOf(
            ((anchorPositionUs / 1_000_000f) - ExtractDurationPreset.MEDIUM.seconds / 2f)
                .coerceIn(0f, maxStartSeconds(ExtractDurationPreset.MEDIUM.seconds.toFloat())),
        )
    }

    val endSeconds = (startSeconds + durationSeconds).coerceAtMost(videoDurationSeconds)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ausschnitt extrahieren") },
        text = {
            Column {
                Text("Länge", style = MaterialTheme.typography.labelLarge)
                Row {
                    ExtractDurationPreset.entries.forEach { option ->
                        val enabled = option.seconds <= videoDurationSeconds
                        FilterChip(
                            modifier = Modifier.padding(end = 8.dp),
                            selected = preset == option,
                            enabled = enabled,
                            onClick = {
                                preset = option
                                startSeconds = startSeconds.coerceIn(0f, maxStartSeconds(option.seconds.toFloat()))
                                if (!option.allowsGif && format == ExtractFormat.GIF) {
                                    format = ExtractFormat.MP4
                                }
                            },
                            label = { Text("${option.seconds}s") },
                        )
                    }
                    FilterChip(
                        selected = preset == null,
                        enabled = customAvailable,
                        onClick = {
                            preset = null
                            if (format == ExtractFormat.GIF) format = ExtractFormat.MP4
                            customDurationSeconds = customDurationSeconds.coerceIn(longPresetSeconds, videoDurationSeconds)
                            startSeconds = startSeconds.coerceIn(0f, maxStartSeconds(customDurationSeconds))
                        },
                        label = { Text("Länger…") },
                    )
                }

                if (preset == null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Dauer: %.1fs".format(customDurationSeconds),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Slider(
                        value = customDurationSeconds,
                        onValueChange = {
                            customDurationSeconds = it.coerceIn(longPresetSeconds, videoDurationSeconds)
                            startSeconds = startSeconds.coerceIn(0f, maxStartSeconds(customDurationSeconds))
                        },
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
                            modifier = Modifier.clickable(enabled = enabled) { format = option },
                        ) {
                            RadioButton(selected = format == option, onClick = { format = option }, enabled = enabled)
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

                Spacer(Modifier.height(12.dp))
                Text(
                    "Position: %.1fs – %.1fs".format(startSeconds, endSeconds),
                    style = MaterialTheme.typography.bodySmall,
                )
                Slider(
                    value = startSeconds,
                    onValueChange = { startSeconds = it.coerceIn(0f, maxStartSeconds(durationSeconds)) },
                    valueRange = 0f..maxStartSeconds(durationSeconds).coerceAtLeast(0.01f),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val startUs = (startSeconds * 1_000_000).toLong()
                val endUs = ((startSeconds + durationSeconds) * 1_000_000).toLong().coerceAtMost(videoDurationUs)
                onConfirm(startUs, endUs, format)
            }) { Text("Extrahieren") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}
