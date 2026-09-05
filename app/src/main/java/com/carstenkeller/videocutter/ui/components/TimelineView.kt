package com.carstenkeller.videocutter.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carstenkeller.videocutter.timeline.VideoClip

@Composable
fun TimelineView(
    clips: List<VideoClip>,
    selectedClipId: String?,
    onSelectClip: (String) -> Unit,
    onTrimChange: (clipId: String, trimStartUs: Long, trimEndUs: Long) -> Unit,
    onMoveClip: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Text("Zeitleiste", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .height(88.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            clips.forEachIndexed { index, clip ->
                val durationSeconds = clip.durationUs / 1_000_000f
                val widthDp = (durationSeconds * 60f).coerceAtLeast(56f).dp
                ClipBlock(
                    index = index,
                    isSelected = clip.id == selectedClipId,
                    widthDp = widthDp,
                    canMoveLeft = index > 0,
                    canMoveRight = index < clips.lastIndex,
                    onClick = { onSelectClip(clip.id) },
                    onMoveLeft = { onMoveClip(index, index - 1) },
                    onMoveRight = { onMoveClip(index, index + 1) },
                )
            }
        }

        val selectedClip = clips.firstOrNull { it.id == selectedClipId }
        if (selectedClip != null) {
            TrimControls(clip = selectedClip, onTrimChange = onTrimChange)
        }
    }
}

@Composable
private fun ClipBlock(
    index: Int,
    isSelected: Boolean,
    widthDp: Dp,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onClick: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(widthDp)
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .border(
                BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                ),
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMoveLeft, enabled = canMoveLeft) {
                    Text("‹", style = MaterialTheme.typography.titleLarge)
                }
                Text("Clip ${index + 1}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = onMoveRight, enabled = canMoveRight) {
                    Text("›", style = MaterialTheme.typography.titleLarge)
                }
            }
        } else {
            Text("Clip ${index + 1}", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TrimControls(
    clip: VideoClip,
    onTrimChange: (clipId: String, trimStartUs: Long, trimEndUs: Long) -> Unit,
) {
    val durationSeconds = (clip.sourceDurationUs / 1_000_000f).coerceAtLeast(0.2f)
    var range by remember(clip.id) {
        mutableStateOf((clip.trimStartUs / 1_000_000f)..(clip.trimEndUs / 1_000_000f))
    }
    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(
            "Trimmen: %.1fs – %.1fs".format(range.start, range.endInclusive),
            style = MaterialTheme.typography.bodySmall,
        )
        RangeSlider(
            value = range,
            onValueChange = { newRange ->
                range = newRange
                onTrimChange(
                    clip.id,
                    (newRange.start * 1_000_000).toLong(),
                    (newRange.endInclusive * 1_000_000).toLong(),
                )
            },
            valueRange = 0f..durationSeconds,
        )
    }
}
