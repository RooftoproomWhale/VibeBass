package com.woong.vibebass.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import com.woong.vibebass.sync.AnchorPoint
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalWasmJsInterop::class)
@Composable
actual fun PdfSheetViewer(
    pdfSource: String,
    onScrollPositionChanged: (Float, Float?) -> Unit,
    onPdfFileSelected: (String, String) -> Unit,
    scrollTarget: AnchorPoint?,
    onRecordAnchor: (() -> Unit)?,
    modifier: Modifier
) {
    val latestScroll = rememberUpdatedState(onScrollPositionChanged)
    val latestSelection = rememberUpdatedState(onPdfFileSelected)
    val latestTarget = rememberUpdatedState(scrollTarget)
    val latestRecord = rememberUpdatedState(onRecordAnchor)
    DisposableEffect(Unit) {
        bindPdfCallbacks(
            onScroll = { pixel, page -> latestScroll.value(pixel.toFloat(), page.takeIf { it >= 0 && it.isFinite() }?.toFloat()) },
            onSelected = { name, url -> latestSelection.value(name, url) },
            onRecord = { repeat ->
                val record = latestRecord.value
                if (record == null) false else { if (!repeat) record(); true }
            }
        )
        onDispose { hidePdfViewerJs() }
    }
    LaunchedEffect(pdfSource) {
        // Reset the document before publishing its target; the bridge retains it during loading.
        initPdfViewerJs(pdfSource)
        snapshotFlow { latestTarget.value }.collect { target ->
            setPdfScrollTargetJs(target?.scrollPixel?.toDouble() ?: -1.0, target?.pagePosition?.toDouble() ?: -1.0)
        }
    }
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
private fun bindPdfCallbacks(onScroll: (Double, Double) -> Unit, onSelected: (String, String) -> Unit, onRecord: (Boolean) -> Boolean) {
    js("""
        window.onPdfScroll = function(scrollTop, pagePosition) { onScroll(scrollTop, pagePosition == null ? -1 : pagePosition); };
        window.onPdfFileSelected = function(name, url) { onSelected(name, url); };
        window.onPdfRecordShortcut = function(repeat) { return onRecord(repeat); };
    """)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun setPdfScrollTargetJs(pixel: Double, pagePosition: Double) {
    js("window.scrollToPdfPixel?.(pixel < 0 ? null : pixel, pagePosition < 0 ? null : pagePosition)")
}
