package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.example.R

val HisabBengaliFont = FontFamily(
    Font(R.font.manrope, FontWeight.Normal)
)

val DisplayGlassFont = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal)
)

@Suppress("DEPRECATION")
private val BanglaPlatformStyle = PlatformTextStyle(
    includeFontPadding = false
)

private val BanglaLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both
)

private val RaisedHeadingShadow = Shadow(
    color = Color(0xE0020617),
    offset = Offset(0f, 2.5f),
    blurRadius = 3.5f
)

private val RaisedBodyShadow = Shadow(
    color = Color(0xCC020617),
    offset = Offset(0f, 1.6f),
    blurRadius = 2.4f
)

private fun hisabTextStyle(
    fontWeight: FontWeight,
    fontSizeSp: Float,
    lineHeightSp: Float,
    letterSpacingSp: Float = 0.05f,
    fontFamily: FontFamily = HisabBengaliFont,
    shadow: Shadow? = null
): TextStyle = TextStyle(
    fontFamily = fontFamily,
    fontWeight = fontWeight,
    fontSize = fontSizeSp.sp,
    lineHeight = lineHeightSp.sp,
    letterSpacing = letterSpacingSp.sp,
    shadow = shadow,
    platformStyle = BanglaPlatformStyle,
    lineHeightStyle = BanglaLineHeightStyle
)

val Typography = Typography(
    displayLarge = hisabTextStyle(FontWeight.Bold, 42f, 48f, -0.25f, DisplayGlassFont, RaisedHeadingShadow),
    displayMedium = hisabTextStyle(FontWeight.Bold, 36f, 42f, -0.15f, DisplayGlassFont, RaisedHeadingShadow),
    displaySmall = hisabTextStyle(FontWeight.Bold, 30f, 36f, -0.05f, DisplayGlassFont, RaisedHeadingShadow),
    headlineLarge = hisabTextStyle(FontWeight.Bold, 26f, 32f, 0f, DisplayGlassFont, RaisedHeadingShadow),
    headlineMedium = hisabTextStyle(FontWeight.Bold, 22f, 28f, 0f, DisplayGlassFont, RaisedHeadingShadow),
    headlineSmall = hisabTextStyle(FontWeight.Bold, 20f, 25f, 0f, DisplayGlassFont, RaisedHeadingShadow),
    titleLarge = hisabTextStyle(FontWeight.Bold, 19f, 24f, 0.05f, DisplayGlassFont, RaisedHeadingShadow),
    titleMedium = hisabTextStyle(FontWeight.SemiBold, 16.5f, 21f, 0.05f, DisplayGlassFont, RaisedHeadingShadow),
    titleSmall = hisabTextStyle(FontWeight.SemiBold, 14.5f, 18.5f, 0.05f, DisplayGlassFont, RaisedBodyShadow),
    bodyLarge = hisabTextStyle(FontWeight.Normal, 15.5f, 20f, 0.1f, HisabBengaliFont, RaisedBodyShadow),
    bodyMedium = hisabTextStyle(FontWeight.Normal, 14f, 18f, 0.1f, HisabBengaliFont, RaisedBodyShadow),
    bodySmall = hisabTextStyle(FontWeight.Normal, 12.5f, 16f, 0.1f, HisabBengaliFont, RaisedBodyShadow),
    labelLarge = hisabTextStyle(FontWeight.SemiBold, 13.5f, 17f, 0.08f, HisabBengaliFont, null),
    labelMedium = hisabTextStyle(FontWeight.Medium, 12.5f, 16f, 0.1f, HisabBengaliFont, RaisedBodyShadow),
    labelSmall = hisabTextStyle(FontWeight.Medium, 11.5f, 14.5f, 0.1f, HisabBengaliFont, null)
)
