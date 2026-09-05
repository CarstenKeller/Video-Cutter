package com.carstenkeller.videocutter.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.carstenkeller.videocutter.export.ExportQuality
import com.carstenkeller.videocutter.export.ExportResolution

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onConfirm: (ExportResolution, ExportQuality) -> Unit,
) {
    var resolution by remember { mutableStateOf(ExportResolution.ORIGINAL) }
    var quality by remember { mutableStateOf(ExportQuality.ORIGINAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Video exportieren") },
        text = {
            Column {
                Text("Auflösung", style = MaterialTheme.typography.labelLarge)
                ExportResolution.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { resolution = option },
                    ) {
                        RadioButton(selected = resolution == option, onClick = { resolution = option })
                        Text(option.label)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Qualität / Kompression", style = MaterialTheme.typography.labelLarge)
                ExportQuality.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { quality = option },
                    ) {
                        RadioButton(selected = quality == option, onClick = { quality = option })
                        Text(option.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(resolution, quality) }) { Text("Exportieren") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}
