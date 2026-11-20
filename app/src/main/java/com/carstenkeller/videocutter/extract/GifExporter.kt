package com.carstenkeller.videocutter.extract

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Baut aus einem Zeitfenster eines Videos ein animiertes GIF (siehe
 * [GifEncoder] für die bewussten Vereinfachungen gegenüber einem vollwertigen
 * GIF-Encoder).
 */
class GifExporter(private val context: Context) {

    fun export(
        sourceUri: Uri,
        startUs: Long,
        endUs: Long,
        outputFile: File,
        onProgress: (Int) -> Unit,
    ) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, sourceUri)
            val windowDurationUs = (endUs - startUs).coerceAtLeast(1)
            val frameCount = ((windowDurationUs / 1_000_000.0) * FPS)
                .toInt()
                .coerceAtLeast(1)
            val frameStepUs = windowDurationUs / frameCount

            // Palette vorab aus Anfang/Mitte/Ende des Fensters bauen (statt
            // einer festen, bildunabhängigen Farbaufteilung - siehe
            // MedianCutPalette) - deckt auch einen Szenenwechsel innerhalb
            // des Fensters halbwegs ab, ohne dafür alle Frames im Speicher
            // halten zu müssen.
            val paletteSampleTimestamps = listOf(
                startUs,
                startUs + windowDurationUs / 2,
                (endUs - frameStepUs).coerceAtLeast(startUs),
            )
            val paletteSamples = paletteSampleTimestamps.flatMap { timeUs ->
                val rawFrame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST) ?: return@flatMap emptyList()
                val frame = scaleDown(rawFrame)
                val samples = samplePixels(frame, PALETTE_SAMPLES_PER_FRAME).toList()
                if (frame !== rawFrame) rawFrame.recycle()
                frame.recycle()
                samples
            }
            val palette = MedianCutPalette.build(paletteSamples.toIntArray())

            FileOutputStream(outputFile).use { out ->
                var encoder: GifEncoder? = null
                var framesWritten = 0
                for (i in 0 until frameCount) {
                    val timeUs = startUs + i * frameStepUs
                    // OPTION_CLOSEST statt OPTION_CLOSEST_SYNC: CLOSEST_SYNC
                    // springt zum nächsten Keyframe, und Handy-Videos haben
                    // meist nur alle 1-2s einen - alle Zeitpunkte eines
                    // kurzen Ausschnitts landeten so auf denselben ein bis
                    // zwei Bildern (Nutzerfeedback: "zwei Einzelbilder").
                    // CLOSEST dekodiert das exakte Bild, ist dafür langsamer.
                    val rawFrame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                        ?: continue
                    val frame = scaleDown(rawFrame)
                    if (encoder == null) {
                        encoder = GifEncoder(out, frame.width, frame.height, CENTISECONDS_PER_FRAME, palette)
                    }
                    encoder.writeFrame(frame)
                    if (frame !== rawFrame) rawFrame.recycle()
                    frame.recycle()
                    framesWritten++
                    onProgress(((i + 1) * 100) / frameCount)
                }
                val finalEncoder = encoder ?: error("Keine Frames im gewählten Zeitfenster gefunden")
                finalEncoder.finish()
                if (framesWritten == 0) error("Keine Frames im gewählten Zeitfenster gefunden")
            }
        } finally {
            retriever.release()
        }
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= MAX_WIDTH) return bitmap
        val scale = MAX_WIDTH.toFloat() / bitmap.width
        val matrix = Matrix().apply { postScale(scale, scale) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** Bis zu [maxSamples] gleichmäßig über das Bild verteilte Pixel (für die Palette, nicht die volle Pixelzahl). */
    private fun samplePixels(bitmap: Bitmap, maxSamples: Int): IntArray {
        val total = bitmap.width * bitmap.height
        if (total <= 0) return IntArray(0)
        val allPixels = IntArray(total)
        bitmap.getPixels(allPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        if (total <= maxSamples) return allPixels
        val step = total / maxSamples
        return IntArray(maxSamples) { i -> allPixels[(i * step).coerceAtMost(total - 1)] }
    }

    companion object {
        private const val FPS = 10.0
        private const val CENTISECONDS_PER_FRAME = (100.0 / FPS).toInt()
        private const val MAX_WIDTH = 480
        private const val PALETTE_SAMPLES_PER_FRAME = 5000
    }
}
