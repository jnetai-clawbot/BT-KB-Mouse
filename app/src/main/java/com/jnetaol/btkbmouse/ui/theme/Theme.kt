package com.jnetaol.btkbmouse.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val BTKBMouseDarkColorScheme = darkColorScheme(
    primary = NeonBlue,
    onPrimary = DarkBackground,
    primaryContainer = DeepIndigo,
    onPrimaryContainer = NeonBlue,
    secondary = NeonIndigo,
    onSecondary = TextPrimary,
    secondaryContainer = DeepIndigo,
    onSecondaryContainer = NeonBlue,
    tertiary = ElectricViolet,
    onTertiary = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = ErrorRed,
    onError = DarkBackground
)

@Composable
fun BTKBMouseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BTKBMouseDarkColorScheme,
        typography = Typography(
            headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = 0.sp),
            headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = 0.sp),
            headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, letterSpacing = 0.sp),
            titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
            bodyLarge = TextStyle(fontSize = 16.sp),
            bodyMedium = TextStyle(fontSize = 14.sp),
            bodySmall = TextStyle(fontSize = 12.sp),
            labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
            labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
            labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp)
        ),
        content = content
    )
}
