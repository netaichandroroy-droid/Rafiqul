package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CloudSyncUiState
import com.example.model.BanglaFormatters
import com.example.model.DepositsSalesNetSummary
import com.example.model.HisabTransaction
import com.example.model.TxType
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.CardBorderSolid
import com.example.ui.theme.CardInnerTileSolid
import com.example.ui.theme.CardSurfaceSolid
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.GlassTextMuted
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface
import com.example.ui.theme.MetallicSilverRim
import com.example.ui.theme.MetallicSlateBottom
import com.example.ui.theme.MetallicSlateMid
import com.example.ui.theme.MetallicSlateTop
import com.example.ui.theme.NagadAccent
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonPurple

@Composable
fun AppFrostedGlassBackground(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Deep midnight navy-black base matching the glossy dashboard canvas
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF030712),
                    Color(0xFF061022),
                    Color(0xFF040A17),
                    Color(0xFF020610)
                )
            )
        )

        // Top ambient electric-blue glow beneath the header bar
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonCyan.copy(alpha = 0.20f),
                    NeonBlue.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = Offset(w * 0.55f, h * 0.06f),
                radius = w * 0.95f
            ),
            radius = w * 0.95f,
            center = Offset(w * 0.55f, h * 0.06f)
        )

        // Mid-left emerald-cyan ambient glow behind cards
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonEmerald.copy(alpha = 0.11f),
                    NeonCyan.copy(alpha = 0.07f),
                    Color.Transparent
                ),
                center = Offset(w * 0.18f, h * 0.42f),
                radius = w * 0.78f
            ),
            radius = w * 0.78f,
            center = Offset(w * 0.18f, h * 0.42f)
        )

        // Lower-right violet-blue ambient glow near bottom navbar
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonPurple.copy(alpha = 0.14f),
                    NeonBlue.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = Offset(w * 0.84f, h * 0.84f),
                radius = w * 0.80f
            ),
            radius = w * 0.80f,
            center = Offset(w * 0.84f, h * 0.84f)
        )
    }
}

