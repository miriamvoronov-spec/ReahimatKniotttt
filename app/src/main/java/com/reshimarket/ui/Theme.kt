package com.reshimarket.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.reshimarket.R

val Rubik = FontFamily(
    Font(R.font.rubik_regular, FontWeight.Normal),
    Font(R.font.rubik_medium, FontWeight.Medium),
    Font(R.font.rubik_bold, FontWeight.Bold),
    Font(R.font.rubik_extrabold, FontWeight.ExtraBold),
)

@Immutable
data class AppColors(
    val dark: Boolean,
    val bg: Color,
    val ink: Color,
    val mut: Color,
    val card: Color,
    val line: Color,
    val red: Color,
    val redBg: Color,
    val grn: Color,
    val grnBg: Color,
    val sun: Color = Color(0xFFFFB703),
    val brand: Color,
    val shelf: Color,
)

val LightColors = AppColors(
    dark = false,
    bg = Color(0xFFFFF8EE), ink = Color(0xFF2A1A10), mut = Color(0xFF7A6252), card = Color(0xFFFFFDFA),
    line = Color(0xFFEFDFCC), red = Color(0xFFD93A3F), redBg = Color(0xFFFFE4E1), grn = Color(0xFF178A4A),
    grnBg = Color(0xFFDCF4E3), brand = Color(0xFF8A4B17), shelf = Color(0xFFE7C79C),
)

val DarkColors = AppColors(
    dark = true,
    bg = Color(0xFF1C1620), ink = Color(0xFFF7EDE6), mut = Color(0xFFB9A7A0), card = Color(0xFF241C28),
    line = Color(0xFF3A2F3E), red = Color(0xFFFF7477), redBg = Color(0xFF3B1F25), grn = Color(0xFF55D68F),
    grnBg = Color(0xFF15301F), brand = Color(0xFFF2B46D), shelf = Color(0xFF4A3A2C),
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

@Composable
fun ReshimarketTheme(dark: Boolean, fontSize: Int, textColor: Int? = null, content: @Composable () -> Unit) {
    val baseColors = if (dark) DarkColors else LightColors
    // צבע כתב שנבחר בהגדרות מחליף את צבע הטקסט הראשי
    val c = if (textColor != null) baseColors.copy(ink = Color(textColor)) else baseColors
    val scheme = if (dark) darkColorScheme(
        primary = c.ink, onPrimary = c.bg, background = c.bg, onBackground = c.ink, surface = c.card,
        onSurface = c.ink, surfaceContainerLow = c.card, surfaceContainer = c.card, surfaceContainerHigh = c.card,
        onSurfaceVariant = c.mut, outline = c.line, error = c.red, secondary = c.grn, tertiary = c.sun,
    ) else lightColorScheme(
        primary = c.ink, onPrimary = c.bg, background = c.bg, onBackground = c.ink, surface = c.card,
        onSurface = c.ink, surfaceContainerLow = c.card, surfaceContainer = c.card, surfaceContainerHigh = c.card,
        onSurfaceVariant = c.mut, outline = c.line, error = c.red, secondary = c.grn, tertiary = c.sun,
    )
    val base = Typography()
    val t = Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Rubik),
        displayMedium = base.displayMedium.copy(fontFamily = Rubik),
        displaySmall = base.displaySmall.copy(fontFamily = Rubik),
        headlineLarge = base.headlineLarge.copy(fontFamily = Rubik),
        headlineMedium = base.headlineMedium.copy(fontFamily = Rubik),
        headlineSmall = base.headlineSmall.copy(fontFamily = Rubik),
        titleLarge = base.titleLarge.copy(fontFamily = Rubik),
        titleMedium = base.titleMedium.copy(fontFamily = Rubik),
        titleSmall = base.titleSmall.copy(fontFamily = Rubik),
        bodyLarge = TextStyle(fontFamily = Rubik, fontSize = 16.sp, lineHeight = 23.sp),
        bodyMedium = TextStyle(fontFamily = Rubik, fontSize = 15.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontFamily = Rubik, fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = base.labelLarge.copy(fontFamily = Rubik),
        labelMedium = base.labelMedium.copy(fontFamily = Rubik),
        labelSmall = base.labelSmall.copy(fontFamily = Rubik),
    )
    val density = LocalDensity.current
    // גודל הכתב מההגדרות (14–24) משנה את כל הטקסטים באפליקציה
    val scaled = Density(density.density, density.fontScale * fontSize / 16f)
    MaterialTheme(colorScheme = scheme, typography = t) {
        CompositionLocalProvider(
            LocalAppColors provides c,
            LocalDensity provides scaled,
            LocalLayoutDirection provides LayoutDirection.Rtl,
            content = content,
        )
    }
}
