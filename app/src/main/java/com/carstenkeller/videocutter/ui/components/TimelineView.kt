package com.carstenkeller.videocutter.ui.components

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.carstenkeller.videocutter.R
import com.carstenkeller.videocutter.timeline.VideoClip
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val FULL_STRIP_THUMBNAILS = 10
private const val HANDLE_WIDTH_DP = 16

/**
 * Zeitleiste. Oben ein einfacher, bewährter Clip-Auswähler (Tippen springt
 * an den Clip-Anfang, Pfeile verschieben) für den Fall mehrerer Clips nach
 * einem Schnitt. Darunter der eigentliche Trimm-Bereich für den gerade
 * aktiven Clip: ein durchgehender Filmstreifen über die komplette
 * Quelldatei (konstante, bildschirmbreite Breite - *nicht* proportional zur
 * Clip-Länge, das hat sich als Fehlerquelle erwiesen), mit zwei weißen
 * Trimm-Griffen an den Rändern des gewählten Bereichs (Rest abgedunkelt)
 * und einem separaten ziehbaren Positionsstrich dazwischen - nachgebaut
 * nach der Standard-Trimm-Oberfläche der Android-Galerie (Nutzervorlage:
 * Screenrecording). Bewusst kein "an beliebiger Stelle tippen" mehr (das
 * war die Hauptursache für Gesten-Konflikte in früheren Versuchen) -
 * scrubben geht ausschließlich über den eigenen Positionsgriff.
 */
@Composable
fun TimelineView(
    clips: List<VideoClip>,
    playheadClipIndex: Int,
    playheadPositionUs: Long,
    onSeek: (clipIndex: Int, positionInClipUs: Long) -> Unit,
    onTrimChange: (clipId: String, trimStartUs: Long, trimEndUs: Long) -> Unit,
    onMoveClip: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        val totalDurationUs = clips.sumOf { it.durationUs }
        val elapsedUs = clips.take(playheadClipIndex).sumOf { it.durationUs } + playheadPositionUs
        Text(
            "Position: ${formatTimeMs(elapsedUs / 1000)} / ${formatTimeMs(totalDurationUs / 1000)}",
            style = MaterialTheme.typography.titleMedium,
        )

        if (clips.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .height(56.dp)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                clips.forEachIndexed { index, clip ->
                    ClipChip(
                        index = index,
                        isActive = index == playheadClipIndex,
                        canMoveLeft = index > 0,
                        canMoveRight = index < clips.lastIndex,
                        onClick = { onSeek(index, 0L) },
                        onMoveLeft = { onMoveClip(index, index - 1) },
                        onMoveRight = { onMoveClip(index, index + 1) },
                    )
                }
            }
        }

        val activeClip = clips.getOrNull(playheadClipIndex)
        if (activeClip != null) {
            ActiveClipTrimStrip(
                clip = activeClip,
                playheadPositionUs = playheadPositionUs,
                onSeek = { positionInClipUs -> onSeek(playheadClipIndex, positionInClipUs) },
                onTrimChange = onTrimChange,
            )
        }
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/** Kleine, einfache Auswahlkachel für einen von mehreren Clips (nach einem Schnitt). */
@Composable
private fun ClipChip(
    index: Int,
    isActive: Boolean,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onClick: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                BorderStroke(if (isActive) 2.dp else 1.dp, if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconActionButton(
            icon = R.drawable.ic_chevron_left,
            label = "Nach links verschieben",
            onClick = onMoveLeft,
            enabled = canMoveLeft,
        )
        Text("Clip ${index + 1}")
        IconActionButton(
            icon = R.drawable.ic_chevron_right,
            label = "Nach rechts verschieben",
            onClick = onMoveRight,
            enabled = canMoveRight,
        )
    }
}

/**
 * Durchgehender Filmstreifen über die komplette Quelldatei des aktiven
 * Clips, bildschirmbreit (konstant, unabhängig von der Videolänge - das
 * macht die Griff-Positionen stabil und vermeidet absurd breite Streifen).
 */
