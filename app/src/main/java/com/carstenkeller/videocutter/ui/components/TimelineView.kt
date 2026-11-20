package com.carstenkeller.videocutter.ui.components

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carstenkeller.videocutter.R
import com.carstenkeller.videocutter.timeline.VideoClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val THUMBNAILS_PER_CLIP = 8

/**
 * Zeitleiste. Jeder Clip-Block zeigt immer die komplette Quelldatei (konstante
 * Breite = Quelllänge), nicht nur den getrimmten Ausschnitt - Trimm-Grenzen
 * sind zwei verschiebbare Griffe *innerhalb* des Blocks, der Bereich davor/
 * danach wird abgedunkelt. Das ist bewusst so (nicht breiten-proportional zur
 * Trimm-Länge): In einer Row ändert sich beim Verkleinern eines Elements nur
 * dessen rechter Rand sichtbar, nie der linke - ein breitenbasierter Ansatz
 * hätte den linken Trimm-Griff optisch in die falsche Richtung laufen lassen.
 *
 * Tippen auf einen Block springt im Video an diese Stelle (Scrubben). Der
 * Clip, in dem sich die Wiedergabeposition gerade befindet, gilt als "aktiv"
 * (Trimm-Griffe, Verschieben-Pfeile) - es gibt bewusst keine davon unabhängige
 * Auswahl mehr (siehe TimelineViewModel).
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
            "Zeitleiste – ${formatTimeMs(elapsedUs / 1000)} / ${formatTimeMs(totalDurationUs / 1000)}",
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .height(96.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            clips.forEachIndexed { index, clip ->
                val sourceSeconds = clip.sourceDurationUs / 1_000_000f
                val widthDp = (sourceSeconds * 60f).coerceAtLeast(56f).dp
                ClipBlock(
                    clip = clip,
                    index = index,
                    isActive = index == playheadClipIndex,
                    widthDp = widthDp,
                    canMoveLeft = index > 0,
                    canMoveRight = index < clips.lastIndex,
                    playheadFraction = ((clip.trimStartUs + playheadPositionUs).toFloat() / clip.sourceDurationUs.toFloat())
                        .coerceIn(0f, 1f),
                    onSeek = { fraction ->
                        val absoluteUs = (fraction * clip.sourceDurationUs).toLong()
                            .coerceIn(clip.trimStartUs, clip.trimEndUs)
                        onSeek(index, absoluteUs - clip.trimStartUs)
                    },
                    onTrimChange = onTrimChange,
                    onMoveLeft = { onMoveClip(index, index - 1) },
                    onMoveRight = { onMoveClip(index, index + 1) },
                )
            }
        }
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun ClipBlock(
    clip: VideoClip,
    index: Int,
    isActive: Boolean,
    widthDp: Dp,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    playheadFraction: Float,
    onSeek: (fraction: Float) -> Unit,
    onTrimChange: (clipId: String, trimStartUs: Long, trimEndUs: Long) -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
) {
    val thumbnails = rememberClipThumbnails(clip)
    val latestClip = rememberUpdatedState(clip)
    val density = LocalDensity.current
    val widthPx = with(density) { widthDp.toPx() }

    val trimStartFraction = (clip.trimStartUs.toFloat() / clip.sourceDurationUs.toFloat()).coerceIn(0f, 1f)
    val trimEndFraction = (clip.trimEndUs.toFloat() / clip.sourceDurationUs.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .width(widthDp)
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                BorderStroke(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                ),
                RoundedCornerShape(8.dp),
            )
            .pointerInput(index) {
                detectTapGestures { offset -> onSeek((offset.x / size.width.toFloat()).coerceIn(0f, 1f)) }
            },
    ) {
        if (thumbnails.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxSize()) {
                thumbnails.forEach { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        } else {
            Text(
                "Clip ${index + 1}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Abgedunkelte Bereiche außerhalb des Trimm-Fensters.
        if (trimStartFraction > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(widthDp * trimStartFraction)
                    .background(Color.Black.copy(alpha = 0.55f)),
            )
        }
        if (trimEndFraction < 1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(widthDp * (1f - trimEndFraction))
                    .background(Color.Black.copy(alpha = 0.55f)),
            )
        }

        if (isActive) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconActionButton(
                    icon = R.drawable.ic_chevron_left,
                    label = "Nach links verschieben",
                    onClick = onMoveLeft,
                    enabled = canMoveLeft,
                    tint = Color.White,
                )
                Text(
                    "Clip ${index + 1}",
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconActionButton(
                    icon = R.drawable.ic_chevron_right,
                    label = "Nach rechts verschieben",
                    onClick = onMoveRight,
                    enabled = canMoveRight,
                    tint = Color.White,
                )
            }

            // Positionsbalken (Playhead), relativ zur vollen Quelllänge.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = widthDp * playheadFraction)
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )

            TrimHandle(
                fraction = trimStartFraction,
                currentValueUs = { latestClip.value.trimStartUs },
                sourceDurationUs = clip.sourceDurationUs,
                widthDp = widthDp,
                widthPx = widthPx,
                onValueChange = { newStartUs -> onTrimChange(latestClip.value.id, newStartUs, latestClip.value.trimEndUs) },
            )
            TrimHandle(
                fraction = trimEndFraction,
                currentValueUs = { latestClip.value.trimEndUs },
                sourceDurationUs = clip.sourceDurationUs,
                widthDp = widthDp,
                widthPx = widthPx,
                onValueChange = { newEndUs -> onTrimChange(latestClip.value.id, latestClip.value.trimStartUs, newEndUs) },
            )
        }
    }
}

/**
 * Trimm-Griff, positioniert bei [fraction] der (konstanten) Block-Breite.
 * [currentValueUs] liefert den aktuellen Trimm-Wert (Start oder Ende) beim
 * Start der Geste - die Verschiebung wird relativ dazu aufsummiert, damit
 * sie nicht pro Frame erneut auf den (sich während der Geste ändernden)
 * Live-Wert aufaddiert wird.
 */
