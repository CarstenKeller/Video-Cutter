package com.carstenkeller.videocutter.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carstenkeller.videocutter.ui.components.rememberVideoPickerLauncher

@Composable
fun HomeScreen(onVideoImported: (Uri, Long) -> Unit) {
    val launchPicker = rememberVideoPickerLauncher(onPicked = onVideoImported)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(text = "Video Cutter", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Wähle ein Video, um mit dem Schnitt zu beginnen.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = launchPicker) {
            Text("Video auswählen")
        }
    }
}
