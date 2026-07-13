package com.woong.vibebass.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
@Composable
actual fun PdfSheetViewer(
    pdfSource: String,
    scrollState: LazyListState,
    modifier: Modifier
) {
    // WasmJs 타겟에서는 pdf.js를 이용해 로컬 PDF 파일을 메모리 Canvas에 그리고,
    // 그 Canvas 이미지를 Compose ImageBitmap으로 가져와 LazyColumn에 렌더링하는 형태로 설계합니다.
    LaunchedEffect(pdfSource) {
        loadLocalPdfJs(pdfSource)
    }

    // 1단계 MVP 검증을 위한 20개의 더미 악보 슬라이스(높이 300dp) 세로 렌더링
    LazyColumn(
        state = scrollState,
        modifier = modifier
    ) {
        items(20) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(if (index % 2 == 0) Color.LightGray else Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "악보 페이지 ${index + 1} (소스: $pdfSource)", color = Color.Black)
            }
        }
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun loadLocalPdfJs(pdfSource: String) {
    js("""
        console.log('로컬 PDF 로드 시작:', pdfSource);
        // pdf.js를 활용하여 바이너리 파싱 후 Compose Canvas와 연동하는 브릿지 로직 시뮬레이션
    """)
}
