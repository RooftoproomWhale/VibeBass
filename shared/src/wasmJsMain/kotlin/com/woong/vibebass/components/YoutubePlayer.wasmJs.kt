package com.woong.vibebass.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
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
    modifier: Modifier
) {
    LaunchedEffect(videoId) {
        // window 전역 객체에 Kotlin 이벤트를 바인딩해둡니다.
        // JS Bridge Script가 실행될 때 Kotlin 단으로 현재 시간과 재생 상태를 전송할 수 있습니다.
        bindYoutubeBridgeCallbacks(
            onTimeUpdate = { time -> onTimeUpdate(time.toFloat()) },
            onStateChange = { playing -> onStateChange(playing) }
        )
        setupYoutubePlayerJs(videoId)
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text("YouTube 플레이어 영역 (DOM 연동 중...)", color = Color.White)
    }
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
