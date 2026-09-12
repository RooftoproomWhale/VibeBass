package com.woong.vibebass.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PdfSheetViewer(
    pdfSource: String,
    onScrollPositionChanged: (Float) -> Unit,
    onPdfFileSelected: (String, String) -> Unit,
    modifier: Modifier = Modifier
)

expect fun triggerPdfUpload()
