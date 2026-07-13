package com.woong.vibebass.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PdfSheetViewer(
    pdfSource: String,
    scrollState: LazyListState,
    onPdfFileSelected: (String) -> Unit,
    modifier: Modifier = Modifier
)

/**
 * 모바일(Android/iOS) 및 웹(Web) 플랫폼 맞춤형 PDF 악보 업로드 다이얼로그를 트리거합니다.
 */
expect fun triggerPdfUpload()
