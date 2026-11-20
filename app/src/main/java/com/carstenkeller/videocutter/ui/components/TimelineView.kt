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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.carstenkeller.videocutter.extract.ExtractWindow
import com.carstenkeller.videocutter.timeline.VideoClip
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val THUMBNAIL_EXTRACT_COUNT = 40
private const val STRIP_HEIGHT_DP = 80
private const val HANDLE_WIDTH_DP = 16
private const val MAX_ZOOM = 60f
private const val ZOOM_STEP = 1.6f
private const val FALLBACK_ASPECT_RATIO = 16f / 9f

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
    extractWindow: ExtractWindow?,
    onExtractWindowMove: (newStartUs: Long) -> Unit,
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
                extractWindow = extractWindow,
                onExtractWindowMove = onExtractWindowMove,
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
 * Clips. Ohne Zoom bildschirmbreit (konstant, unabhängig von der
 * Videolänge - das macht die Griff-Positionen stabil und vermeidet absurd
 * breite Streifen); über die Zoom-Buttons lässt sich der Streifen bis auf
 * das Sechsfache strecken und per Scrollen durchfahren, um bei längeren
 * Videos trotzdem präzise eine Stelle treffen zu können.
 */
@Composable
private fun ActiveClipTrimStrip(
    clip: VideoClip,
    playheadPositionUs: Long,
    onSeek: (positionInClipUs: Long) -> Unit,
    onTrimChange: (clipId: String, trimStartUs: Long, trimEndUs: Long) -> Unit,
    extractWindow: ExtractWindow?,
    onExtractWindowMove: (newStartUs: Long) -> Unit,
) {
    val latestClip = rememberUpdatedState(clip)
    var viewportWidthPx by remember { mutableStateOf(0f) }
    var zoomLevel by remember(clip.id) { mutableStateOf(1f) }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val handleWidthPx = with(density) { HANDLE_WIDTH_DP.dp.toPx() }
    val playheadWidthPx = with(density) { 4.dp.toPx() }
    val stripWidthPx = viewportWidthPx * zoomLevel
    // Feste, von Zoom unabhängige Anzahl tatsächlich extrahierter Einzelbilder
    // (teuer: ein MediaMetadataRetriever-Seek pro Bild) - mehrere angezeigte
    // Kacheln können bei hohem Zoom auf dasselbe Einzelbild zeigen (s. u.).
    val thumbnails = rememberFullSourceThumbnails(clip, THUMBNAIL_EXTRACT_COUNT)

    // Kachel-Breite aus dem tatsächlichen Bildformat des Videos ableiten
    // (anhand der Maße des ersten geladenen Frames) statt einer festen Anzahl
    // Kacheln pro Bildschirmbreite - sonst würde bei normalen Videoformaten
    // (z. B. 16:9 oder 9:16) die komplette Videolänge in eine Bildschirmbreite
    // gepresst und jede Kachel dadurch stark verzerrt (viel zu schmal/hoch)
    // (Nutzerfeedback: "Die Thumbnails sind dadurch 4x so hoch wie breit...
    // müssen im Bildformat des Videos bleiben"). Die Streifenbreite selbst
    // bleibt bewusst unverändert bildschirmbreite*Zoom (nicht an die
    // Videolänge gekoppelt - frühere Fehlerquelle), nur die Anzahl der
    // Kacheln darin ergibt sich jetzt aus deren fester, seitenverhältnis-
    // treuer Breite: beim Zoomen wächst die Streifenbreite, also auch die
    // Kachel-Anzahl und die nötige Scrollstrecke (genau wie gewünscht).
    val tileHeightPx = with(density) { STRIP_HEIGHT_DP.dp.toPx() }
    val videoAspectRatio = thumbnails.firstNotNullOfOrNull { it }
        ?.let { it.width.toFloat() / it.height.toFloat() }
        ?: FALLBACK_ASPECT_RATIO
    val tileWidthPx = tileHeightPx * videoAspectRatio
    val totalThumbnailSlots = (stripWidthPx / tileWidthPx).roundToInt().coerceAtLeast(1)

    val trimStartFraction = (clip.trimStartUs.toFloat() / clip.sourceDurationUs.toFloat()).coerceIn(0f, 1f)
    val trimEndFraction = (clip.trimEndUs.toFloat() / clip.sourceDurationUs.toFloat()).coerceIn(0f, 1f)
    val playheadFraction = ((clip.trimStartUs + playheadPositionUs).toFloat() / clip.sourceDurationUs.toFloat())
        .coerceIn(0f, 1f)

    // Beim Zoomen die aktuelle Wiedergabeposition im sichtbaren Bereich
    // zentrieren, statt sie aus dem Blickfeld scrollen zu lassen.
    LaunchedEffect(zoomLevel, viewportWidthPx) {
        if (viewportWidthPx <= 0f) return@LaunchedEffect
        val target = (playheadFraction * stripWidthPx - viewportWidthPx / 2f)
            .roundToInt().coerceIn(0, scrollState.maxValue)
        scrollState.scrollTo(target)
    }

    // Während der Wiedergabe der Zeitleiste folgen, statt den
    // Positionsgriff aus dem sichtbaren Bereich laufen zu lassen
    // (Nutzerfeedback: "Nicht die Markierung in der Zeitleiste soll sich
    // aus dem Bild bewegen, sondern die Zeitleiste bewegt sich fort").
    // Nur wenn der Griff den sichtbaren Bereich tatsächlich verlässt (nicht
    // bei jedem Positions-Tick neu zentrieren) - sonst ließe sich der
    // Streifen im pausierten Zustand nicht mehr frei verschieben, um sich
    // andere Stellen anzusehen.
    LaunchedEffect(playheadFraction) {
        if (viewportWidthPx <= 0f) return@LaunchedEffect
        val playheadPx = playheadFraction * stripWidthPx
        val visibleStart = scrollState.value.toFloat()
        val visibleEnd = visibleStart + viewportWidthPx
        if (playheadPx < visibleStart || playheadPx > visibleEnd) {
            val target = (playheadPx - viewportWidthPx / 2f).roundToInt().coerceIn(0, scrollState.maxValue)
            scrollState.animateScrollTo(target)
        }
    }

    Column(Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Trimmen: %.1fs – %.1fs".format(clip.trimStartUs / 1_000_000f, clip.trimEndUs / 1_000_000f),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                ZoomButton(label = "−", enabled = zoomLevel > 1f) {
                    zoomLevel = (zoomLevel / ZOOM_STEP).coerceAtLeast(1f)
                }
                Text("${zoomLevel.roundToInt()}x", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 4.dp))
                ZoomButton(label = "+", enabled = zoomLevel < MAX_ZOOM) {
                    zoomLevel = (zoomLevel * ZOOM_STEP).coerceAtMost(MAX_ZOOM)
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(STRIP_HEIGHT_DP.dp)
                .padding(top = 4.dp)
                .onGloballyPositioned { viewportWidthPx = it.size.width.toFloat() }
                .horizontalScroll(scrollState),
        ) {
            Box(
                modifier = Modifier
                    .width(with(density) { stripWidthPx.toDp() })
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (thumbnails.any { it != null }) {
                    val tileWidthDp = with(density) { tileWidthPx.toDp() }
                    Row(modifier = Modifier.fillMaxHeight()) {
                        repeat(totalThumbnailSlots) { slot ->
                            // Mehrere Kacheln können auf dasselbe extrahierte
                            // Einzelbild zeigen (s. Kommentar oben); jede Kachel
                            // hat eine feste, seitenverhältnis-treue Breite -
                            // am rechten Rand kann dadurch eine letzte, nur
                            // teilweise gefüllte Kachel entstehen (abgeschnitten
                            // durch den Clip des äußeren Streifens), das ist
                            // hier bewusst in Kauf genommen statt die Kacheln
                            // wieder zu verzerren.
                            val thumbnailIndex = (slot * thumbnails.size / totalThumbnailSlots).coerceIn(0, thumbnails.size - 1)
                            val bitmap = thumbnails[thumbnailIndex]
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    modifier = Modifier.width(tileWidthDp).fillMaxHeight(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Box(modifier = Modifier.width(tileWidthDp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant))
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

                // Positions-Griff: einzige Möglichkeit zu scrubben (bewusst
                // kein "an beliebiger Stelle tippen" mehr).
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

                if (extractWindow == null) {
                    // Trimm-Griffe an den Rändern des gewählten Bereichs. Auf
                    // [0, stripWidthPx - handleWidthPx] begrenzt, damit der
                    // Griff nie über den (geclippten) Streifenrand hinausragt
                    // - sonst würde er im Ausgangszustand (kein Trimm) halb
                    // unsichtbar abgeschnitten.
                    StripHandle(
                        xPx = (trimStartFraction * stripWidthPx - handleWidthPx)
                            .coerceIn(0f, (stripWidthPx - handleWidthPx).coerceAtLeast(0f)),
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
                } else {
                    // Schnellausschnitt-Fenster fester Länge: als Ganzes
                    // ziehbar (nicht an einzelnen Rändern), da die Länge
                    // bereits über die Längenauswahl feststeht - genau wie
                    // vom Nutzer beschrieben ("Ausschnitt auf der Zeitleiste
                    // verschieben").
                    val windowFraction = extractWindow.durationUs.toFloat() / clip.sourceDurationUs.toFloat()
                    val windowStartFraction = (extractWindow.startUs.toFloat() / clip.sourceDurationUs.toFloat())
                        .coerceIn(0f, (1f - windowFraction).coerceAtLeast(0f))
                    ExtractWindowHandle(
                        xPx = windowStartFraction * stripWidthPx,
                        widthPx = (windowFraction * stripWidthPx).coerceAtLeast(1f),
                        stripWidthPx = stripWidthPx,
                        sourceDurationUs = clip.sourceDurationUs,
                        windowDurationUs = extractWindow.durationUs,
                        getBaseStartUs = { extractWindow.startUs },
                        onNewStartUs = onExtractWindowMove,
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.titleMedium,
        )
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

/**
 * Durchsichtig hervorgehobenes Schnellausschnitt-Fenster fester Breite
 * [widthPx], das als Ganzes gezogen werden kann (verschiebt
 * [ExtractWindow.startUs], ändert nie die Länge). Auf
 * [0, sourceDurationUs - windowDurationUs] begrenzt.
 */
@Composable
private fun BoxScope.ExtractWindowHandle(
    xPx: Float,
    widthPx: Float,
    stripWidthPx: Float,
    sourceDurationUs: Long,
    windowDurationUs: Long,
    getBaseStartUs: () -> Long,
    onNewStartUs: (Long) -> Unit,
) {
    var baseUs by remember { mutableStateOf(0L) }
    var accumulatedPx by remember { mutableStateOf(0f) }
    val pxPerUs = stripWidthPx / sourceDurationUs.coerceAtLeast(1)
    val maxStartUs = (sourceDurationUs - windowDurationUs).coerceAtLeast(0)
    val dragState = rememberDraggableState { delta ->
        accumulatedPx += delta
        onNewStartUs((baseUs + (accumulatedPx / pxPerUs).toLong()).coerceIn(0, maxStartUs))
    }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .offset { IntOffset(xPx.roundToInt(), 0) }
            .fillMaxHeight()
            .width(with(density) { widthPx.toDp() })
            .border(BorderStroke(3.dp, MaterialTheme.colorScheme.tertiary))
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                onDragStarted = {
                    baseUs = getBaseStartUs()
                    accumulatedPx = 0f
                },
            ),
    )
}

/** Lädt [count] Vorschaubilder über die komplette Quelldatei (feste Anzahl, unabhängig vom Zoom). */
@Composable
private fun rememberFullSourceThumbnails(clip: VideoClip, count: Int): List<ImageBitmap?> {
    val context = LocalContext.current
    val state = produceState(initialValue = List<ImageBitmap?>(count) { null }, clip.id, count) {
        value = withContext(Dispatchers.IO) {
            extractThumbnails(context, clip.sourceUri, clip.sourceDurationUs, count)
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
