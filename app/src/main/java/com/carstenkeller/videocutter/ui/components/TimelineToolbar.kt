package com.carstenkeller.videocutter.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TimelineToolbar(
    exportEnabled: Boolean,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
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
        IconActionButton(icon = Icons.Filled.ContentCut, label = "Schneiden", onClick = onSplit)
        IconActionButton(icon = Icons.Filled.Delete, label = "Löschen", onClick = onDelete)
        IconActionButton(icon = Icons.Filled.VideoLibrary, label = "Clip hinzufügen", onClick = onAddClip)
        IconActionButton(icon = Icons.Filled.Videocam, label = "Aufnehmen", onClick = onRecordClip)
        IconActionButton(icon = Icons.Filled.Save, label = "Exportieren", onClick = onExport, enabled = exportEnabled)
    }
}
