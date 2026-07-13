package com.woong.vibebass.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.coroutines.launch

@OptIn(ExperimentalWasmJsInterop::class)
@Composable
actual fun PdfSheetViewer(
    pdfSource: String,
    scrollState: LazyListState,
    onPdfFileSelected: (String) -> Unit,
    modifier: Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pdfSource) {
        if (pdfSource.isNotEmpty()) {
            initPdfViewerJs(pdfSource)
        }
        
        // 스크롤 동기화 등록
        bindPdfScrollCallback { scrollTop ->
            val itemHeight = 300f
            val index = (scrollTop / itemHeight).toInt()
            val offset = (scrollTop % itemHeight).toInt()
            
            coroutineScope.launch {
                scrollState.scrollToItem(index, offset)
            }
        }

        // 파일 선택 완료 콜백 브릿지 등록
        bindPdfFileSelectedCallback { fileName ->
            onPdfFileSelected(fileName)
        }
    }

    val index = scrollState.firstVisibleItemIndex
    val offset = scrollState.firstVisibleItemScrollOffset
    LaunchedEffect(index, offset) {
        val itemHeight = 300f
        val calculatedPixel = (index * itemHeight) + offset
        scrollToPdfPixelJs(calculatedPixel.toDouble())
    }

    Box(modifier = modifier)
}

@OptIn(ExperimentalWasmJsInterop::class)
actual fun triggerPdfUpload() {
    js("""
        if (typeof window.triggerPdfUpload === 'function') {
            window.triggerPdfUpload();
        } else {
            console.warn('window.triggerPdfUpload 함수가 정의되지 않았습니다.');
        }
    """)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun initPdfViewerJs(pdfUrl: String) {
    js("""
        if (typeof window.initPdfViewer === 'function') {
            window.initPdfViewer(pdfUrl);
        }
    """)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun scrollToPdfPixelJs(pixel: Double) {
    js("""
        if (typeof window.scrollToPdfPixel === 'function') {
            var container = document.getElementById('pdf-viewer-container');
            if (container && Math.abs(container.scrollTop - pixel) > 5) {
                window.scrollToPdfPixel(pixel);
            }
        }
    """)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun bindPdfScrollCallback(onScroll: (Double) -> Unit) {
    js("""
        window.onPdfScroll = function(scrollTop) {
            onScroll(scrollTop);
        };
    """)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun bindPdfFileSelectedCallback(onSelected: (String) -> Unit) {
    js("""
        window.onPdfFileSelected = function(fileName) {
            onSelected(fileName);
        };
    """)
}
