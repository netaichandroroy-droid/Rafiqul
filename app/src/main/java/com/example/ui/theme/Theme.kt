package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = BrandTint,
    onPrimaryContainer = GlassTextPrimary,
    secondary = Color(0xFFA5F3FC),
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF163A54),
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
    onPrimaryContainer = GlassTextPrimary,
    secondary = Color(0xFFA5F3FC),
    onSecondary = DarkOnPrimary,
    secondaryContainer = Color(0xFF163A54),
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
