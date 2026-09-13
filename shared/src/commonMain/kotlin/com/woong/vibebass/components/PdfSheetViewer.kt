package com.woong.vibebass.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woong.vibebass.sync.AnchorPoint

@Composable
expect fun PdfSheetViewer(
    pdfSource: String,
    onScrollPositionChanged: (Float, Float?) -> Unit,
    onPdfFileSelected: (String, String) -> Unit,
    scrollTarget: AnchorPoint? = null,
    onRecordAnchor: (() -> Unit)? = null,
    modifier: Modifier = Modifier
)

expect fun triggerPdfUpload()
