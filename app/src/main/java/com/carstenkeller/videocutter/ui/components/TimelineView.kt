package com.carstenkeller.videocutter.ui.components

import android.content.Context
import android.media.MediaMetadataRetriever
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

private const val THUMBNAILS_PER_CLIP = 6
private const val TRIM_HANDLE_WIDTH_DP = 14

/**
 * Zeitleiste. Jeder Block ist proportional zur *getrimmten* Clip-Länge breit
 * (nicht zur vollen Quelllänge) - eine frühere Variante zeigte die komplette
 * Quelldatei pro Block an, was bei normal langen Videos absurd breite,
 * kaum noch nutzbare Blöcke ergab (60s Quelle -> ~3600dp) und dadurch
 * Positionsbalken/Schnitte praktisch unsichtbar machte. Die Trimm-Griffe
 * sitzen deshalb schlicht an den beiden Rändern des Blocks (Start=0%,
 * Ende=100% der getrimmten Länge per Definition); einzige bekannte
 * Einschränkung: Da ein Row-Layout ein Element nur von rechts sichtbar
 * schrumpft/wächst, bleibt der linke Griff beim Ziehen optisch an Ort und
 * Stelle, während der Block sich dahinter verkleinert/vergrößert - die
 * Geste selbst (Fingerbewegung -> Trimm-Wert) funktioniert trotzdem korrekt.
 *
 * Tippen auf einen Block springt im Video an diese Stelle (Scrubben). Der
 * Clip, in dem sich die Wiedergabeposition gerade befindet, gilt als "aktiv"
 * (Trimm-Griffe, Verschieben-Pfeile) - es gibt bewusst keine davon
 * unabhängige Auswahl mehr (siehe TimelineViewModel).
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
                    onSeek = { fraction -> onSeek(index, (fraction * clip.durationUs).toLong()) },
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
            .pointerInput(index) {
                detectTapGestures { offset -> onSeek((offset.x / size.width.toFloat()).coerceIn(0f, 1f)) }
            },
        contentAlignment = Alignment.Center,
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

            // Gut sichtbarer Positionsbalken (Playhead) - deutlich breiter und
            // mit Außenlinie, damit er auch auf hellen/dunklen Thumbnails nicht
            // untergeht (eine reine 2dp-Primärfarblinie war zu unauffällig).
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (widthDp * playheadFraction - 2.dp).coerceAtLeast(0.dp))
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color.Black)),
            )

            TrimHandle(
                alignment = Alignment.CenterStart,
                currentValueUs = { latestClip.value.trimStartUs },
                otherBoundaryUs = { latestClip.value.trimEndUs },
                durationUsAtDragStart = { latestClip.value.durationUs },
                widthDp = widthDp,
                onNewTrim = { newStartUs, trimEndUs -> onTrimChange(latestClip.value.id, newStartUs, trimEndUs) },
            )
            TrimHandle(
                alignment = Alignment.CenterEnd,
                currentValueUs = { latestClip.value.trimEndUs },
                otherBoundaryUs = { latestClip.value.trimStartUs },
                durationUsAtDragStart = { latestClip.value.durationUs },
                widthDp = widthDp,
                onNewTrim = { newEndUs, trimStartUs -> onTrimChange(latestClip.value.id, trimStartUs, newEndUs) },
            )
        }
    }
}

/**
 * Trimm-Griff am Rand ([alignment] CenterStart oder CenterEnd) des aktiven
 * Blocks. [currentValueUs] liefert bei Gesten-Start den zu verändernden
 * Grenzwert (Start oder Ende), [otherBoundaryUs] die jeweils andere,
 * unveränderte Grenze (live, für den onNewTrim-Aufruf). Die Verschiebung
 * wird relativ zum bei Gesten-Start gesicherten Wert aufsummiert, damit sie
 * nicht pro Frame erneut auf den sich ändernden Live-Wert aufaddiert wird.
 */
@Composable
private fun BoxScope.TrimHandle(
    alignment: Alignment,
    currentValueUs: () -> Long,
    otherBoundaryUs: () -> Long,
    durationUsAtDragStart: () -> Long,
    widthDp: Dp,
    onNewTrim: (newValueUs: Long, otherBoundaryUs: Long) -> Unit,
) {
    val density = LocalDensity.current
    var baseValueUs by remember { mutableStateOf(0L) }
    var pxPerUs by remember { mutableStateOf(0f) }
    var accumulatedPx by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .align(alignment)
            .fillMaxHeight()
            .width(TRIM_HANDLE_WIDTH_DP.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        baseValueUs = currentValueUs()
                        val widthPx = with(density) { widthDp.toPx() }
                        pxPerUs = widthPx / durationUsAtDragStart().coerceAtLeast(1)
                        accumulatedPx = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedPx += dragAmount.x
                        val deltaUs = (accumulatedPx / pxPerUs).toLong()
                        onNewTrim(baseValueUs + deltaUs, otherBoundaryUs())
                    },
                )
            },
    )
}

/**
 * Lädt eine feste Anzahl Vorschaubilder für den *getrimmten* Ausschnitt des
 * Clips. Wird bewusst nur bei neuer Clip-Id neu berechnet (nicht bei jeder
 * Trimm-Änderung), damit das Ziehen am Trimm-Griff nicht durch wiederholte
 * Frame-Extraktion ruckelt - und damit zwei durch einen Schnitt entstandene
 * Clips sichtbar unterschiedliche Thumbnails zeigen (unterschiedlicher
 * Zeitbereich), statt beide dieselbe Vorschau der vollen Quelldatei.
 */
@Composable
private fun rememberClipThumbnails(clip: VideoClip): List<ImageBitmap> {
    val context = LocalContext.current
    val state = produceState(initialValue = emptyList(), clip.id) {
        value = withContext(Dispatchers.IO) {
            extractThumbnails(context, clip, THUMBNAILS_PER_CLIP)
        }
    }
    return state.value
}

private fun extractThumbnails(context: Context, clip: VideoClip, count: Int): List<ImageBitmap> {
    if (count <= 0) return emptyList()
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, clip.sourceUri)
        val rangeUs = (clip.trimEndUs - clip.trimStartUs).coerceAtLeast(1)
        (0 until count).mapNotNull { i ->
            val timeUs = clip.trimStartUs + (rangeUs * i / count)
            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?.asImageBitmap()
        }
    } catch (_: Exception) {
        emptyList()
    } finally {
        retriever.release()
    }
}
