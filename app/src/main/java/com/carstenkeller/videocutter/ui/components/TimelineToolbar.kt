package com.carstenkeller.videocutter.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TimelineToolbar(
    exportEnabled: Boolean,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
    onAddClip: () -> Unit,
    onExport: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onSplit) { Text("Schneiden") }
        OutlinedButton(onClick = onDelete) { Text("Löschen") }
        OutlinedButton(onClick = onAddClip) { Text("+ Clip") }
        Button(onClick = onExport, enabled = exportEnabled) { Text("Exportieren") }
    }
}
