package com.carstenkeller.videocutter.export

import android.content.Context
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.carstenkeller.videocutter.timeline.VideoClip
import com.carstenkeller.videocutter.timeline.buildComposition
import java.io.File

/**
 * Exportiert die Timeline-Clips über [Transformer]. Die Composition kommt aus
 * [buildComposition] - derselben Funktion, die auch die Vorschau benutzt.
 */
@UnstableApi
class VideoExporter(private val context: Context) {

    fun export(
        clips: List<VideoClip>,
        resolution: ExportResolution,
        quality: ExportQuality,
        outputFile: File,
        onCompleted: () -> Unit,
        onError: (Throwable) -> Unit,
    ): Transformer {
        val videoEffects = resolution.targetHeight?.let { listOf(Presentation.createForHeight(it)) } ?: emptyList()
        val composition = buildComposition(clips, videoEffects)

        val transformerBuilder = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    onCompleted()
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    onError(exportException)
                }
            })

        quality.videoBitrateBps?.let { bitrate ->
            val encoderSettings = VideoEncoderSettings.Builder()
                .setBitrate(bitrate)
                .build()
            transformerBuilder.setEncoderFactory(
                DefaultEncoderFactory.Builder(context)
                    .setRequestedVideoEncoderSettings(encoderSettings)
                    .build(),
            )
        }

        val transformer = transformerBuilder.build()
        transformer.start(composition, outputFile.absolutePath)
        return transformer
    }
}
