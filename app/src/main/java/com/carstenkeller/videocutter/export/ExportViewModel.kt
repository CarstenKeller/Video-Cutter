package com.carstenkeller.videocutter.export

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
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
class ExportViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val uiState: StateFlow<ExportUiState> = _uiState

    private var progressPollJob: Job? = null

    fun startExport(clips: List<VideoClip>, resolution: ExportResolution, quality: ExportQuality) {
        if (clips.isEmpty() || _uiState.value is ExportUiState.Exporting) return
        _uiState.value = ExportUiState.Exporting(0)

        val context = getApplication<Application>()
        val exporter = VideoExporter(context)
        val fileName = "video_cutter_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}"
        val outputFile = File(context.cacheDir, "$fileName.mp4")

        val transformer = exporter.export(
            clips = clips,
            resolution = resolution,
            quality = quality,
            outputFile = outputFile,
            onCompleted = {
                progressPollJob?.cancel()
                viewModelScope.launch { finishExport(outputFile, fileName) }
            },
            onError = { throwable ->
                progressPollJob?.cancel()
                outputFile.delete()
                _uiState.value = ExportUiState.Error(throwable.message ?: "Export fehlgeschlagen")
            },
        )

        progressPollJob = viewModelScope.launch {
            val progressHolder = ProgressHolder()
            while (isActive) {
                if (transformer.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                    _uiState.value = ExportUiState.Exporting(progressHolder.progress)
                }
                delay(200)
            }
        }
    }

    private suspend fun finishExport(outputFile: File, displayName: String) {
        withContext(Dispatchers.IO) {
            try {
                MediaStoreSaver.saveVideoToAlbum(getApplication(), outputFile, "$displayName.mp4")
                outputFile.delete()
                _uiState.value = ExportUiState.Success("Video im Album \"Video Cutter\" gespeichert")
            } catch (t: Throwable) {
                _uiState.value = ExportUiState.Error(t.message ?: "Speichern fehlgeschlagen")
            }
        }
    }

    fun consumeMessage() {
        _uiState.value = ExportUiState.Idle
    }
}
