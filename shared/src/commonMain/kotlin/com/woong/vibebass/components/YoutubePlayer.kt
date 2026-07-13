package com.woong.vibebass.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun YoutubePlayer(
    videoId: String,
    currentTime: Float,
    isPlaying: Boolean,
    onTimeUpdate: (Float) -> Unit,
    onStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
)
