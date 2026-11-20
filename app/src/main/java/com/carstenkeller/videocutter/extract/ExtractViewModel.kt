package com.carstenkeller.videocutter.extract

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.carstenkeller.videocutter.export.ExportQuality
import com.carstenkeller.videocutter.export.ExportResolution
import com.carstenkeller.videocutter.export.VideoExporter
import com.carstenkeller.videocutter.storage.MediaStoreSaver
import com.carstenkeller.videocutter.timeline.VideoClip
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
class ExtractViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ExtractUiState>(ExtractUiState.Idle)
    val uiState: StateFlow<ExtractUiState> = _uiState

    private var progressPollJob: Job? = null

    /**
     * [windowStartUs]/[windowEndUs] sind absolute Positionen innerhalb der
     * Quelldatei (gleiche Zeitbasis wie [VideoClip.trimStartUs]/[trimEndUs]),
     * nicht relativ zum sichtbaren Clip-Ausschnitt.
     */
    fun startExtract(
        sourceUri: Uri,
        sourceDurationUs: Long,
        windowStartUs: Long,
        windowEndUs: Long,
        format: ExtractFormat,
    ) {
        if (_uiState.value is ExtractUiState.Extracting) return
        _uiState.value = ExtractUiState.Extracting(0)

        val context = getApplication<Application>()
        val fileName = "clip_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}"

        when (format) {
            ExtractFormat.MP4 -> startMp4Extract(sourceUri, sourceDurationUs, windowStartUs, windowEndUs, fileName)
            ExtractFormat.GIF -> startGifExtract(sourceUri, windowStartUs, windowEndUs, fileName)
        }
    }

    private fun startMp4Extract(
        sourceUri: Uri,
        sourceDurationUs: Long,
        windowStartUs: Long,
        windowEndUs: Long,
        fileName: String,
    ) {
        val context = getApplication<Application>()
        val outputFile = File(context.cacheDir, "$fileName.mp4")
        val clip = VideoClip(
            sourceUri = sourceUri,
            sourceDurationUs = sourceDurationUs,
            trimStartUs = windowStartUs,
            trimEndUs = windowEndUs,
        )

        val transformer = VideoExporter(context).export(
            clips = listOf(clip),
            resolution = ExportResolution.ORIGINAL,
            quality = ExportQuality.ORIGINAL,
            outputFile = outputFile,
            onCompleted = {
                progressPollJob?.cancel()
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            MediaStoreSaver.saveVideoToAlbum(context, outputFile, "$fileName.mp4")
                            outputFile.delete()
                            _uiState.value = ExtractUiState.Success("Videoausschnitt im Album \"Video Cutter\" gespeichert")
                        } catch (t: Throwable) {
                            _uiState.value = ExtractUiState.Error(t.message ?: "Speichern fehlgeschlagen")
                        }
                    }
                }
            },
            onError = { throwable ->
                progressPollJob?.cancel()
                outputFile.delete()
                _uiState.value = ExtractUiState.Error(throwable.message ?: "Export fehlgeschlagen")
            },
        )

        progressPollJob = viewModelScope.launch {
            val progressHolder = ProgressHolder()
            while (isActive) {
                if (transformer.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                    _uiState.value = ExtractUiState.Extracting(progressHolder.progress)
                }
                delay(200)
            }
        }
    }

    private fun startGifExtract(sourceUri: Uri, windowStartUs: Long, windowEndUs: Long, fileName: String) {
        val context = getApplication<Application>()
        val outputFile = File(context.cacheDir, "$fileName.gif")

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                try {
                    GifExporter(context).export(
                        sourceUri = sourceUri,
                        startUs = windowStartUs,
                        endUs = windowEndUs,
                        outputFile = outputFile,
                        onProgress = { percent -> _uiState.value = ExtractUiState.Extracting(percent) },
                    )
                    MediaStoreSaver.saveImageToAlbum(context, outputFile, "$fileName.gif")
                    outputFile.delete()
                    _uiState.value = ExtractUiState.Success("GIF im Album \"Video Cutter\" gespeichert")
                } catch (t: Throwable) {
                    outputFile.delete()
                    _uiState.value = ExtractUiState.Error(t.message ?: "GIF-Export fehlgeschlagen")
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.value = ExtractUiState.Idle
    }
}
