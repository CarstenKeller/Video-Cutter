package com.carstenkeller.videocutter.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carstenkeller.videocutter.record.CameraRecorderScreen
import com.carstenkeller.videocutter.timeline.TimelineViewModel
import com.carstenkeller.videocutter.ui.screens.EditorScreen
import com.carstenkeller.videocutter.ui.screens.HomeScreen

@Composable
fun VideoCutterApp() {
    val timelineViewModel: TimelineViewModel = viewModel()
    val state by timelineViewModel.state.collectAsState()
    var showRecorder by remember { mutableStateOf(false) }

    when {
        showRecorder -> CameraRecorderScreen(
            onVideoRecorded = { uri, durationUs ->
                timelineViewModel.addClip(uri, durationUs)
                showRecorder = false
            },
            onCancel = { showRecorder = false },
        )
        state.clips.isEmpty() -> HomeScreen(
            onVideoImported = timelineViewModel::addClip,
            onRecordRequested = { showRecorder = true },
        )
        else -> EditorScreen(
            timelineViewModel = timelineViewModel,
            onRecordRequested = { showRecorder = true },
        )
    }
}
