package com.carstenkeller.videocutter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.carstenkeller.videocutter.R
import com.carstenkeller.videocutter.extract.ExtractDurationPreset
import com.carstenkeller.videocutter.extract.ExtractFormat
import kotlin.math.roundToInt

private const val CUSTOM_DURATION_STEP_SECONDS = 5f

/**
 * Längen-/Formatauswahl für den Schnellausschnitt, direkt auf der
 * Hauptseite statt in einem Dialog (Nutzerwunsch). Die Position des
 * Ausschnitts wird nicht hier, sondern per Zieh-Fenster direkt auf der
 * Zeitleiste festgelegt (siehe `TimelineView`/`ExtractWindow`).
 *
 * Bewusst zwei feste, nicht scrollende Zeilen ohne Überschriften oder
 * Erklärtext (Nutzerfeedback): Zeile 1 Längen-Presets + Stepper für eine
 * frei wählbare Länge (ersetzt den vorherigen Schieberegler, der "sehr
 * fummelig und nicht exakt zu bedienen" war), Zeile 2 Format-Auswahl +
 * Abbrechen/Extrahieren - alles ohne horizontales Scrollen erreichbar.
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
    val effectiveDurationSeconds = preset?.seconds?.toFloat() ?: customDurationSeconds

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            DurationStepper(
                enabled = customAvailable,
                isActive = preset == null,
                seconds = effectiveDurationSeconds,
                onChange = { newSeconds ->
                    onCustomDurationChange(newSeconds.coerceIn(longPresetSeconds, videoDurationSeconds))
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExtractFormat.entries.forEach { option ->
                val enabled = option != ExtractFormat.GIF || allowsGif
                FilterChip(
                    selected = format == option,
                    enabled = enabled,
                    onClick = { onFormatSelected(option) },
                    label = { Text(option.label) },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconActionButton(icon = R.drawable.ic_close, label = "Abbrechen", onClick = onCancel)
            IconActionButton(icon = R.drawable.ic_check, label = "Extrahieren", onClick = onExtract)
        }
    }
}

/** Kompakter Ersatz für den vorherigen Schieberegler: Dauer-Anzeige mit Rauf-/Runter-Buttons. */
@Composable
private fun DurationStepper(
    enabled: Boolean,
    isActive: Boolean,
    seconds: Float,
    onChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(label = "−", enabled = enabled) { onChange(seconds - CUSTOM_DURATION_STEP_SECONDS) }
        Text(
            "${seconds.roundToInt()}s",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        StepperButton(label = "+", enabled = enabled) { onChange(seconds + CUSTOM_DURATION_STEP_SECONDS) }
    }
}

@Composable
private fun StepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
