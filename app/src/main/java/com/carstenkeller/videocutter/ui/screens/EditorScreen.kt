package com.carstenkeller.videocutter.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.CompositionPlayer
import com.carstenkeller.videocutter.export.ExportUiState
import com.carstenkeller.videocutter.export.ExportViewModel
import com.carstenkeller.videocutter.extract.ExtractDurationPreset
import com.carstenkeller.videocutter.extract.ExtractFormat
import com.carstenkeller.videocutter.extract.ExtractUiState
import com.carstenkeller.videocutter.extract.ExtractViewModel
import com.carstenkeller.videocutter.extract.ExtractWindow
import com.carstenkeller.videocutter.timeline.TimelineViewModel
import com.carstenkeller.videocutter.timeline.VideoClip
import com.carstenkeller.videocutter.timeline.buildComposition
import com.carstenkeller.videocutter.timeline.clipPositionAt
import com.carstenkeller.videocutter.timeline.elapsedUsAt
import com.carstenkeller.videocutter.ui.components.ExportDialog
import com.carstenkeller.videocutter.ui.components.ExportProgressOverlay
import com.carstenkeller.videocutter.ui.components.ExtractControls
import com.carstenkeller.videocutter.ui.components.PlayerPreview
import com.carstenkeller.videocutter.ui.components.TimelineToolbar
import com.carstenkeller.videocutter.ui.components.TimelineView
import com.carstenkeller.videocutter.ui.components.rememberVideoPickerLauncher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class, ExperimentalApi::class)
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

    // CompositionPlayer statt ExoPlayer-Playlist: spielt dieselbe Composition,
    // die auch exportiert wird (Voraussetzung für Übergänge und Bildformat in
    // der Vorschau). Er stellt die ganze Composition als *ein* Element dar -
    // Clip-Index und Position im Clip werden deshalb über clipPositionAt()
    // aus der durchgehenden Position berechnet, bezogen auf loadedClips (die
    // Clips, die der Player gerade tatsächlich geladen hat).
    val player = remember { CompositionPlayer.Builder(context).build() }
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }
    var loadedClips by remember { mutableStateOf(emptyList<VideoClip>()) }

    // Debounced statt bei jedem einzelnen Trimm-Griff-Tick: sonst baut jede
    // Fingerbewegung auf der Zeitleiste die komplette Composition neu auf,
    // was die App spürbar träge macht.
    LaunchedEffect(timelineViewModel) {
        var pendingRebuild: Job? = null
        timelineViewModel.state.collect { current ->
            pendingRebuild?.cancel()
            pendingRebuild = launch {
                delay(250)
                if (current.clips.isEmpty()) return@launch

                // Die durchgehende Position bleibt über den Neuaufbau erhalten,
                // sonst würde z. B. jeder Trimm-Zug an den Anfang springen.
                val totalUs = current.clips.sumOf { it.durationUs }
                val targetMs = (player.currentPosition * 1000).coerceIn(0, totalUs) / 1000
                player.setComposition(buildComposition(current.clips), targetMs)
                // Unschädlich, wenn schon vorbereitet; nach einem Fehler nötig.
                player.prepare()
                loadedClips = current.clips
            }
        }
    }

    var showExportDialog by remember { mutableStateOf(false) }
    val addClipLauncher = rememberVideoPickerLauncher(onPicked = timelineViewModel::addClip)

    // Schnellausschnitt: Länge/Format direkt auf der Hauptseite statt in
    // einem Dialog; die Position wird per Zieh-Fenster auf der Zeitleiste
    // festgelegt (extractWindowStartUs), nicht hier.
    var extractModeActive by remember { mutableStateOf(false) }
    var extractPreset by remember { mutableStateOf<ExtractDurationPreset?>(ExtractDurationPreset.MEDIUM) }
    var extractCustomDurationSeconds by remember { mutableStateOf(15f) }
    var extractFormat by remember { mutableStateOf(ExtractFormat.GIF) }
    var extractWindowStartUs by remember { mutableStateOf(0L) }
    val extractDurationUs = ((extractPreset?.seconds?.toFloat() ?: extractCustomDurationSeconds) * 1_000_000L).toLong()
    // Die dauerhaft laufende Loop-Coroutine unten (LaunchedEffect(player), Key
    // ändert sich nie) wurde einmalig gestartet und schließt sonst über den
    // zu diesem Zeitpunkt aktuellen Wert von extractDurationUs - spätere
    // Recompositions (z. B. eine andere Länge gewählt) hätten dort keine
    // Wirkung mehr gezeigt (Bug: Loop blieb dauerhaft auf der zuerst
    // gewählten Länge). rememberUpdatedState hält die Coroutine auf dem
    // aktuellen Wert.
    val latestExtractDurationUs = rememberUpdatedState(extractDurationUs)

    // Playhead-Position fürs Timeline-Lineal: der Player liefert keinen
    // Compose-State für currentPosition, daher wird hier regelmäßig abgefragt
    // (reicht für eine Positionsanzeige, muss nicht jeden Frame aktuell sein).
    var playheadClipIndex by remember { mutableStateOf(0) }
    var playheadPositionUs by remember { mutableStateOf(0L) }
    LaunchedEffect(player) {
        while (true) {
            val position = clipPositionAt(loadedClips, player.currentPosition * 1000)
            playheadClipIndex = position.clipIndex
            playheadPositionUs = position.positionInClipUs

            // Im Ausschnitt-Modus nur das gewählte Fenster in Dauerschleife
            // abspielen: sobald die Wiedergabe das Fenster verlässt, zurück
            // an den Fensteranfang springen.
            if (extractModeActive) {
                val clip = loadedClips.getOrNull(playheadClipIndex)
                if (clip != null) {
                    val absoluteUs = clip.trimStartUs + playheadPositionUs
                    val windowEndUs = extractWindowStartUs + latestExtractDurationUs.value
                    if (absoluteUs < extractWindowStartUs || absoluteUs >= windowEndUs) {
                        val windowStartInClipUs = extractWindowStartUs - clip.trimStartUs
                        player.seekTo(elapsedUsAt(loadedClips, playheadClipIndex, windowStartInClipUs) / 1000)
                    }
                }
            }

            delay(200)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Feste Höhe (Seitenverhältnis statt weight(1f)): die Vorschau
            // darf nicht schrumpfen, wenn der Ausschnitt-Bereich darunter
            // wächst (z. B. bei aktivem Schnellausschnitt-Panel) - stattdessen
            // scrollt der Bereich darunter bei Bedarf.
            PlayerPreview(
                player = player,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black),
            )

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            ) {
                TimelineToolbar(
                    exportEnabled = state.clips.isNotEmpty() && exportState !is ExportUiState.Exporting,
                    onSplit = {
                        val position = clipPositionAt(loadedClips, player.currentPosition * 1000)
                        val clip = loadedClips.getOrNull(position.clipIndex)
                        if (clip != null) timelineViewModel.splitClip(clip.id, position.positionInClipUs)
                    },
                    onDelete = {
                        state.clips.getOrNull(playheadClipIndex)?.let { timelineViewModel.deleteClip(it.id) }
                    },
                    onAddClip = addClipLauncher,
                    onRecordClip = onRecordRequested,
                    onQuickExtract = {
                        val clip = state.clips.getOrNull(playheadClipIndex)
                        if (clip != null) {
                            val centerUs = clip.trimStartUs + playheadPositionUs
                            extractWindowStartUs = (centerUs - extractDurationUs / 2)
                                .coerceIn(0, (clip.sourceDurationUs - extractDurationUs).coerceAtLeast(0))
                            extractModeActive = true
                        }
                    },
                    onExport = { showExportDialog = true },
                )

                if (extractModeActive) {
                    val extractClip = state.clips.getOrNull(playheadClipIndex)
                    ExtractControls(
                        videoDurationUs = extractClip?.sourceDurationUs ?: 0L,
                        preset = extractPreset,
                        customDurationSeconds = extractCustomDurationSeconds,
                        format = extractFormat,
                        onPresetSelected = { newPreset ->
                            val oldDurationUs = extractDurationUs
                            extractPreset = newPreset
                            val allowsGif = newPreset?.allowsGif ?: false
                            if (!allowsGif && extractFormat == ExtractFormat.GIF) extractFormat = ExtractFormat.MP4
                            if (extractClip != null) {
                                val newDurationUs = ((newPreset?.seconds?.toFloat() ?: extractCustomDurationSeconds) * 1_000_000L).toLong()
                                extractWindowStartUs = recenteredExtractStartUs(
                                    oldStartUs = extractWindowStartUs,
                                    oldDurationUs = oldDurationUs,
                                    newDurationUs = newDurationUs,
                                    sourceDurationUs = extractClip.sourceDurationUs,
                                )
                            }
                        },
                        onCustomDurationChange = { newSeconds ->
                            // Setzt bewusst auch den Preset-Modus zurück (statt
                            // zwei getrennte Callbacks aufzurufen): zwei
                            // aufeinanderfolgende State-Änderungen in einem
                            // Klick würden sich hier gegenseitig mit jeweils
                            // veraltetem extractDurationUs überschreiben.
                            val oldDurationUs = extractDurationUs
                            extractPreset = null
                            extractCustomDurationSeconds = newSeconds
                            if (extractClip != null) {
                                val newDurationUs = (newSeconds * 1_000_000L).toLong()
                                extractWindowStartUs = recenteredExtractStartUs(
                                    oldStartUs = extractWindowStartUs,
                                    oldDurationUs = oldDurationUs,
                                    newDurationUs = newDurationUs,
                                    sourceDurationUs = extractClip.sourceDurationUs,
                                )
                            }
                        },
                        onFormatSelected = { extractFormat = it },
                        onCancel = { extractModeActive = false },
                        onExtract = {
                            if (extractClip != null) {
                                extractModeActive = false
                                extractViewModel.startExtract(
                                    sourceUri = extractClip.sourceUri,
                                    sourceDurationUs = extractClip.sourceDurationUs,
                                    windowStartUs = extractWindowStartUs,
                                    windowEndUs = (extractWindowStartUs + extractDurationUs).coerceAtMost(extractClip.sourceDurationUs),
                                    format = extractFormat,
                                )
                            }
                        },
                    )
                }

                TimelineView(
                    clips = state.clips,
                    playheadClipIndex = playheadClipIndex,
                    playheadPositionUs = playheadPositionUs,
                    onSeek = { index, positionInClipUs -> player.seekTo(elapsedUsAt(loadedClips, index, positionInClipUs) / 1000) },
                    onTrimChange = timelineViewModel::updateTrim,
                    onMoveClip = timelineViewModel::moveClip,
                    extractWindow = if (extractModeActive) ExtractWindow(extractWindowStartUs, extractDurationUs) else null,
                    onExtractWindowMove = { newStartUs -> extractWindowStartUs = newStartUs },
                )
            }
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

/**
 * Neue Fenster-Startzeit, wenn sich die Ausschnitt-Länge ändert: hält die
 * Fenster-MITTE fest (statt den Anfang) - Nutzerfeedback: "setzt du die
 * Auswahl als Startzeitpunkt für die Sequenz, es sollte genau die Mitte
 * sein". Ohne das würde z. B. ein auf der Zeitleiste verschobenes Fenster
 * bei einer Längenänderung wieder an seinem (alten) Anfang kleben, statt
 * um die zuletzt fokussierte Stelle herum zu bleiben.
 */
private fun recenteredExtractStartUs(
    oldStartUs: Long,
    oldDurationUs: Long,
    newDurationUs: Long,
    sourceDurationUs: Long,
): Long {
    val centerUs = oldStartUs + oldDurationUs / 2
    return (centerUs - newDurationUs / 2).coerceIn(0, (sourceDurationUs - newDurationUs).coerceAtLeast(0))
}
