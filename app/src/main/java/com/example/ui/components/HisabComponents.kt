package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.model.BanglaFormatters
import com.example.model.HisabTransaction
import com.example.model.TxType
import com.example.ui.theme.BrandDeep
import com.example.ui.theme.BrandHeroEnd
import com.example.ui.theme.BrandHeroStart
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandTint
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface
import kotlinx.coroutines.delay

@Composable
fun rememberLiveTimestampMs(): Long {
    val lifecycleOwner = LocalLifecycleOwner.current
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                nowMs = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            nowMs = System.currentTimeMillis()
        }
    }

    return nowMs
}

@Composable
fun rememberLiveTodayIso(): String {
    val liveNowMs = rememberLiveTimestampMs()
    return remember(liveNowMs / 60_000L) { BanglaFormatters.todayIso() }
}

@Composable
fun rememberLiveCurrentYearMonth(): String {
    return rememberLiveTodayIso().substring(0, 7)
}

@Composable
fun AppFrostedGlassBackground(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Deep charcoal-plum base matching the top section of the reference image (no bottom blue)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF140D22),
                    Color(0xFF1A1525),
                    Color(0xFF13101B)
                )
            )
        )

        // Ambient plum-violet glow behind the top-right silk wave
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF8E24AA).copy(alpha = 0.56f),
                    Color(0xFF4A148C).copy(alpha = 0.24f),
                    Color.Transparent
                ),
                center = Offset(w * 0.85f, h * 0.08f),
                radius = w * 0.92f
            ),
            radius = w * 0.92f,
            center = Offset(w * 0.85f, h * 0.08f)
        )

        // Top-right silky purple-magenta sweep curve (matching top half of reference image)
        val upperSilk = Path().apply {
            moveTo(w * 0.30f, -h * 0.04f)
            cubicTo(
                w * 0.58f, h * 0.06f,
                w * 0.88f, h * 0.16f,
                w * 1.12f, h * 0.26f
            )
            lineTo(w * 1.12f, -h * 0.05f)
            close()
        }
        drawPath(
            path = upperSilk,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF3E0F66).copy(alpha = 0.90f),
                    Color(0xFF7B1FA2).copy(alpha = 0.92f),
                    Color(0xFFAD2EC9).copy(alpha = 0.84f)
                ),
                start = Offset(w * 0.35f, 0f),
                end = Offset(w, h * 0.20f)
            )
        )

        // Secondary flowing plum-violet fold across the upper-middle canvas
        val midPlumFold = Path().apply {
            moveTo(-w * 0.08f, h * 0.16f)
            cubicTo(
                w * 0.36f, h * 0.10f,
                w * 0.72f, h * 0.28f,
                w * 1.08f, h * 0.42f
            )
            lineTo(w * 1.08f, h * 0.22f)
            cubicTo(
                w * 0.72f, h * 0.10f,
                w * 0.38f, -h * 0.02f,
                -w * 0.08f, h * 0.06f
            )
            close()
        }
        drawPath(
            path = midPlumFold,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF280D3E).copy(alpha = 0.50f),
                    Color(0xFF6A1B9A).copy(alpha = 0.46f),
                    Color(0xFF230E35).copy(alpha = 0.72f)
                ),
                start = Offset(0f, h * 0.10f),
                end = Offset(w, h * 0.36f)
            )
        )

        // Crisp luminous orchid specular highlight along the top-right silk ridge
        val ridgeHighlight = Path().apply {
            moveTo(w * 0.34f, -h * 0.02f)
            cubicTo(
                w * 0.60f, h * 0.08f,
                w * 0.86f, h * 0.17f,
                w * 1.08f, h * 0.27f
            )
        }
        drawPath(
            path = ridgeHighlight,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFD05CE3).copy(alpha = 0.35f),
                    Color(0xFFE996FF).copy(alpha = 0.72f),
                    Color(0xFF9C27B0).copy(alpha = 0.25f)
                )
            ),
            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
        )

        // Subtle warm violet ambient glow on lower-left so glass cards refract cleanly
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF5A189A).copy(alpha = 0.26f),
                    Color.Transparent
                ),
                center = Offset(w * 0.16f, h * 0.78f),
                radius = w * 0.75f
            ),
            radius = w * 0.75f,
            center = Offset(w * 0.16f, h * 0.78f)
        )
    }
}

