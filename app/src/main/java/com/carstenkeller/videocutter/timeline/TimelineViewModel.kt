package com.carstenkeller.videocutter.timeline

import android.net.Uri
import androidx.lifecycle.ViewModel
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class TimelineViewModel : ViewModel() {

    private val _state = MutableStateFlow(TimelineState())
    val state: StateFlow<TimelineState> = _state

    fun addClip(uri: Uri, durationUs: Long) {
        val clip = VideoClip(sourceUri = uri, sourceDurationUs = durationUs)
        _state.update { current ->
            current.copy(clips = current.clips + clip, selectedClipId = clip.id)
        }
    }

    fun selectClip(clipId: String) {
        _state.update { it.copy(selectedClipId = clipId) }
    }

    fun deleteClip(clipId: String) {
        _state.update { current ->
            val remaining = current.clips.filterNot { it.id == clipId }
            current.copy(
                clips = remaining,
                selectedClipId = if (current.selectedClipId == clipId) {
                    remaining.firstOrNull()?.id
                } else {
                    current.selectedClipId
                },
            )
        }
    }

    fun updateTrim(clipId: String, trimStartUs: Long, trimEndUs: Long) {
        _state.update { current ->
            current.copy(
                clips = current.clips.map { clip ->
                    if (clip.id != clipId) return@map clip
                    val minDuration = VideoClip.MIN_DURATION_US
                    val start = trimStartUs.coerceIn(0, (clip.sourceDurationUs - minDuration).coerceAtLeast(0))
                    val end = trimEndUs.coerceIn(
                        (start + minDuration).coerceAtMost(clip.sourceDurationUs),
                        clip.sourceDurationUs,
                    )
                    clip.copy(trimStartUs = start, trimEndUs = end)
                },
            )
        }
    }

    /** Schneidet den Clip [clipId] an [positionInClipUs] (relativ zum getrimmten Clip-Anfang) in zwei Clips. */
    fun splitClip(clipId: String, positionInClipUs: Long) {
        _state.update { current ->
            val index = current.clips.indexOfFirst { it.id == clipId }
            if (index == -1) return@update current
            val clip = current.clips[index]
            val splitAtSource = clip.trimStartUs + positionInClipUs
            val minDuration = VideoClip.MIN_DURATION_US
            if (splitAtSource <= clip.trimStartUs + minDuration || splitAtSource >= clip.trimEndUs - minDuration) {
                return@update current
            }
            val firstHalf = clip.copy(trimEndUs = splitAtSource)
            val secondHalf = clip.copy(id = UUID.randomUUID().toString(), trimStartUs = splitAtSource)
            val newClips = current.clips.toMutableList().apply {
                removeAt(index)
                addAll(index, listOf(firstHalf, secondHalf))
            }
            current.copy(clips = newClips, selectedClipId = firstHalf.id)
        }
    }

    fun moveClip(fromIndex: Int, toIndex: Int) {
        _state.update { current ->
            if (fromIndex == toIndex || fromIndex !in current.clips.indices || toIndex !in current.clips.indices) {
                return@update current
            }
            val newClips = current.clips.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            current.copy(clips = newClips)
        }
    }
}
