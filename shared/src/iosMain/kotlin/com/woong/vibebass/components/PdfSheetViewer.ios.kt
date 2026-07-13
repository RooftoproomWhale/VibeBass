package com.woong.vibebass.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PdfSheetViewer(
    pdfSource: String,
    scrollState: LazyListState,
    onPdfFileSelected: (String, String) -> Unit,
    modifier: Modifier
) {
    // iOS 용 PDF 뷰어 뼈대
}

actual fun triggerPdfUpload() {
    // iOS 파일 다이얼로그 호출 뼈대
}