@Composable
private fun BoxScope.TrimHandle(
    fraction: Float,
    currentValueUs: () -> Long,
    sourceDurationUs: Long,
    widthDp: Dp,
    widthPx: Float,
    onValueChange: (newValueUs: Long) -> Unit,
) {
    var baseValueUs by remember { mutableStateOf(0L) }
    var accumulatedPx by remember { mutableStateOf(0f) }
    val pxPerUs = widthPx / sourceDurationUs.coerceAtLeast(1)

    Box(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .offset(x = (widthDp * fraction - 8.dp).coerceIn(0.dp, widthDp - 16.dp))
            .fillMaxHeight()
            .width(16.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), RoundedCornerShape(3.dp))
            .pointerInput(sourceDurationUs) {
                detectDragGestures(
                    onDragStart = {
                        baseValueUs = currentValueUs()
                        accumulatedPx = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedPx += dragAmount.x
                        val deltaUs = (accumulatedPx / pxPerUs).toLong()
                        onValueChange(baseValueUs + deltaUs)
                    },
                )
            },
    )
}

/**
 * Lädt eine feste Anzahl Vorschaubilder über die komplette Quelldatei
 * (unabhängig vom aktuellen Trimm, der sich nur noch über die Griffe/
 * Abdunkelung zeigt). Wird bewusst nur bei neuer Clip-Id neu berechnet.
 */
@Composable
private fun rememberClipThumbnails(clip: VideoClip): List<ImageBitmap> {
    val context = LocalContext.current
    val state = produceState(initialValue = emptyList(), clip.id) {
        value = withContext(Dispatchers.IO) {
            extractThumbnails(context, clip.sourceUri, clip.sourceDurationUs, THUMBNAILS_PER_CLIP)
        }
    }
    return state.value
}

private fun extractThumbnails(
    context: Context,
    sourceUri: Uri,
    sourceDurationUs: Long,
    count: Int,
): List<ImageBitmap> {
    if (count <= 0) return emptyList()
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, sourceUri)
        val durationUs = sourceDurationUs.coerceAtLeast(1)
        (0 until count).mapNotNull { i ->
            val timeUs = durationUs * i / count
            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?.asImageBitmap()
        }
    } catch (_: Exception) {
        emptyList()
    } finally {
        retriever.release()
    }
}
