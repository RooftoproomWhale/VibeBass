package com.woong.vibebass

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

import vibebass.webapp.generated.resources.NotoSansKR
import vibebass.webapp.generated.resources.Res

@OptIn(ExperimentalResourceApi::class)
@Composable
internal inline fun WithFontResourcesLoaded(
    content: @Composable () -> Unit
) {
    val font by preloadFont(Res.font.NotoSansKR)

    var fontFallbackInitialized by remember { mutableStateOf(false) }
    val fontFamilyResolver = LocalFontFamilyResolver.current

    LaunchedEffect(fontFamilyResolver, font) {
        font?.let { font ->
            fontFamilyResolver.preload(FontFamily(font))
            fontFallbackInitialized = true
        }
    }

    if (fontFallbackInitialized) {
        content()
    }
}