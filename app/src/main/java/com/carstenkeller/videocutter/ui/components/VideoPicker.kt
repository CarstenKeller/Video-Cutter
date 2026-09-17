package com.carstenkeller.videocutter.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.carstenkeller.videocutter.media.MediaDurationReader

/**
 * Startet den systemeigenen Foto-/Video-Picker (kein Runtime-Permission-Dialog
 * nötig) und liefert die gewählte Video-Uri plus ihre Dauer in Mikrosekunden.
 */
@Composable
fun rememberVideoPickerLauncher(onPicked: (Uri, Long) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onPicked(uri, MediaDurationReader.readDurationUs(context, uri))
        }
    }
    return {
        launcher.launch(PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.VideoOnly))
    }
}
