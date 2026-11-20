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

            FileOutputStream(outputFile).use { out ->
                var encoder: GifEncoder? = null
                var framesWritten = 0
                for (i in 0 until frameCount) {
                    val timeUs = startUs + i * frameStepUs
                    val rawFrame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: continue
                    val frame = scaleDown(rawFrame)
                    if (encoder == null) {
                        encoder = GifEncoder(out, frame.width, frame.height, CENTISECONDS_PER_FRAME)
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

    companion object {
        private const val FPS = 10.0
        private const val CENTISECONDS_PER_FRAME = (100.0 / FPS).toInt()
        private const val MAX_WIDTH = 480
    }
}
