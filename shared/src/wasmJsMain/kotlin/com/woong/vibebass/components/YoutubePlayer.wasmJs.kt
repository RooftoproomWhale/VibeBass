package com.woong.vibebass.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
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
    LaunchedEffect(videoId) {
        bindYoutubeBridgeCallbacks(
            onTimeUpdate = { time -> onTimeUpdate(time.toFloat()) },
            onStateChange = { playing -> onStateChange(playing) },
            onVideoIdFound = { foundId -> onVideoIdFound(foundId) }
        )
        setupYoutubePlayerJs(videoId)
    }

    // 오버레이 유튜브 컨테이너의 크기/위치는 CSS calc 매핑으로 이중 렌더링 오차를 완전히 격리 차단
    Box(
        modifier = modifier.background(Color.Black)
    )
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun bindYoutubeBridgeCallbacks(
    onTimeUpdate: (Double) -> Unit,
    onStateChange: (Boolean) -> Unit,
    onVideoIdFound: (String) -> Unit
) {
    js("""
        window.onYoutubeTimeUpdate = function(time) {
            onTimeUpdate(time);
        };
        window.onYoutubeStateChange = function(isPlaying) {
            onStateChange(isPlaying);
        };
        window.onYoutubeVideoIdFound = function(foundId) {
            onVideoIdFound(foundId);
        };
    """)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun setupYoutubePlayerJs(videoId: String) {
    js("""
        if (typeof window.initYoutubePlayer === 'function') {
            window.initYoutubePlayer(videoId);
        } else {
            console.warn('window.initYoutubePlayer 함수가 아직 정의되지 않았습니다.');
        }
    """)
}