fun Modifier.glossyMetallicSurface(
    shape: Shape,
    accentColor: Color = NeonCyan,
    secondaryGlow: Color = accentColor,
    borderWidth: Dp = 1.5.dp,
    streakIntensity: Float = 1f,
    isHero: Boolean = false
): Modifier = this
    .shadow(
        elevation = if (isHero) 16.dp else 12.dp,
        shape = shape,
        clip = false,
        ambientColor = accentColor.copy(alpha = 0.52f),
        spotColor = Color.Black.copy(alpha = 0.96f)
    )
    .clip(shape)
    .drawWithCache {
        val w = size.width
        val h = size.height
        val baseBrush = Brush.linearGradient(
            colors = if (isHero) {
                listOf(
                    Color(0xFF1E3254),
                    Color(0xFF101D33),
                    Color(0xFF070F1E)
                )
            } else {
                listOf(
                    MetallicSlateTop,
                    MetallicSlateMid,
                    MetallicSlateBottom
                )
            },
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )

        val leftCornerWash = Brush.radialGradient(
            colors = listOf(
                accentColor.copy(alpha = if (isHero) 0.24f else 0.17f),
                Color.Transparent
            ),
            center = Offset(w * 0.14f, h * 0.85f),
            radius = maxOf(w, h) * 0.62f
        )

        val rightCornerWash = Brush.radialGradient(
            colors = listOf(
                secondaryGlow.copy(alpha = if (isHero) 0.18f else 0.13f),
                Color.Transparent
            ),
            center = Offset(w * 0.88f, h * 0.16f),
            radius = maxOf(w, h) * 0.56f
        )

        val streakPrimary = Path().apply {
            moveTo(w * 0.04f, 0f)
            lineTo(w * 0.24f, 0f)
            lineTo(w * 0.44f, h)
            lineTo(w * 0.22f, h)
            close()
        }
        val streakPrimaryBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.04f * streakIntensity),
                Color.White.copy(alpha = 0.12f * streakIntensity),
                Color.White.copy(alpha = 0.02f * streakIntensity)
            ),
            start = Offset(w * 0.08f, 0f),
            end = Offset(w * 0.36f, h)
        )

        val streakSecondary = Path().apply {
            moveTo(w * 0.76f, 0f)
            lineTo(w * 0.94f, 0f)
            lineTo(w * 0.68f, h)
            lineTo(w * 0.54f, h)
            close()
        }
        val streakSecondaryBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFBAE6FD).copy(alpha = 0.14f * streakIntensity),
                Color.White.copy(alpha = 0.06f * streakIntensity),
                Color.Transparent
            ),
            start = Offset(w * 0.86f, 0f),
            end = Offset(w * 0.58f, h)
        )

        val topSheenBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f * streakIntensity),
                Color(0xFFBAE6FD).copy(alpha = 0.06f * streakIntensity),
                Color.Transparent
            ),
            startY = 0f,
            endY = (h * 0.28f).coerceAtLeast(1f)
        )

        val topBevelLineBrush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                MetallicSilverRim.copy(alpha = 0.68f),
                Color.White.copy(alpha = 0.82f),
                MetallicSilverRim.copy(alpha = 0.56f),
                Color.Transparent
            )
        )

        val bottomExtrusionShadowBrush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF02060F).copy(alpha = 0.78f),
                Color(0xFF01040B).copy(alpha = 0.94f)
            ),
            startY = (h - 14.dp.toPx()).coerceAtLeast(0f),
            endY = h
        )

        val bottomGlowBrush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                accentColor.copy(alpha = 0.88f),
                MetallicSilverRim.copy(alpha = 0.76f),
                secondaryGlow.copy(alpha = 0.86f),
                Color.Transparent
            )
        )

        onDrawBehind {
            drawRect(brush = baseBrush)
            drawRect(brush = leftCornerWash)
            drawRect(brush = rightCornerWash)
            drawPath(path = streakPrimary, brush = streakPrimaryBrush)
            drawPath(path = streakSecondary, brush = streakSecondaryBrush)
            drawRect(brush = topSheenBrush)
            drawRect(brush = bottomExtrusionShadowBrush)
            val topBevelY = 1.6.dp.toPx()
            drawLine(
                brush = topBevelLineBrush,
                start = Offset(w * 0.05f, topBevelY),
                end = Offset(w * 0.95f, topBevelY),
                strokeWidth = 1.4.dp.toPx(),
                cap = StrokeCap.Round
            )
            val rimThickness = 2.6.dp.toPx()
            drawLine(
                brush = bottomGlowBrush,
                start = Offset(w * 0.07f, h - rimThickness * 0.55f),
                end = Offset(w * 0.93f, h - rimThickness * 0.55f),
                strokeWidth = rimThickness,
                cap = StrokeCap.Round
            )
        }
    }
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(
                MetallicSilverRim.copy(alpha = 0.94f),
                accentColor.copy(alpha = 0.72f),
                MetallicSilverRim.copy(alpha = 0.44f),
                secondaryGlow.copy(alpha = 0.98f)
            )
        ),
        shape = shape
    )

