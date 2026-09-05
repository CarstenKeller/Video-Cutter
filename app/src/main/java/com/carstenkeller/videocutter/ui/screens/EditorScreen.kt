package com.carstenkeller.videocutter.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.carstenkeller.videocutter.export.ExportUiState
import com.carstenkeller.videocutter.export.ExportViewModel
import com.carstenkeller.videocutter.timeline.TimelineViewModel
import com.carstenkeller.videocutter.ui.components.ExportDialog
import com.carstenkeller.videocutter.ui.components.ExportProgressOverlay
import com.carstenkeller.videocutter.ui.components.PlayerPreview
import com.carstenkeller.videocutter.ui.components.TimelineToolbar
import com.carstenkeller.videocutter.ui.components.TimelineView
import com.carstenkeller.videocutter.ui.components.rememberVideoPickerLauncher

@OptIn(UnstableApi::class)
@Composable
fun EditorScreen(timelineViewModel: TimelineViewModel) {
    val context = LocalContext.current
    val state by timelineViewModel.state.collectAsState()
    val exportViewModel: ExportViewModel = viewModel()
    val exportState by exportViewModel.uiState.collectAsState()

    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    LaunchedEffect(state.clips) {
        if (state.clips.isEmpty()) return@LaunchedEffect
        val mediaItems = state.clips.map { clip ->
            MediaItem.Builder()
                .setUri(clip.sourceUri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.trimStartUs / 1000)
                        .setEndPositionMs(clip.trimEndUs / 1000)
                        .build(),
                )
                .build()
        }
        player.setMediaItems(mediaItems)
        player.prepare()
    }

    var showExportDialog by remember { mutableStateOf(false) }
    val addClipLauncher = rememberVideoPickerLauncher(onPicked = timelineViewModel::addClip)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlayerPreview(
                player = player,
                modifier = Modifier.weight(1f).background(Color.Black),
            )

            TimelineToolbar(
                exportEnabled = state.clips.isNotEmpty() && exportState !is ExportUiState.Exporting,
                onSplit = {
                    val index = player.currentMediaItemIndex
                    val clip = state.clips.getOrNull(index) ?: return@TimelineToolbar
                    timelineViewModel.splitClip(clip.id, player.currentPosition * 1000)
                },
                onDelete = { state.selectedClipId?.let(timelineViewModel::deleteClip) },
                onAddClip = addClipLauncher,
                onExport = { showExportDialog = true },
            )

            TimelineView(
                clips = state.clips,
                selectedClipId = state.selectedClipId,
                onSelectClip = { clipId ->
                    timelineViewModel.selectClip(clipId)
                    val index = state.clips.indexOfFirst { it.id == clipId }
                    if (index >= 0) player.seekTo(index, 0)
                },
                onTrimChange = timelineViewModel::updateTrim,
                onMoveClip = timelineViewModel::moveClip,
            )
        }

        val currentExportState = exportState
        if (currentExportState is ExportUiState.Exporting) {
            ExportProgressOverlay(progress = currentExportState.progress)
        }
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onConfirm = { resolution, quality ->
                showExportDialog = false
                exportViewModel.startExport(state.clips, resolution, quality)
            },
        )
    }

    LaunchedEffect(exportState) {
        when (val current = exportState) {
            is ExportUiState.Success -> {
                Toast.makeText(context, current.message, Toast.LENGTH_LONG).show()
                exportViewModel.consumeMessage()
            }
            is ExportUiState.Error -> {
                Toast.makeText(context, current.message, Toast.LENGTH_LONG).show()
                exportViewModel.consumeMessage()
            }
            else -> Unit
        }
    }
}
