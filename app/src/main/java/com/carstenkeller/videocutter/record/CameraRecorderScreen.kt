package com.carstenkeller.videocutter.record

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.carstenkeller.videocutter.media.MediaDurationReader
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun CameraRecorderScreen(
    onVideoRecorded: (Uri, Long) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(hasPermission(context, Manifest.permission.CAMERA)) }
    var hasAudioPermission by remember { mutableStateOf(hasPermission(context, Manifest.permission.RECORD_AUDIO)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        hasCameraPermission = granted[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasAudioPermission = granted[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
    }

    LaunchedEffect(Unit) {
        val missing = buildList {
            if (!hasCameraPermission) add(Manifest.permission.CAMERA)
            if (!hasAudioPermission) add(Manifest.permission.RECORD_AUDIO)
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }

    if (!hasCameraPermission) {
        PermissionRequiredNotice(
            onRetry = { permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) },
            onCancel = onCancel,
        )
        return
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }

    val previewView = remember { PreviewView(context) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    DisposableEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()
                val newVideoCapture = VideoCapture.withOutput(recorder)
                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, newVideoCapture)
                    videoCapture = newVideoCapture
                }
            },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            activeRecording?.stop()
            activeRecording = null
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    LaunchedEffect(isRecording) {
        elapsedSeconds = 0
        while (isRecording) {
            delay(1000)
            elapsedSeconds += 1
        }
    }

    fun toggleRecording() {
        val capture = videoCapture ?: return
        val current = activeRecording
        if (current != null) {
            current.stop()
            activeRecording = null
            return
        }
        val outputDir = File(context.cacheDir, "recordings").apply { mkdirs() }
        val outputFile = File(outputDir, "recording_${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(outputFile).build()
        var pendingRecording = capture.output.prepareRecording(context, outputOptions)
        if (hasAudioPermission) {
            pendingRecording = pendingRecording.withAudioEnabled()
        }
        activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
            if (event is VideoRecordEvent.Finalize) {
                isRecording = false
                activeRecording = null
                if (!event.hasError() && outputFile.exists()) {
                    val uri = Uri.fromFile(outputFile)
                    onVideoRecorded(uri, MediaDurationReader.readDurationUs(context, uri))
                }
            }
        }
        isRecording = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        TextButton(
            onClick = onCancel,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        ) {
            Text("Abbrechen", color = Color.White)
        }

        if (isRecording) {
            Text(
                text = "● %02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60),
                color = Color.Red,
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = {
                    if (!isRecording) {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                            CameraSelector.LENS_FACING_BACK
                        } else {
                            CameraSelector.LENS_FACING_FRONT
                        }
                    }
                },
                enabled = !isRecording,
            ) {
                Text("Kamera wechseln", color = Color.White)
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(color = if (isRecording) Color.Red else Color.White, shape = CircleShape)
                    .clickable { toggleRecording() },
            )

            Box(modifier = Modifier.size(1.dp))
        }
    }
}

@Composable
private fun PermissionRequiredNotice(onRetry: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            "Für die Videoaufnahme werden Kamera- und Mikrofon-Berechtigung benötigt.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onRetry) { Text("Berechtigungen erteilen") }
        TextButton(onClick = onCancel) { Text("Abbrechen") }
    }
}

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