@Composable
fun GlossyNeonIconOrb(
    icon: ImageVector,
    contentDescription: String? = null,
    glowColor: Color = NeonCyan,
    iconTint: Color = Color.White,
    size: Dp = 42.dp,
    iconSize: Dp = 21.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .drawWithCache {
                val r = this.size.minDimension / 2f
                val bgBrush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.62f),
                        glowColor.copy(alpha = 0.30f),
                        Color(0xFF0A1426)
                    ),
                    center = Offset(r * 0.85f, r * 0.78f),
                    radius = r * 1.15f
                )
                val highlightBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.34f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(this.size.width * 0.65f, this.size.height * 0.65f)
                )
                onDrawBehind {
                    drawCircle(brush = bgBrush)
                    drawCircle(brush = highlightBrush)
                }
            }
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.92f),
                        glowColor.copy(alpha = 0.92f),
                        Color.White.copy(alpha = 0.68f)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun NetaiBrandLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    glowColor: Color = NeonCyan,
    contentDescription: String? = "NETAI Logo"
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(
                width = 1.6.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MetallicSilverRim.copy(alpha = 0.94f),
                        glowColor,
                        NeonEmerald.copy(alpha = 0.92f)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.netai_app_logo),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun GlossyInnerMetricTile(
    modifier: Modifier = Modifier,
    accentColor: Color = NeonCyan,
    shape: Shape = RoundedCornerShape(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 7.dp,
                shape = shape,
                clip = false,
                ambientColor = accentColor.copy(alpha = 0.42f),
                spotColor = Color.Black.copy(alpha = 0.92f)
            )
            .clip(shape)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val bgBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF15243D),
                        CardInnerTileSolid,
                        Color(0xFF060C18)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
                val glowWash = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.2f, h * 0.9f),
                    radius = maxOf(w, h) * 0.72f
                )
                val topSheen = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = (h * 0.32f).coerceAtLeast(1f)
                )
                val bottomDepth = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF02060E).copy(alpha = 0.88f)
                    ),
                    startY = (h - 9.dp.toPx()).coerceAtLeast(0f),
                    endY = h
                )
                val topBevelBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MetallicSilverRim.copy(alpha = 0.48f),
                        Color.White.copy(alpha = 0.56f),
                        MetallicSilverRim.copy(alpha = 0.36f),
                        Color.Transparent
                    )
                )
                onDrawBehind {
                    drawRect(brush = bgBrush)
                    drawRect(brush = glowWash)
                    drawRect(brush = topSheen)
                    drawRect(brush = bottomDepth)
                    val topY = 1.2.dp.toPx()
                    drawLine(
                        brush = topBevelBrush,
                        start = Offset(w * 0.06f, topY),
                        end = Offset(w * 0.94f, topY),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MetallicSilverRim.copy(alpha = 0.72f),
                        accentColor.copy(alpha = 0.84f),
                        Color(0xFF071122).copy(alpha = 0.90f)
                    )
                ),
                shape = shape
            )
    ) {
        Column(content = content)
    }
}

@Composable
fun CleanHomeMetricTile(
    modifier: Modifier = Modifier,
    accentColor: Color = NeonCyan,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(content = content)
    }
}

@Composable
fun UnifiedPageCardSurface(
    modifier: Modifier = Modifier,
    accentColor: Color = NeonCyan,
    secondaryGlow: Color = NeonBlue,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .glossyMetallicSurface(
                shape = RoundedCornerShape(0.dp),
                accentColor = accentColor,
                secondaryGlow = secondaryGlow,
                borderWidth = 1.dp,
                streakIntensity = 0.25f,
                isHero = true
            )
    ) {
        CompositionLocalProvider(LocalContentColor provides GlassTextPrimary) {
            content()
        }
    }
}

@Composable
fun UnifiedPageSectionDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        thickness = 0.75.dp,
        color = CardBorderSolid.copy(alpha = 0.35f),
        modifier = modifier.padding(horizontal = 14.dp)
    )
}

@Composable
fun GlassCardSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    horizontalOuterPadding: Dp = 10.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    accentColor: Color = NeonCyan,
    secondaryGlow: Color = accentColor,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalOuterPadding)
            .glossyMetallicSurface(
                shape = shape,
                accentColor = accentColor,
                secondaryGlow = secondaryGlow
            )
    ) {
        CompositionLocalProvider(LocalContentColor provides GlassTextPrimary) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                content = content
            )
        }
    }
}

