package com.carstenkeller.videocutter.timeline

data class TimelineState(
    val clips: List<VideoClip> = emptyList(),
    val selectedClipId: String? = null,
)
