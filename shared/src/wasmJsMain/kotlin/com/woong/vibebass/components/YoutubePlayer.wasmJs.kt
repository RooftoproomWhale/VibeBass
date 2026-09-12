package com.woong.vibebass.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
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
    val latestTime = rememberUpdatedState(onTimeUpdate)
    val latestState = rememberUpdatedState(onStateChange)
    val latestVideo = rememberUpdatedState(onVideoIdFound)
    DisposableEffect(Unit) {
        bindYoutubeBridgeCallbacks(
            onTimeUpdate = { latestTime.value(it.toFloat()) },
            onStateChange = { latestState.value(it) },
            onVideoIdFound = { latestVideo.value(it) }
        )
        onDispose { hideYoutubePlayerJs() }
    }
    LaunchedEffect(videoId) { setupYoutubePlayerJs(videoId) }
    Box(modifier = modifier.mediaOverlayBounds("youtube"))
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun bindYoutubeBridgeCallbacks(
    onTimeUpdate: (Double) -> Unit,
    onStateChange: (Boolean) -> Unit,
    onVideoIdFound: (String) -> Unit
) {
    js("""
        window.onYoutubeTimeUpdate = function(time) { onTimeUpdate(time); };
        window.onYoutubeStateChange = function(playing) { onStateChange(playing); };
        window.onYoutubeVideoIdFound = function(id) { onVideoIdFound(id); };
    """)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun setupYoutubePlayerJs(videoId: String) {
    js("window.initYoutubePlayer?.(videoId)")
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun hideYoutubePlayerJs() {
    // Playback and callbacks survive compact tab changes; the next mount rebinds latest state.
    js("window.hideYoutubePlayer?.()")
}
