package com.carstenkeller.videocutter.timeline

import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects

/**
 * Baut aus den Timeline-Clips eine Media3-[Composition]. Wird von der
 * Vorschau (CompositionPlayer) und vom Export (Transformer) gleichermaßen
 * benutzt, damit beide exakt dasselbe zeigen.
 *
 * [VideoClip.sourceDurationUs] wird als `durationUs` gesetzt: Media3 erwartet
 * dort die Dauer der ganzen Quelldatei *vor* dem Clipping, und der
 * CompositionPlayer stürzt ohne diesen Wert ab.
 */
@UnstableApi
fun buildComposition(clips: List<VideoClip>, videoEffects: List<Effect> = emptyList()): Composition {
    val items = clips.map { clip ->
        val mediaItem = MediaItem.Builder()
            .setUri(clip.sourceUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.trimStartUs / 1000)
                    .setEndPositionMs(clip.trimEndUs / 1000)
                    .build(),
            )
            .build()
        val builder = EditedMediaItem.Builder(mediaItem).setDurationUs(clip.sourceDurationUs)
        if (videoEffects.isNotEmpty()) {
            builder.setEffects(Effects(emptyList(), videoEffects))
        }
        builder.build()
    }
    return Composition.Builder(EditedMediaItemSequence.withAudioAndVideoFrom(items)).build()
}

data class ClipPosition(val clipIndex: Int, val positionInClipUs: Long)

/**
 * Die Vorschau kennt nur eine durchgehende Position über alle Clips
 * (der CompositionPlayer stellt die ganze Composition als *ein* Element dar).
 * Diese Funktion rechnet sie auf Clip-Index + Position im Clip um.
 */
fun clipPositionAt(clips: List<VideoClip>, elapsedUs: Long): ClipPosition {
    if (clips.isEmpty()) return ClipPosition(0, 0L)
    var remainingUs = elapsedUs.coerceAtLeast(0)
    for (i in clips.indices) {
        val duration = clips[i].durationUs
        if (i == clips.lastIndex || remainingUs < duration) {
            return ClipPosition(i, remainingUs.coerceIn(0, duration))
        }
        remainingUs -= duration
    }
    return ClipPosition(clips.lastIndex, 0L)
}

/** Umkehrung von [clipPositionAt]: Clip-Index + Position im Clip → durchgehende Position. */
fun elapsedUsAt(clips: List<VideoClip>, clipIndex: Int, positionInClipUs: Long): Long {
    val index = clipIndex.coerceIn(0, (clips.size - 1).coerceAtLeast(0))
    val before = clips.take(index).sumOf { it.durationUs }
    val duration = clips.getOrNull(index)?.durationUs ?: 0L
    return before + positionInClipUs.coerceIn(0, duration)
}
