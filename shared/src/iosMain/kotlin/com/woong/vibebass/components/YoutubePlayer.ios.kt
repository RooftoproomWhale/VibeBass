package com.woong.vibebass.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun YoutubePlayer(
    videoId: String,
    currentTime: Float,
    isPlaying: Boolean,
    onTimeUpdate: (Float) -> Unit,
    onStateChange: (Boolean) -> Unit,
    onVideoIdFound: (String) -> Unit,
    modifier: Modifier
) {
    // iOS 용 유튜브 플레이어 뼈대
}
