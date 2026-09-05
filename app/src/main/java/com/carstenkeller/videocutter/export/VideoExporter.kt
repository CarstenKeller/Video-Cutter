package com.carstenkeller.videocutter.export

import android.content.Context
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.carstenkeller.videocutter.timeline.VideoClip
import java.io.File

/**
 * Baut aus den Timeline-Clips eine Media3 [Composition] und startet den Export
 * über [Transformer].
 *
 * ACHTUNG (Transparenz-Hinweis): Diese Sandbox hat weder Android-SDK noch
 * Netzwerkzugriff auf Googles Maven-Repository, daher konnte die exakte
 * Media3-Transformer-API hier NICHT kompiliert/verifiziert werden. Am
 * unsichersten sind der Importpfad von [Effects] sowie die Builder-Form von
 * [DefaultEncoderFactory]/[VideoEncoderSettings]. Bei Build-Fehlern in dieser
 * Datei bitte gegen die aktuelle androidx.media3:media3-transformer-Doku
 * gegenprüfen.
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
        val editedItems = clips.map { it.toEditedMediaItem(resolution) }
        val sequence = EditedMediaItemSequence(editedItems)
        val composition = Composition.Builder(listOf(sequence)).build()

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

    private fun VideoClip.toEditedMediaItem(resolution: ExportResolution): EditedMediaItem {
        val mediaItem = MediaItem.Builder()
            .setUri(sourceUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(trimStartUs / 1000)
                    .setEndPositionMs(trimEndUs / 1000)
                    .build(),
            )
            .build()

        val builder = EditedMediaItem.Builder(mediaItem).setRemoveAudio(false)
        val videoEffects: List<Effect> = resolution.targetHeight?.let { height ->
            listOf(Presentation.createForHeight(height))
        } ?: emptyList()
        if (videoEffects.isNotEmpty()) {
            builder.setEffects(Effects(emptyList(), videoEffects))
        }
        return builder.build()
    }
}
