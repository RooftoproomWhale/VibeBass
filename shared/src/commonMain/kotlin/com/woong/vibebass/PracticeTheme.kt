package com.woong.vibebass

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object PracticeColors {
    val Desk = Color(0xFFF4F2EC)
    val Surface = Color(0xFFFCFBF8)
    val Paper = Color(0xFFFFFFFF)
    val Ink = Color(0xFF242923)
    val Muted = Color(0xFF656B61)
    val Divider = Color(0xFFDDDCD3)
    val Rust = Color(0xFFA33F24)
}

@Composable
internal fun PracticeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PracticeColors.Rust,
            onPrimary = PracticeColors.Paper,
            primaryContainer = PracticeColors.Desk,
            onPrimaryContainer = PracticeColors.Ink,
            secondary = PracticeColors.Ink,
            onSecondary = PracticeColors.Surface,
            secondaryContainer = PracticeColors.Divider,
            onSecondaryContainer = PracticeColors.Ink,
            background = PracticeColors.Desk,
            onBackground = PracticeColors.Ink,
            surface = PracticeColors.Surface,
            onSurface = PracticeColors.Ink,
            surfaceVariant = PracticeColors.Desk,
            onSurfaceVariant = PracticeColors.Muted,
            surfaceTint = PracticeColors.Rust,
            surfaceContainerLowest = PracticeColors.Paper,
            surfaceContainerLow = PracticeColors.Surface,
            surfaceContainer = PracticeColors.Desk,
            surfaceContainerHigh = PracticeColors.Desk,
            surfaceContainerHighest = PracticeColors.Divider,
            outline = PracticeColors.Muted,
            outlineVariant = PracticeColors.Divider,
            error = PracticeColors.Rust,
            onError = PracticeColors.Paper
        ),
        typography = Typography(
            headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
            headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
            titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
            titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 28.sp),
            bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 28.sp),
            bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 20.sp),
            labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(8.dp), large = RoundedCornerShape(8.dp),
            extraLarge = RoundedCornerShape(8.dp)
        ),
        content = content
    )
}