@Composable
fun DepositsSalesNetBalanceCard(
    summary: DepositsSalesNetSummary,
    cloudSyncState: CloudSyncUiState = CloudSyncUiState(),
    onConnectFirestore: (() -> Unit)? = null,
    embeddedInOuterCard: Boolean = false,
    modifier: Modifier = Modifier
) {
    val netBalance = summary.netBalance
    val netFormatted = if (netBalance > 0.0) {
        "+${BanglaFormatters.formatBdt(netBalance)}"
    } else {
        BanglaFormatters.formatBdt(netBalance)
    }
    val netColor = if (netBalance >= 0.0) IncomeEmerald else ExpenseCrimson
    val positionLabel = when {
        netBalance > 0.0 -> "Advance"
        netBalance < 0.0 -> "Due Balance"
        else -> "Settled"
    }
    val positionBg = when {
        netBalance > 0.0 -> IncomeSurface
        netBalance < 0.0 -> ExpenseSurface
        else -> Color.White.copy(alpha = 0.14f)
    }

    val (syncLabel, syncColor, syncIcon) = when {
        cloudSyncState.isConnected && cloudSyncState.isSyncing ->
            Triple("Syncing...", BrandPrimary, Icons.Outlined.Sync)
        cloudSyncState.isConnected && !cloudSyncState.errorMessage.isNullOrBlank() ->
            Triple("Sync Error", ExpenseCrimson, Icons.Outlined.CloudOff)
        cloudSyncState.isConnected && cloudSyncState.autoSyncEnabled ->
            Triple("Cloud Sync Live", IncomeEmerald, Icons.Outlined.CloudDone)
        cloudSyncState.isConnected ->
            Triple("Sync Paused", NagadAccent, Icons.Outlined.CloudOff)
        else ->
            Triple(
                if (onConnectFirestore != null) "Connect Cloud" else "Local Cache",
                GlassTextSecondary,
                Icons.Outlined.CloudOff
            )
    }

    val cardShape = RoundedCornerShape(20.dp)
    val outerModifier = if (embeddedInOuterCard) {
        modifier
            .fillMaxWidth()
            .testTag("firestore_net_balance_card")
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .glossyMetallicSurface(
                shape = cardShape,
                accentColor = NeonCyan,
                secondaryGlow = if (netBalance >= 0.0) NeonEmerald else ExpenseCrimson,
                borderWidth = 1.6.dp,
                isHero = true
            )
            .testTag("firestore_net_balance_card")
    }
    Box(modifier = outerModifier) {
        CompositionLocalProvider(LocalContentColor provides GlassTextPrimary) {
            val hPad = if (embeddedInOuterCard) 14.dp else 12.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = hPad,
                        vertical = if (embeddedInOuterCard) 2.dp else 9.dp
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Net Balance",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val connectAction = onConnectFirestore?.takeIf { !cloudSyncState.isConnected }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (embeddedInOuterCard) Color.White.copy(alpha = 0.06f) else CardInnerTileSolid.copy(alpha = 0.92f),
                        border = BorderStroke(
                            if (embeddedInOuterCard) 0.8.dp else 1.dp,
                            syncColor.copy(alpha = if (embeddedInOuterCard) 0.45f else 0.65f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .let { base ->
                                if (connectAction != null) base.clickable(onClick = connectAction) else base
                            }
                            .testTag("firestore_net_balance_status")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = syncIcon,
                                contentDescription = syncLabel,
                                tint = syncColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = syncLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = syncColor,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = netFormatted,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = netColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("firestore_net_balance_amount")
                    )
                    if (!embeddedInOuterCard) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = positionBg,
                            border = BorderStroke(0.8.dp, netColor.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = positionLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (netBalance == 0.0) GlassTextPrimary else netColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val depositTileContent: @Composable ColumnScope.() -> Unit = {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(
                                text = "Total Deposits",
                                style = MaterialTheme.typography.labelSmall,
                                color = GlassTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = BanglaFormatters.formatBdt(summary.totalDeposits),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = IncomeEmerald,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("firestore_total_deposits_value")
                            )
                        }
                    }
                    val salesTileContent: @Composable ColumnScope.() -> Unit = {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(
                                text = "Total Sales (TP+VAT)",
                                style = MaterialTheme.typography.labelSmall,
                                color = GlassTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = BanglaFormatters.formatBdt(summary.totalSales),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GlassTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("firestore_total_sales_value")
                            )
                        }
                    }
                    if (embeddedInOuterCard) {
                        CleanHomeMetricTile(
                            modifier = Modifier.weight(1f),
                            accentColor = NeonEmerald,
                            shape = RoundedCornerShape(12.dp),
                            content = depositTileContent
                        )
                        CleanHomeMetricTile(
                            modifier = Modifier.weight(1f),
                            accentColor = NeonCyan,
                            shape = RoundedCornerShape(12.dp),
                            content = salesTileContent
                        )
                    } else {
                        GlossyInnerMetricTile(
                            modifier = Modifier.weight(1f),
                            accentColor = NeonEmerald,
                            shape = RoundedCornerShape(12.dp),
                            content = depositTileContent
                        )
                        GlossyInnerMetricTile(
                            modifier = Modifier.weight(1f),
                            accentColor = NeonCyan,
                            shape = RoundedCornerShape(12.dp),
                            content = salesTileContent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeroBalanceCard(
    balance: Double,
    totalIncome: Double,
    totalExpense: Double,
    embeddedInOuterCard: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(20.dp)
    val outerModifier = if (embeddedInOuterCard) {
        modifier
            .fillMaxWidth()
            .testTag("hero_balance_card")
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .glossyMetallicSurface(
                shape = cardShape,
                accentColor = NeonEmerald,
                secondaryGlow = NeonBlue,
                borderWidth = 1.6.dp,
                isHero = true
            )
            .testTag("hero_balance_card")
    }
    Box(modifier = outerModifier) {
        CompositionLocalProvider(LocalContentColor provides GlassTextPrimary) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (embeddedInOuterCard) 14.dp else 12.dp,
                        vertical = if (embeddedInOuterCard) 2.dp else 11.dp
                    )
            ) {
                Text(
                    text = stringResource(R.string.total_balance),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlassTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = BanglaFormatters.formatBdt(balance),
                    style = MaterialTheme.typography.displaySmall,
                    color = GlassTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroStatBox(
                        label = stringResource(R.string.total_income),
                        value = BanglaFormatters.formatBdt(totalIncome),
                        accentColor = NeonEmerald,
                        valueColor = IncomeEmerald,
                        cleanStyle = embeddedInOuterCard,
                        modifier = Modifier.weight(1f)
                    )
                    HeroStatBox(
                        label = stringResource(R.string.total_expense),
                        value = BanglaFormatters.formatBdt(totalExpense),
                        accentColor = NeonPurple,
                        valueColor = GlassTextPrimary,
                        cleanStyle = embeddedInOuterCard,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStatBox(
    label: String,
    value: String,
    accentColor: Color = NeonCyan,
    valueColor: Color = GlassTextPrimary,
    cleanStyle: Boolean = false,
    modifier: Modifier = Modifier
) {
    val tileContent: @Composable ColumnScope.() -> Unit = {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = GlassTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    if (cleanStyle) {
        CleanHomeMetricTile(
            modifier = modifier,
            accentColor = accentColor,
            shape = RoundedCornerShape(12.dp),
            content = tileContent
        )
    } else {
        GlossyInnerMetricTile(
            modifier = modifier,
            accentColor = accentColor,
            shape = RoundedCornerShape(12.dp),
            content = tileContent
        )
    }
}

@Composable
fun TransactionItemRow(
    transaction: HisabTransaction,
    onEdit: ((HisabTransaction) -> Unit)? = null,
    onDelete: ((HisabTransaction) -> Unit)? = null,
    showDivider: Boolean = true
) {
    val isIncome = transaction.resolvedType == TxType.INCOME
    val semanticColor = if (isIncome) IncomeEmerald else ExpenseCrimson

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .testTag("transaction_row_${transaction.id}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlossyNeonIconOrb(
                icon = if (isIncome) {
                    Icons.AutoMirrored.Filled.CallReceived
                } else {
                    Icons.AutoMirrored.Filled.CallMade
                },
                contentDescription = transaction.resolvedType.bnLabel,
                glowColor = semanticColor,
                iconTint = Color.White,
                size = 38.dp,
                iconSize = 18.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.displayTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = "${transaction.category} • ${BanglaFormatters.formatBanglaDate(transaction.date)} • ${transaction.resolvedAccount.bnShort}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = BanglaFormatters.formatSignedBdt(transaction.amount, transaction.resolvedType),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = semanticColor
            )
            if (onEdit != null) {
                IconButton(
                    onClick = { onEdit(transaction) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("edit_tx_${transaction.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (onDelete != null) {
                IconButton(
                    onClick = { onDelete(transaction) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_tx_${transaction.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete",
                        tint = semanticColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 0.8.dp,
                color = Color.White.copy(alpha = 0.14f)
            )
        }
    }
}

@Composable
fun EmptyTransactionsState(
    message: String = "No transactions yet. Add an income or expense entry above or below.",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlossyNeonIconOrb(
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            contentDescription = null,
            glowColor = NeonCyan,
            size = 56.dp,
            iconSize = 26.dp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SectionPanelCard(
    modifier: Modifier = Modifier,
    accentColor: Color = NeonCyan,
    secondaryGlow: Color = accentColor,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCardSurface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        horizontalOuterPadding = 10.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
        accentColor = accentColor,
        secondaryGlow = secondaryGlow,
        content = content
    )
}

@Composable
fun hisabFormFieldColors(
    accentColor: Color = NeonCyan
): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CardInnerTileSolid,
    unfocusedContainerColor = CardInnerTileSolid.copy(alpha = 0.94f),
    disabledContainerColor = CardInnerTileSolid.copy(alpha = 0.55f),
    focusedBorderColor = accentColor,
    unfocusedBorderColor = MetallicSilverRim.copy(alpha = 0.48f),
    disabledBorderColor = MetallicSilverRim.copy(alpha = 0.24f),
    errorBorderColor = ExpenseCrimson,
    focusedTextColor = GlassTextPrimary,
    unfocusedTextColor = GlassTextPrimary,
    disabledTextColor = GlassTextSecondary.copy(alpha = 0.72f),
    focusedLabelColor = accentColor,
    unfocusedLabelColor = GlassTextSecondary,
    disabledLabelColor = GlassTextMuted,
    focusedPlaceholderColor = GlassTextMuted,
    unfocusedPlaceholderColor = GlassTextMuted,
    focusedLeadingIconColor = accentColor,
    unfocusedLeadingIconColor = GlassTextSecondary,
    focusedTrailingIconColor = accentColor,
    unfocusedTrailingIconColor = GlassTextSecondary,
    cursorColor = accentColor
)

fun Modifier.hisab3DFieldDepth(
    accentColor: Color = NeonCyan,
    cornerRadius: Dp = 12.dp,
    topLabelOffset: Dp = 8.dp
): Modifier = this.drawWithContent {
    val topY = topLabelOffset.toPx().coerceAtMost(size.height * 0.25f)
    val boxHeight = (size.height - topY).coerceAtLeast(1f)
    val cr = cornerRadius.toPx()

    // 3D drop shadow & extruded lower lip directly beneath the OutlinedTextField box
    drawRoundRect(
        color = Color(0xFF01040B).copy(alpha = 0.82f),
        topLeft = Offset(1.dp.toPx(), topY + 3.5.dp.toPx()),
        size = Size((size.width - 2.dp.toPx()).coerceAtLeast(1f), boxHeight),
        cornerRadius = CornerRadius(cr, cr)
    )
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                accentColor.copy(alpha = 0.42f),
                MetallicSilverRim.copy(alpha = 0.32f),
                accentColor.copy(alpha = 0.42f),
                Color.Transparent
            )
        ),
        topLeft = Offset(2.dp.toPx(), topY + 2.dp.toPx()),
        size = Size((size.width - 4.dp.toPx()).coerceAtLeast(1f), boxHeight),
        cornerRadius = CornerRadius(cr, cr),
        style = Stroke(width = 1.4.dp.toPx())
    )

    drawContent()

    // Subtle upper metallic sheen inside the field box without touching the floating label
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                MetallicSilverRim.copy(alpha = 0.08f),
                Color.Transparent
            ),
            startY = topY,
            endY = topY + (boxHeight * 0.34f)
        ),
        topLeft = Offset(1.dp.toPx(), topY + 1.dp.toPx()),
        size = Size((size.width - 2.dp.toPx()).coerceAtLeast(1f), (boxHeight - 2.dp.toPx()).coerceAtLeast(1f)),
        cornerRadius = CornerRadius(cr, cr)
    )
}

fun Modifier.hisab3DButtonSurface(
    accentColor: Color = NeonCyan,
    cornerRadius: Dp = 14.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .shadow(
            elevation = 10.dp,
            shape = shape,
            clip = false,
            ambientColor = accentColor.copy(alpha = 0.62f),
            spotColor = Color.Black.copy(alpha = 0.96f)
        )
        .drawWithContent {
            val cr = cornerRadius.toPx()
            drawContent()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.24f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.36f)
                    )
                ),
                cornerRadius = CornerRadius(cr, cr)
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MetallicSilverRim.copy(alpha = 0.78f),
                        accentColor.copy(alpha = 0.68f),
                        Color.Black.copy(alpha = 0.45f)
                    )
                ),
                cornerRadius = CornerRadius(cr, cr),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
}

fun Modifier.hisab3DSegmentedShell(
    accentColor: Color = NeonCyan,
    cornerRadius: Dp = 20.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this.shadow(
        elevation = 6.dp,
        shape = shape,
        clip = false,
        ambientColor = accentColor.copy(alpha = 0.40f),
        spotColor = Color.Black.copy(alpha = 0.90f)
    )
}

@Composable
fun hisabFilterChipColors(
    accentColor: Color = NeonCyan
) = FilterChipDefaults.filterChipColors(
    containerColor = CardInnerTileSolid,
    labelColor = GlassTextSecondary,
    iconColor = GlassTextSecondary,
    selectedContainerColor = accentColor.copy(alpha = 0.26f),
    selectedLabelColor = Color.White,
    selectedLeadingIconColor = accentColor
)

@Composable
fun hisabFilterChipBorder(
    selected: Boolean,
    accentColor: Color = NeonCyan
) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = MetallicSilverRim.copy(alpha = 0.42f),
    selectedBorderColor = accentColor.copy(alpha = 0.94f),
    borderWidth = 1.1.dp,
    selectedBorderWidth = 1.4.dp
)

@Composable
fun GlossyFormSectionHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    glowColor: Color = NeonCyan,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlossyNeonIconOrb(
            icon = icon,
            contentDescription = title,
            glowColor = glowColor,
            size = 40.dp,
            iconSize = 20.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GlassTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun DeleteTransactionDialog(
    transaction: HisabTransaction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurfaceSolid,
        titleContentColor = GlassTextPrimary,
        textContentColor = GlassTextSecondary,
        title = { Text(stringResource(R.string.delete_confirm_title)) },
        text = {
            Text(
                "${transaction.displayTitle} (${BanglaFormatters.formatBdt(transaction.amount)}) — ${stringResource(R.string.delete_confirm_body)}"
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(stringResource(R.string.delete_action))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_button")
            ) {
                Text(stringResource(R.string.cancel_action))
            }
        }
    )
}

data class StylishNavTab(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val testTag: String,
    val onClick: () -> Unit,
    val popupContent: (@Composable () -> Unit)? = null
)

@Composable
fun StylishBottomNavBar(
    items: List<StylishNavTab>,
    dockedEdgeToEdge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val barShape = if (dockedEdgeToEdge) RoundedCornerShape(0.dp) else RoundedCornerShape(22.dp)
    val outerBoxModifier = if (dockedEdgeToEdge) {
        modifier.fillMaxWidth()
    } else {
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 4.dp)
    }
    Box(modifier = outerBoxModifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glossyMetallicSurface(
                    shape = barShape,
                    accentColor = NeonCyan,
                    secondaryGlow = NeonBlue,
                    borderWidth = if (dockedEdgeToEdge) 1.dp else 1.4.dp,
                    streakIntensity = 1.15f
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (dockedEdgeToEdge) it.navigationBarsPadding() else it }
                    .padding(horizontal = 6.dp, vertical = 5.dp)
                    .height(54.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val pillShape = RoundedCornerShape(15.dp)
                    val pillModifier = if (item.selected) {
                        Modifier
                            .height(48.dp)
                            .fillMaxWidth()
                            .clip(pillShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        NeonCyan.copy(alpha = 0.28f),
                                        NeonBlue.copy(alpha = 0.34f),
                                        Color(0xFF0B1C38)
                                    )
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MetallicSilverRim.copy(alpha = 0.88f),
                                        NeonCyan,
                                        NeonBlue.copy(alpha = 0.88f)
                                    )
                                ),
                                shape = pillShape
                            )
                    } else {
                        Modifier
                            .height(48.dp)
                            .fillMaxWidth()
                            .clip(pillShape)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 2.dp)
                            .clip(pillShape)
                            .clickable(onClick = item.onClick)
                            .testTag(item.testTag),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = pillModifier,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (item.selected) Color.White else GlassTextSecondary.copy(alpha = 0.78f),
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.label,
                                fontSize = 11.5.sp,
                                lineHeight = 14.sp,
                                fontWeight = if (item.selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (item.selected) Color.White else GlassTextSecondary.copy(alpha = 0.78f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        item.popupContent?.invoke()
                    }
                }
            }
        }
    }
}

