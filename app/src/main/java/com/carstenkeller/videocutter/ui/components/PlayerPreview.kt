package com.carstenkeller.videocutter.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView

@Composable
fun PlayerPreview(player: Player, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            PlayerView(context).apply {
                useController = true
                this.player = player
            }
        },
        update = { view -> view.player = player },
    )
}
