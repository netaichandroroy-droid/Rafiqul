package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.example.R

val HisabBengaliFont = FontFamily(
    Font(R.font.hind_siliguri, FontWeight.Normal)
)

@Suppress("DEPRECATION")
private val BanglaPlatformStyle = PlatformTextStyle(
    includeFontPadding = true
)

private val BanglaLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private fun hisabTextStyle(
    fontWeight: FontWeight,
    fontSizeSp: Float,
    lineHeightSp: Float,
    letterSpacingSp: Float = 0.1f
): TextStyle = TextStyle(
    fontFamily = HisabBengaliFont,
    fontWeight = fontWeight,
    fontSize = fontSizeSp.sp,
    lineHeight = lineHeightSp.sp,
    letterSpacing = letterSpacingSp.sp,
    platformStyle = BanglaPlatformStyle,
    lineHeightStyle = BanglaLineHeightStyle
)

val Typography = Typography(
    displayLarge = hisabTextStyle(FontWeight.Bold, 44f, 54f, -0.2f),
    displayMedium = hisabTextStyle(FontWeight.Bold, 38f, 48f, -0.1f),
    displaySmall = hisabTextStyle(FontWeight.Bold, 32f, 42f, 0f),
    headlineLarge = hisabTextStyle(FontWeight.Bold, 28f, 38f, 0f),
    headlineMedium = hisabTextStyle(FontWeight.Bold, 24f, 34f, 0f),
    headlineSmall = hisabTextStyle(FontWeight.Bold, 22f, 31f, 0f),
    titleLarge = hisabTextStyle(FontWeight.Bold, 20f, 28f, 0.05f),
    titleMedium = hisabTextStyle(FontWeight.SemiBold, 17f, 25f, 0.1f),
    titleSmall = hisabTextStyle(FontWeight.SemiBold, 15f, 22f, 0.1f),
    bodyLarge = hisabTextStyle(FontWeight.Normal, 16f, 24f, 0.15f),
    bodyMedium = hisabTextStyle(FontWeight.Normal, 14f, 21f, 0.15f),
    bodySmall = hisabTextStyle(FontWeight.Normal, 13f, 19f, 0.15f),
    labelLarge = hisabTextStyle(FontWeight.SemiBold, 14f, 20f, 0.1f),
    labelMedium = hisabTextStyle(FontWeight.Medium, 13f, 18f, 0.15f),
    labelSmall = hisabTextStyle(FontWeight.Medium, 12f, 17f, 0.15f)
)
