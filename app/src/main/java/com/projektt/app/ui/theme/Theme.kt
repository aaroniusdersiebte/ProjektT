package com.projektt.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.projektt.app.R

// Colors
data class ProjektTColors(
    val background: Color = Color(0xFF0D0D0D),
    val surface: Color = Color(0xFF1A1A1A),
    val surfaceVariant: Color = Color(0xFF252525),
    val primary: Color = Color(0xFFFFB300),
    val primaryDim: Color = Color(0xFF996B00),
    val onBackground: Color = Color(0xFFE0E0E0),
    val onBackgroundDim: Color = Color(0xFF808080),
    val success: Color = Color(0xFF33FF57),
    val warning: Color = Color(0xFFFFAA33)
)

// Typography
val MonospaceFont = FontFamily.Monospace

data class ProjektTTypography(
    val displayLarge: TextStyle = TextStyle(
        fontFamily = MonospaceFont,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        letterSpacing = 4.sp
    ),
    val displayMedium: TextStyle = TextStyle(
        fontFamily = MonospaceFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = 2.sp
    ),
    val titleLarge: TextStyle = TextStyle(
        fontFamily = MonospaceFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 1.sp
    ),
    val titleMedium: TextStyle = TextStyle(
        fontFamily = MonospaceFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontFamily = MonospaceFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    val bodyMedium: TextStyle = TextStyle(
        fontFamily = MonospaceFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    val labelSmall: TextStyle = TextStyle(
        fontFamily = MonospaceFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 1.sp
    )
)

val LocalProjektTColors = staticCompositionLocalOf { ProjektTColors() }
val LocalProjektTTypography = staticCompositionLocalOf { ProjektTTypography() }

object ProjektTTheme {
    val colors: ProjektTColors
        @Composable get() = LocalProjektTColors.current
    val typography: ProjektTTypography
        @Composable get() = LocalProjektTTypography.current
}

@Composable
fun ProjektTTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalProjektTColors provides ProjektTColors(),
        LocalProjektTTypography provides ProjektTTypography(),
        content = content
    )
}
