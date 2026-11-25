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
    Font(R.font.plus_jakarta_sans, FontWeight.Normal)
)

@Suppress("DEPRECATION")
private val BanglaPlatformStyle = PlatformTextStyle(
    includeFontPadding = false
)

private val BanglaLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both
)

private fun hisabTextStyle(
    fontWeight: FontWeight,
    fontSizeSp: Float,
    lineHeightSp: Float,
    letterSpacingSp: Float = 0.05f
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
    displayLarge = hisabTextStyle(FontWeight.Bold, 42f, 48f, -0.2f),
    displayMedium = hisabTextStyle(FontWeight.Bold, 36f, 42f, -0.1f),
    displaySmall = hisabTextStyle(FontWeight.Bold, 30f, 36f, 0f),
    headlineLarge = hisabTextStyle(FontWeight.Bold, 26f, 32f, 0f),
    headlineMedium = hisabTextStyle(FontWeight.Bold, 22f, 28f, 0f),
    headlineSmall = hisabTextStyle(FontWeight.Bold, 20f, 25f, 0f),
    titleLarge = hisabTextStyle(FontWeight.Bold, 19f, 24f, 0.05f),
    titleMedium = hisabTextStyle(FontWeight.SemiBold, 16.5f, 21f, 0.05f),
    titleSmall = hisabTextStyle(FontWeight.SemiBold, 14.5f, 18.5f, 0.05f),
    bodyLarge = hisabTextStyle(FontWeight.Normal, 15.5f, 20f, 0.1f),
    bodyMedium = hisabTextStyle(FontWeight.Normal, 14f, 18f, 0.1f),
    bodySmall = hisabTextStyle(FontWeight.Normal, 12.5f, 16f, 0.1f),
    labelLarge = hisabTextStyle(FontWeight.SemiBold, 13.5f, 17f, 0.08f),
    labelMedium = hisabTextStyle(FontWeight.Medium, 12.5f, 16f, 0.1f),
    labelSmall = hisabTextStyle(FontWeight.Medium, 11.5f, 14.5f, 0.1f)
)
