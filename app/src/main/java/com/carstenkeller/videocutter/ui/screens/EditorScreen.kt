package com.carstenkeller.videocutter.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.carstenkeller.videocutter.extract.ExtractUiState
import com.carstenkeller.videocutter.extract.ExtractViewModel
import com.carstenkeller.videocutter.timeline.TimelineViewModel
import com.carstenkeller.videocutter.ui.components.ExportDialog
import com.carstenkeller.videocutter.ui.components.ExportProgressOverlay
import com.carstenkeller.videocutter.ui.components.PlayerPreview
import com.carstenkeller.videocutter.ui.components.QuickExtractDialog
import com.carstenkeller.videocutter.ui.components.TimelineToolbar
import com.carstenkeller.videocutter.ui.components.TimelineView
import com.carstenkeller.videocutter.ui.components.rememberVideoPickerLauncher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun EditorScreen(
    timelineViewModel: TimelineViewModel,
    onRecordRequested: () -> Unit,
) {
    val context = LocalContext.current
    val state by timelineViewModel.state.collectAsState()
    val exportViewModel: ExportViewModel = viewModel()
    val exportState by exportViewModel.uiState.collectAsState()
    val extractViewModel: ExtractViewModel = viewModel()
    val extractState by extractViewModel.uiState.collectAsState()

    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    // Debounced statt bei jedem einzelnen Trimm-Griff-Tick: sonst baut jede
    // Fingerbewegung auf der Zeitleiste die komplette Playlist neu auf
    // (setMediaItems + prepare), was die App spürbar träge macht.
    LaunchedEffect(timelineViewModel) {
        var pendingRebuild: Job? = null
        var loadedClips = timelineViewModel.state.value.clips
        timelineViewModel.state.collect { current ->
            pendingRebuild?.cancel()
            pendingRebuild = launch {
                delay(250)
                if (current.clips.isEmpty()) return@launch

                // setMediaItems(List) allein würde die Wiedergabe immer auf den
                // Projektanfang zurücksetzen. Stattdessen: aktuelle Position als
                // "verstrichene Zeit seit Projektanfang" (bezogen auf die noch
                // geladene Clip-Liste) sichern und nach dem Neuaufbau an der
                // äquivalenten Stelle im neuen Clip-Satz fortsetzen - sonst würde
                // z. B. jeder Trimm-Zug die Vorschau an den Anfang zurückspringen
                // lassen.
                val elapsedBeforeUs = loadedClips.take(player.currentMediaItemIndex).sumOf { it.durationUs } +
                    player.currentPosition * 1000

                val mediaItems = current.clips.map { clip ->
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

                var remainingUs = elapsedBeforeUs
                var targetIndex = 0
                for (i in current.clips.indices) {
                    val duration = current.clips[i].durationUs
                    if (i == current.clips.lastIndex || remainingUs < duration) {
                        targetIndex = i
                        break
                    }
                    remainingUs -= duration
                }
                val targetPositionMs = remainingUs.coerceIn(0, current.clips[targetIndex].durationUs) / 1000

                player.setMediaItems(mediaItems, targetIndex, targetPositionMs)
                player.prepare()
                loadedClips = current.clips
            }
        }
    }

    var showExportDialog by remember { mutableStateOf(false) }
    var showQuickExtractDialog by remember { mutableStateOf(false) }
    val addClipLauncher = rememberVideoPickerLauncher(onPicked = timelineViewModel::addClip)

    // Playhead-Position fürs Timeline-Lineal: ExoPlayer liefert keinen
    // Compose-State für currentPosition, daher wird hier regelmäßig abgefragt
    // (reicht für eine Positionsanzeige, muss nicht jeden Frame aktuell sein).
    var playheadClipIndex by remember { mutableStateOf(0) }
    var playheadPositionUs by remember { mutableStateOf(0L) }
    LaunchedEffect(player) {
        while (true) {
            playheadClipIndex = player.currentMediaItemIndex
            playheadPositionUs = player.currentPosition * 1000
            delay(200)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlayerPreview(
                player = player,
                modifier = Modifier.weight(1f).background(Color.Black),
            )

            TimelineToolbar(
                exportEnabled = state.clips.isNotEmpty() && exportState !is ExportUiState.Exporting,
                onAddClip = addClipLauncher,
                onRecordClip = onRecordRequested,
                onExport = { showExportDialog = true },
            )

            TimelineView(
                clips = state.clips,
                playheadClipIndex = playheadClipIndex,
                playheadPositionUs = playheadPositionUs,
                onSeek = { index, positionInClipUs -> player.seekTo(index, positionInClipUs / 1000) },
                onTrimChange = timelineViewModel::updateTrim,
                onMoveClip = timelineViewModel::moveClip,
                onSplit = {
                    val index = player.currentMediaItemIndex
                    val clip = state.clips.getOrNull(index)
                    if (clip != null) timelineViewModel.splitClip(clip.id, player.currentPosition * 1000)
                },
                onDelete = {
                    state.clips.getOrNull(playheadClipIndex)?.let { timelineViewModel.deleteClip(it.id) }
                },
                onQuickExtract = { showQuickExtractDialog = true },
            )
        }

        val currentExportState = exportState
        if (currentExportState is ExportUiState.Exporting) {
            ExportProgressOverlay(progress = currentExportState.progress)
        }
        val currentExtractState = extractState
        if (currentExtractState is ExtractUiState.Extracting) {
            ExportProgressOverlay(progress = currentExtractState.progress)
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

    if (showQuickExtractDialog) {
        val index = player.currentMediaItemIndex
        val currentClip = state.clips.getOrNull(index)
        if (currentClip != null) {
            QuickExtractDialog(
                videoDurationUs = currentClip.sourceDurationUs,
                anchorPositionUs = currentClip.trimStartUs + player.currentPosition * 1000,
                onDismiss = { showQuickExtractDialog = false },
                onConfirm = { windowStartUs, windowEndUs, format ->
                    showQuickExtractDialog = false
                    extractViewModel.startExtract(
                        sourceUri = currentClip.sourceUri,
                        sourceDurationUs = currentClip.sourceDurationUs,
                        windowStartUs = windowStartUs,
                        windowEndUs = windowEndUs,
                        format = format,
                    )
                },
            )
        }
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

    LaunchedEffect(extractState) {
        when (val current = extractState) {
            is ExtractUiState.Success -> {
                Toast.makeText(context, current.message, Toast.LENGTH_LONG).show()
                extractViewModel.consumeMessage()
            }
            is ExtractUiState.Error -> {
                Toast.makeText(context, current.message, Toast.LENGTH_LONG).show()
                extractViewModel.consumeMessage()
            }
            else -> Unit
        }
    }
}
