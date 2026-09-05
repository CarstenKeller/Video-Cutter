package com.carstenkeller.videocutter.ui.components

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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
            onPicked(uri, readVideoDurationUs(context, uri))
        }
    }
    return {
        launcher.launch(PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.VideoOnly))
    }
}

private fun readVideoDurationUs(context: Context, uri: Uri): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val durationMs = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
        durationMs * 1000
    } finally {
        retriever.release()
    }
}
