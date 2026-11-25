package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFE0B8FF),
    onPrimary = Color(0xFF2B0848),
    primaryContainer = BrandTint,
    onPrimaryContainer = Color(0xFFF5E8FF),
    secondary = Color(0xFFC084FC),
    onSecondary = Color(0xFF2B0848),
    secondaryContainer = Color(0xFF391C54),
    onSecondaryContainer = BrandDeep,
    tertiary = IncomeEmerald,
    onTertiary = Color(0xFF062C22),
    background = SilverBackground,
    onBackground = TextPrimary,
    surface = CardWhite,
    onSurface = TextPrimary,
    surfaceVariant = SilverSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = SilverBorder,
    error = ExpenseCrimson,
    onError = Color(0xFF450A14)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = BrandTint,
    onPrimaryContainer = Color(0xFFF5E8FF),
    secondary = Color(0xFFC084FC),
    onSecondary = DarkOnPrimary,
    secondaryContainer = Color(0xFF391C54),
    onSecondaryContainer = BrandDeep,
    tertiary = DarkIncomeText,
    onTertiary = Color(0xFF062C22),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = SilverBorder,
    error = DarkExpenseText,
    onError = Color(0xFF450A14)
)

@Composable
fun HisabKhataTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
