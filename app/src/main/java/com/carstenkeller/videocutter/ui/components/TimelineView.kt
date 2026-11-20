package com.carstenkeller.videocutter.ui.components

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carstenkeller.videocutter.R
import com.carstenkeller.videocutter.timeline.VideoClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val THUMBNAILS_PER_CLIP = 6

/**
 * Zeitleiste - bewusst auf einfache, bewährte Bausteine zurückgestellt
 * (Tippen zum Springen an den Clip-Anfang, Trimmen über einen separaten
 * Regler) statt komplexer Gesten direkt auf der Zeitleiste (Tippen-an-
 * beliebiger-Stelle, Zieh-Griffe am Blockrand): diese hatten sich in der
 * Praxis als unzuverlässig erwiesen und ließen sich ohne echtes Gerät nicht
 * zuverlässig weiterentwickeln. Standardfunktionalität zuerst, Komfort-
 * Gesten erst wieder, wenn diese Basis stabil bestätigt ist.
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .height(88.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            clips.forEachIndexed { index, clip ->
                val durationSeconds = clip.durationUs / 1_000_000f
                val widthDp = (durationSeconds * 60f).coerceAtLeast(56f).dp
                ClipBlock(
                    clip = clip,
                    index = index,
                    isActive = index == playheadClipIndex,
                    widthDp = widthDp,
                    canMoveLeft = index > 0,
                    canMoveRight = index < clips.lastIndex,
                    playheadFraction = (playheadPositionUs.toFloat() / clip.durationUs.toFloat()).coerceIn(0f, 1f),
                    onClick = { onSeek(index, 0L) },
                    onMoveLeft = { onMoveClip(index, index - 1) },
                    onMoveRight = { onMoveClip(index, index + 1) },
                )
            }
        }

        val activeClip = clips.getOrNull(playheadClipIndex)
        if (activeClip != null) {
            TrimControls(clip = activeClip, onTrimChange = onTrimChange)
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
    onClick: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
) {
    val thumbnails = rememberClipThumbnails(clip)

    Box(
        modifier = Modifier
            .width(widthDp)
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                BorderStroke(
                    width = if (isActive) 3.dp else 1.dp,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                ),
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnails.any { it != null }) {
            // Immer THUMBNAILS_PER_CLIP gleich breite Slots, auch wenn
            // einzelne Frames nicht extrahiert werden konnten (Platzhalter
            // statt die erfolgreichen Bilder über die ganze Blockbreite zu
            // strecken).
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
        } else {
            Text("Clip ${index + 1}", maxLines = 1, overflow = TextOverflow.Ellipsis)
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

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (widthDp * playheadFraction - 2.dp).coerceAtLeast(0.dp))
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color.Black)),
            )
        }
    }
}

/**
 * Lädt eine feste Anzahl Vorschaubilder für den getrimmten Ausschnitt des
 * Clips. Wird bewusst nur bei neuer Clip-Id neu berechnet (nicht bei jeder
 * Trimm-Änderung), damit das Ziehen am Trimm-Regler nicht durch wiederholte
 * Frame-Extraktion ruckelt.
 */
@Composable
private fun rememberClipThumbnails(clip: VideoClip): List<ImageBitmap?> {
    val context = LocalContext.current
    val state = produceState(initialValue = List<ImageBitmap?>(THUMBNAILS_PER_CLIP) { null }, clip.id) {
        value = withContext(Dispatchers.IO) {
            extractThumbnails(context, clip, THUMBNAILS_PER_CLIP)
        }
    }
    return state.value
}

/** Liste fester Länge [count]; einzelne Einträge sind `null`, wenn dieser Frame nicht extrahiert werden konnte. */
private fun extractThumbnails(context: Context, clip: VideoClip, count: Int): List<ImageBitmap?> {
    if (count <= 0) return emptyList()
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, clip.sourceUri)
        val rangeUs = (clip.trimEndUs - clip.trimStartUs).coerceAtLeast(1)
        (0 until count).map { i ->
            val timeUs = clip.trimStartUs + (rangeUs * i / count)
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

@Composable
private fun TrimControls(
    clip: VideoClip,
    onTrimChange: (clipId: String, trimStartUs: Long, trimEndUs: Long) -> Unit,
) {
    val durationSeconds = (clip.sourceDurationUs / 1_000_000f).coerceAtLeast(0.2f)
    var range by remember(clip.id) {
        mutableStateOf((clip.trimStartUs / 1_000_000f)..(clip.trimEndUs / 1_000_000f))
    }
    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(
            "Trimmen: %.1fs – %.1fs".format(range.start, range.endInclusive),
            style = MaterialTheme.typography.bodySmall,
        )
        RangeSlider(
            value = range,
            onValueChange = { newRange ->
                range = newRange
                onTrimChange(
                    clip.id,
                    (newRange.start * 1_000_000).toLong(),
                    (newRange.endInclusive * 1_000_000).toLong(),
                )
            },
            valueRange = 0f..durationSeconds,
        )
    }
}