@Composable
fun GlassCardSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    horizontalOuterPadding: Dp = 12.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalOuterPadding),
        shape = shape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.36f),
                    Color.White.copy(alpha = 0.12f)
                )
            )
        ),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            BrandHeroStart.copy(alpha = 0.14f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun HeroBalanceCard(
    balance: Double,
    totalIncome: Double,
    totalExpense: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .testTag("hero_balance_card"),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        contentColor = Color.White,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.48f),
                    Color.White.copy(alpha = 0.16f)
                )
            )
        ),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            BrandHeroStart.copy(alpha = 0.68f),
                            BrandHeroEnd.copy(alpha = 0.64f)
                        )
                    )
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.total_balance),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.86f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = BanglaFormatters.formatBdt(balance),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroStatBox(
                        label = stringResource(R.string.total_income),
                        value = BanglaFormatters.formatBdt(totalIncome),
                        modifier = Modifier.weight(1f)
                    )
                    HeroStatBox(
                        label = stringResource(R.string.total_expense),
                        value = BanglaFormatters.formatBdt(totalExpense),
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.16f),
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.34f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.86f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
    val badgeBg = if (isIncome) IncomeSurface else ExpenseSurface
    val semanticColor = if (isIncome) IncomeEmerald else ExpenseCrimson

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .testTag("transaction_row_${transaction.id}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeBg)
                    .border(0.8.dp, semanticColor.copy(alpha = 0.28f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) {
                        Icons.AutoMirrored.Filled.CallReceived
                    } else {
                        Icons.AutoMirrored.Filled.CallMade
                    },
                    contentDescription = transaction.resolvedType.bnLabel,
                    tint = semanticColor,
                    modifier = Modifier.size(18.dp)
                )
            }
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
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
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
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCardSurface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        horizontalOuterPadding = 12.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        content = content
    )
}

@Composable
fun DeleteTransactionDialog(
    transaction: HisabTransaction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = barShape,
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.34f),
                    Color.White.copy(alpha = 0.10f)
                )
            )
        ),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF211633).copy(alpha = 0.84f),
                            Color(0xFF140E21).copy(alpha = 0.90f)
                        )
                    )
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                            BrandHeroStart.copy(alpha = 0.16f)
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 8.dp)
                .height(58.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val pillModifier = if (item.selected) {
                    Modifier
                        .height(44.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    BrandHeroStart.copy(alpha = 0.28f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.58f),
                                    Color.White.copy(alpha = 0.18f)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                } else {
                    Modifier
                        .height(44.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
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
                            tint = if (item.selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            fontSize = 11.5.sp,
                            lineHeight = 14.sp,
                            fontWeight = if (item.selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (item.selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
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

@Composable
fun GlassTopBarContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val barShape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = barShape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.34f),
                    Color.White.copy(alpha = 0.12f)
                )
            )
        ),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF231338).copy(alpha = 0.72f),
                            Color(0xFF171024).copy(alpha = 0.62f)
                        )
                    )
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent,
                            BrandHeroStart.copy(alpha = 0.16f)
                        )
                    )
                )
        ) {
            content()
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
        val isSuccess = msg.contains("success", ignoreCase = true) ||
            msg.contains("approved", ignoreCase = true) ||
            msg.contains("restored", ignoreCase = true) ||
            msg.contains("connected", ignoreCase = true) ||
            msg.contains("backed up", ignoreCase = true) ||
            msg.contains("copied", ignoreCase = true)
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
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

