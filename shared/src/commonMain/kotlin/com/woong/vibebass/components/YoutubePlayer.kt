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
    onVideoIdFound: (String) -> Unit, // 유튜브 영상 자동 검색 시 videoId 콜백 연동
    modifier: Modifier = Modifier
)