@Composable
fun GlassTopBarContainer(
    modifier: Modifier = Modifier,
    dockedEdgeToEdge: Boolean = false,
    content: @Composable () -> Unit
) {
    val barShape = if (dockedEdgeToEdge) RoundedCornerShape(0.dp) else RoundedCornerShape(22.dp)
    val outerBoxModifier = if (dockedEdgeToEdge) {
        modifier.fillMaxWidth()
    } else {
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 4.dp)
    }
    Box(modifier = outerBoxModifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glossyMetallicSurface(
                    shape = barShape,
                    accentColor = NeonCyan,
                    secondaryGlow = NeonBlue,
                    borderWidth = if (dockedEdgeToEdge) 1.dp else 1.4.dp,
                    streakIntensity = 1.25f
                )
        ) {
            CompositionLocalProvider(LocalContentColor provides GlassTextPrimary) {
                Box(
                    modifier = if (dockedEdgeToEdge) {
                        Modifier.fillMaxWidth().statusBarsPadding()
                    } else {
                        Modifier.fillMaxWidth()
                    }
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun AppGlassSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        val msg = data.visuals.message
        val isSuccess = isSuccessFeedbackMessage(msg)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("app_snackbar_banner"),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1F1630).copy(alpha = 0.94f),
            border = BorderStroke(
                width = 1.dp,
                color = if (isSuccess) {
                    IncomeEmerald.copy(alpha = 0.65f)
                } else {
                    Color.White.copy(alpha = 0.30f)
                }
            ),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isSuccess) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Success",
                        tint = IncomeEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GlassTextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

fun isSuccessFeedbackMessage(message: String): Boolean {
    return message.contains("success", ignoreCase = true) ||
        message.contains("submitted", ignoreCase = true) ||
        message.contains("approved", ignoreCase = true) ||
        message.contains("restored", ignoreCase = true) ||
        message.contains("connected", ignoreCase = true) ||
        message.contains("backed up", ignoreCase = true) ||
        message.contains("copied", ignoreCase = true) ||
        message.contains("updated", ignoreCase = true) ||
        message.contains("saved", ignoreCase = true)
}

@Composable
fun AppDataSuccessBanner(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("app_success_banner"),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0E2B24),
            border = BorderStroke(1.5.dp, IncomeEmerald),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(IncomeEmerald.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Successful",
                        tint = IncomeEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Successful!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = IncomeEmerald
                    )
                    Text(
                        text = message.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = GlassTextPrimary
                    )
                }
                androidx.compose.material3.IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("app_success_banner_dismiss")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss success message",
                        tint = GlassTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun rememberLiveTodayIso(): String {
    return produceState(initialValue = BanglaFormatters.todayIso()) {
        while (true) {
            delay(30_000L)
            val latest = BanglaFormatters.todayIso()
            if (value != latest) {
                value = latest
            }
        }
    }.value
}
