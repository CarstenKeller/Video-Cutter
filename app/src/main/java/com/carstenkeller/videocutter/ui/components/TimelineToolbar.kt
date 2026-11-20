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

@Composable
fun TimelineToolbar(
    exportEnabled: Boolean,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
    onAddClip: () -> Unit,
    onRecordClip: () -> Unit,
    onQuickExtract: () -> Unit,
    onExport: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconActionButton(icon = R.drawable.ic_cut, label = "Schneiden", onClick = onSplit)
        IconActionButton(icon = R.drawable.ic_delete, label = "Löschen", onClick = onDelete)
        IconActionButton(icon = R.drawable.ic_add_clip, label = "Clip hinzufügen", onClick = onAddClip)
        IconActionButton(icon = R.drawable.ic_videocam, label = "Aufnehmen", onClick = onRecordClip)
        IconActionButton(icon = R.drawable.ic_gif, label = "Ausschnitt extrahieren", onClick = onQuickExtract)
        IconActionButton(icon = R.drawable.ic_export, label = "Exportieren", onClick = onExport, enabled = exportEnabled)
    }
}
