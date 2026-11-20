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
 * (GIF oder MP4). [anchorPositionUs] ist die Position, an der der Nutzer in
 * die Timeline/Vorschau getippt hat (relativ zum sichtbaren Clip-Ausschnitt),
 * [clipDurationUs] die Gesamtlänge des sichtbaren Clip-Ausschnitts – beide in
 * Mikrosekunden. [onConfirm] liefert Start/Ende ebenfalls relativ zum
 * sichtbaren Clip-Ausschnitt zurück (der Aufrufer rechnet das bei Bedarf auf
 * absolute Quelldatei-Zeit um).
 */
@Composable
fun QuickExtractDialog(
    clipDurationUs: Long,
    anchorPositionUs: Long,
    onDismiss: () -> Unit,
    onConfirm: (windowStartUs: Long, windowEndUs: Long, format: ExtractFormat) -> Unit,
) {
    val clipDurationSeconds = (clipDurationUs / 1_000_000f).coerceAtLeast(0.2f)

    fun maxStartSeconds(preset: ExtractDurationPreset) =
        (clipDurationSeconds - preset.seconds).coerceAtLeast(0f)

    var preset by remember { mutableStateOf(ExtractDurationPreset.MEDIUM) }
    var format by remember { mutableStateOf(ExtractFormat.GIF) }
    var startSeconds by remember {
        mutableStateOf(
            ((anchorPositionUs / 1_000_000f) - ExtractDurationPreset.MEDIUM.seconds / 2f)
                .coerceIn(0f, maxStartSeconds(ExtractDurationPreset.MEDIUM)),
        )
    }

    val endSeconds = (startSeconds + preset.seconds).coerceAtMost(clipDurationSeconds)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ausschnitt extrahieren") },
        text = {
            Column {
                Text("Länge", style = MaterialTheme.typography.labelLarge)
                Row {
                    ExtractDurationPreset.entries.forEach { option ->
                        val enabled = option.seconds <= clipDurationSeconds
                        FilterChip(
                            modifier = Modifier.padding(end = 8.dp),
                            selected = preset == option,
                            enabled = enabled,
                            onClick = {
                                preset = option
                                startSeconds = startSeconds.coerceIn(0f, maxStartSeconds(option))
                                if (!option.allowsGif && format == ExtractFormat.GIF) {
                                    format = ExtractFormat.MP4
                                }
                            },
                            label = { Text("${option.seconds}s") },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Format", style = MaterialTheme.typography.labelLarge)
                Row {
                    ExtractFormat.entries.forEach { option ->
                        val enabled = option != ExtractFormat.GIF || preset.allowsGif
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(enabled = enabled) { format = option },
                        ) {
                            RadioButton(selected = format == option, onClick = { format = option }, enabled = enabled)
                            Text(option.label)
                        }
                    }
                }
                if (!preset.allowsGif) {
                    Text(
                        "GIF ist bei ${preset.seconds}s nicht verfügbar (Datei würde zu groß/langsam werden).",
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
                    onValueChange = { startSeconds = it.coerceIn(0f, maxStartSeconds(preset)) },
                    valueRange = 0f..maxStartSeconds(preset).coerceAtLeast(0.01f),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val startUs = (startSeconds * 1_000_000).toLong()
                val endUs = ((startSeconds + preset.seconds) * 1_000_000).toLong().coerceAtMost(clipDurationUs)
                onConfirm(startUs, endUs, format)
            }) { Text("Extrahieren") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}