@Composable
private fun ActiveClipTrimStrip(
    clip: VideoClip,
    playheadPositionUs: Long,
    onSeek: (positionInClipUs: Long) -> Unit,
    onTrimChange: (clipId: String, trimStartUs: Long, trimEndUs: Long) -> Unit,
) {
    val thumbnails = rememberFullSourceThumbnails(clip)
    val latestClip = rememberUpdatedState(clip)
    var stripWidthPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val handleWidthPx = with(density) { HANDLE_WIDTH_DP.dp.toPx() }
    val playheadWidthPx = with(density) { 4.dp.toPx() }

    val trimStartFraction = (clip.trimStartUs.toFloat() / clip.sourceDurationUs.toFloat()).coerceIn(0f, 1f)
    val trimEndFraction = (clip.trimEndUs.toFloat() / clip.sourceDurationUs.toFloat()).coerceIn(0f, 1f)
    val playheadFraction = ((clip.trimStartUs + playheadPositionUs).toFloat() / clip.sourceDurationUs.toFloat())
        .coerceIn(0f, 1f)

    Column(Modifier.padding(vertical = 8.dp)) {
        Text(
            "Trimmen: %.1fs – %.1fs".format(clip.trimStartUs / 1_000_000f, clip.trimEndUs / 1_000_000f),
            style = MaterialTheme.typography.bodySmall,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(top = 4.dp)
                .onGloballyPositioned { stripWidthPx = it.size.width.toFloat() }
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (thumbnails.any { it != null }) {
                Row(modifier = Modifier.fillMaxSize()) {
                    thumbnails.forEach { bitmap ->
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant))
                        }
                    }
                }
            }

            if (trimStartFraction > 0f) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .fillMaxWidth(trimStartFraction)
                        .background(Color.Black.copy(alpha = 0.6f)),
                )
            }
            if (trimEndFraction < 1f) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .fillMaxWidth(1f - trimEndFraction)
                        .background(Color.Black.copy(alpha = 0.6f)),
                )
            }

            // Positions-Griff: einzige Möglichkeit zu scrubben (bewusst kein
            // "an beliebiger Stelle tippen" mehr).
            StripHandle(
                xPx = (playheadFraction * stripWidthPx - playheadWidthPx / 2f)
                    .coerceIn(0f, (stripWidthPx - playheadWidthPx).coerceAtLeast(0f)),
                widthDp = 4,
                color = Color.White,
                stripWidthPx = stripWidthPx,
                sourceDurationUs = clip.sourceDurationUs,
                getBaseUs = { clip.trimStartUs + playheadPositionUs },
                onNewUs = { newSourceUs ->
                    val newPositionInClipUs = (newSourceUs - latestClip.value.trimStartUs)
                        .coerceIn(0, latestClip.value.durationUs)
                    onSeek(newPositionInClipUs)
                },
            )

            // Trimm-Griffe an den Rändern des gewählten Bereichs. Auf
            // [0, stripWidthPx - handleWidthPx] begrenzt, damit der Griff nie
            // über den (geclippten) Streifenrand hinausragt - sonst würde er
            // im Ausgangszustand (kein Trimm) halb unsichtbar abgeschnitten.
            StripHandle(
                xPx = (trimStartFraction * stripWidthPx - handleWidthPx).coerceIn(0f, (stripWidthPx - handleWidthPx).coerceAtLeast(0f)),
                widthDp = HANDLE_WIDTH_DP,
                color = MaterialTheme.colorScheme.primary,
                stripWidthPx = stripWidthPx,
                sourceDurationUs = clip.sourceDurationUs,
                getBaseUs = { clip.trimStartUs },
                onNewUs = { newStartUs -> onTrimChange(latestClip.value.id, newStartUs, latestClip.value.trimEndUs) },
            )
            StripHandle(
                xPx = (trimEndFraction * stripWidthPx).coerceIn(0f, (stripWidthPx - handleWidthPx).coerceAtLeast(0f)),
                widthDp = HANDLE_WIDTH_DP,
                color = MaterialTheme.colorScheme.primary,
                stripWidthPx = stripWidthPx,
                sourceDurationUs = clip.sourceDurationUs,
                getBaseUs = { clip.trimEndUs },
                onNewUs = { newEndUs -> onTrimChange(latestClip.value.id, latestClip.value.trimStartUs, newEndUs) },
            )
        }
    }
}

/**
 * Ein ziehbarer, senkrechter Griff an absoluter Pixel-Position [xPx] auf dem
 * Streifen. [getBaseUs] liefert den zu verändernden Wert (absolute
 * Quelldatei-Mikrosekunden) bei Gesten-Start; die Verschiebung wird relativ
 * dazu aufsummiert (nicht pro Frame neu auf den sich ändernden Live-Wert
 * addiert). Da die Streifenbreite konstant ist (bildschirmbreit, unabhängig
 * vom Trimm-Zustand), ist der Pixel-pro-Mikrosekunde-Faktor stabil und muss
 * nicht bei Gesten-Start eingefroren werden.
 */
@Composable
private fun BoxScope.StripHandle(
    xPx: Float,
    widthDp: Int,
    color: Color,
    stripWidthPx: Float,
    sourceDurationUs: Long,
    getBaseUs: () -> Long,
    onNewUs: (Long) -> Unit,
) {
    var baseUs by remember { mutableStateOf(0L) }
    var accumulatedPx by remember { mutableStateOf(0f) }
    val pxPerUs = stripWidthPx / sourceDurationUs.coerceAtLeast(1)
    val dragState = rememberDraggableState { delta ->
        accumulatedPx += delta
        onNewUs(baseUs + (accumulatedPx / pxPerUs).toLong())
    }

    Box(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .offset { IntOffset(xPx.roundToInt(), 0) }
            .fillMaxHeight()
            .width(widthDp.dp)
            .background(color)
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                onDragStarted = {
                    baseUs = getBaseUs()
                    accumulatedPx = 0f
                },
            ),
    )
}

/** Lädt eine feste Anzahl Vorschaubilder über die komplette Quelldatei. */
@Composable
private fun rememberFullSourceThumbnails(clip: VideoClip): List<ImageBitmap?> {
    val context = LocalContext.current
    val state = produceState(initialValue = List<ImageBitmap?>(FULL_STRIP_THUMBNAILS) { null }, clip.id) {
        value = withContext(Dispatchers.IO) {
            extractThumbnails(context, clip.sourceUri, clip.sourceDurationUs, FULL_STRIP_THUMBNAILS)
        }
    }
    return state.value
}

/** Liste fester Länge [count]; einzelne Einträge sind `null`, wenn dieser Frame nicht extrahiert werden konnte. */
private fun extractThumbnails(
    context: Context,
    sourceUri: android.net.Uri,
    sourceDurationUs: Long,
    count: Int,
): List<ImageBitmap?> {
    if (count <= 0) return emptyList()
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, sourceUri)
        val durationUs = sourceDurationUs.coerceAtLeast(1)
        (0 until count).map { i ->
            val timeUs = durationUs * i / count
            val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
            frame?.asImageBitmap()
        }
    } catch (_: Exception) {
        List(count) { null }
    } finally {
        retriever.release()
    }
}
