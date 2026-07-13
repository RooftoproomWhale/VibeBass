package com.woong.vibebass.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PdfSheetViewer(
    pdfSource: String,
    scrollState: LazyListState,
    modifier: Modifier = Modifier
)
