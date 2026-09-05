package com.carstenkeller.videocutter.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carstenkeller.videocutter.timeline.TimelineViewModel
import com.carstenkeller.videocutter.ui.screens.EditorScreen
import com.carstenkeller.videocutter.ui.screens.HomeScreen

@Composable
fun VideoCutterApp() {
    val timelineViewModel: TimelineViewModel = viewModel()
    val state by timelineViewModel.state.collectAsState()

    if (state.clips.isEmpty()) {
        HomeScreen(onVideoImported = timelineViewModel::addClip)
    } else {
        EditorScreen(timelineViewModel = timelineViewModel)
    }
}
