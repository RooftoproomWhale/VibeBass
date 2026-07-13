package com.woong.vibebass

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
fun main() {
    ComposeViewport {
        WithFontResourcesLoaded {
            App()
        }
    }
    
    // Compose Canvas 생성 직후 DOM 계층 구조 상 오버레이 컨테이너들을 가장 뒤로(최상단) 정렬
    adjustOverlayLayersJs()
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun adjustOverlayLayersJs() {
    js("""
        if (typeof window.adjustOverlayLayers === 'function') {
            window.adjustOverlayLayers();
        }
    """)
}