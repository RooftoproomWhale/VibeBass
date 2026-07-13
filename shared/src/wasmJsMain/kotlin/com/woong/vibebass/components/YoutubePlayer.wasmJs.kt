package com.woong.vibebass.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
@Composable
actual fun YoutubePlayer(
    videoId: String,
    currentTime: Float,
    isPlaying: Boolean,
    onTimeUpdate: (Float) -> Unit,
    onStateChange: (Boolean) -> Unit,
    modifier: Modifier
) {
    LaunchedEffect(videoId) {
        bindYoutubeBridgeCallbacks(
            onTimeUpdate = { time -> onTimeUpdate(time.toFloat()) },
            onStateChange = { playing -> onStateChange(playing) }
        )
        setupYoutubePlayerJs(videoId)
    }

    // Compose Box 영역의 브라우저 윈도우 상 실제 좌표 및 가로/세로 픽셀을 측정하여 유튜브 플레이어 위치 밀착 동기화
    Box(
        modifier = modifier
            .background(Color.Black)
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                val size = coordinates.size
                updateYoutubePositionJs(
                    left = position.x.toDouble(),
                    top = position.y.toDouble(),
                    width = size.width.toDouble(),
                    height = size.height.toDouble()
                )
            }
    )
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun updateYoutubePositionJs(left: Double, top: Double, width: Double, height: Double) {
    js("""
        if (typeof window.updateYoutubePosition === 'function') {
            window.updateYoutubePosition(left, top, width, height);
        }
    """)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun bindYoutubeBridgeCallbacks(
    onTimeUpdate: (Double) -> Unit,
    onStateChange: (Boolean) -> Unit
) {
    js("""
        window.onYoutubeTimeUpdate = function(time) {
            onTimeUpdate(time);
        };
        window.onYoutubeStateChange = function(isPlaying) {
            onStateChange(isPlaying);
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
