package com.woong.vibebass.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
@Composable
actual fun PdfSheetViewer(
    pdfSource: String,
    onScrollPositionChanged: (Float) -> Unit,
    onPdfFileSelected: (String, String) -> Unit,
    modifier: Modifier
) {
    val latestScroll = rememberUpdatedState(onScrollPositionChanged)
    val latestSelection = rememberUpdatedState(onPdfFileSelected)
    DisposableEffect(Unit) {
        bindPdfCallbacks(
            onScroll = { latestScroll.value(it.toFloat()) },
            onSelected = { name, url -> latestSelection.value(name, url) }
        )
        onDispose { hidePdfViewerJs() }
    }
    LaunchedEffect(pdfSource) { initPdfViewerJs(pdfSource) }
    Box(modifier = modifier.mediaOverlayBounds("pdf"))
}

@Composable
internal fun Modifier.mediaOverlayBounds(kind: String): Modifier {
    val density = LocalDensity.current.density
    return onGloballyPositioned { coordinates ->
        val origin = coordinates.positionInWindow()
        val clipped = coordinates.boundsInWindow()
        updateMediaBoundsJs(
            kind,
            (origin.x / density).toDouble(), (origin.y / density).toDouble(),
            (coordinates.size.width / density).toDouble(), (coordinates.size.height / density).toDouble(),
            (clipped.left / density).toDouble(), (clipped.top / density).toDouble(),
            (clipped.right / density).toDouble(), (clipped.bottom / density).toDouble()
        )
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun updateMediaBoundsJs(
    kind: String, left: Double, top: Double, width: Double, height: Double,
    clipLeft: Double, clipTop: Double, clipRight: Double, clipBottom: Double
) {
    js("""
        const update = kind === 'pdf' ? window.updatePdfPosition : window.updateYoutubePosition;
        if (update) update(left, top, width, height, clipLeft, clipTop, clipRight, clipBottom);
    """)
}

@OptIn(ExperimentalWasmJsInterop::class)
actual fun triggerPdfUpload() {
    js("window.triggerPdfUpload?.()")
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun initPdfViewerJs(pdfUrl: String) {
    js("window.initPdfViewer?.(pdfUrl)")
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun hidePdfViewerJs() {
    // Keep the file callback and document alive while the compact UI shows its other tab.
    js("window.hidePdfViewer?.()")
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun bindPdfCallbacks(onScroll: (Double) -> Unit, onSelected: (String, String) -> Unit) {
    js("""
        window.onPdfScroll = function(scrollTop) { onScroll(scrollTop); };
        window.onPdfFileSelected = function(name, url) { onSelected(name, url); };
    """)
}
