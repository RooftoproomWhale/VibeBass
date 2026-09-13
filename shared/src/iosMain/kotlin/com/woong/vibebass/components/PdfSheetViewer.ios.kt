package com.woong.vibebass.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woong.vibebass.sync.AnchorPoint

@Composable
actual fun PdfSheetViewer(
    pdfSource: String,
    onScrollPositionChanged: (Float, Float?) -> Unit,
    onPdfFileSelected: (String, String) -> Unit,
    scrollTarget: AnchorPoint?,
    onRecordAnchor: (() -> Unit)?,
    modifier: Modifier
) {
    // iOS 용 PDF 뷰어 뼈대
}

actual fun triggerPdfUpload() {
    // iOS 파일 다이얼로그 호출 뼈대
}
