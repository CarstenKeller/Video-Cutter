package com.carstenkeller.videocutter.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carstenkeller.videocutter.R

/**
 * Projektweite Aktionen (nicht auf eine Position in der Zeitleiste
 * bezogen). Positionsbezogene Aktionen (Schneiden, Löschen, Ausschnitt
 * extrahieren) sitzen stattdessen direkt bei der Zeitleiste/dem
 * Trimm-Streifen - siehe `TimelineView`.
 */
@Composable
fun TimelineToolbar(
    exportEnabled: Boolean,
    onAddClip: () -> Unit,
    onRecordClip: () -> Unit,
    onExport: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconActionButton(icon = R.drawable.ic_add_clip, label = "Clip hinzufügen", onClick = onAddClip)
        IconActionButton(icon = R.drawable.ic_videocam, label = "Aufnehmen", onClick = onRecordClip)
        IconActionButton(icon = R.drawable.ic_export, label = "Exportieren", onClick = onExport, enabled = exportEnabled)
    }
}
