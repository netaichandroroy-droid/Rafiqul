package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ai.AiCallQueueItem
import com.example.ai.FemaleAiVoiceOption
import com.example.ai.MondayAutoCallScheduler
import com.example.data.AiCallCenterPrefsStore
import com.example.data.AiCallStatus
import com.example.model.BanglaFormatters
import com.example.ui.AiCallCenterUiState
import com.example.ui.components.SectionPanelCard
import com.example.ui.theme.CardBorderSolid
import com.example.ui.theme.CardInnerTileSolid
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import com.example.ui.theme.HeroCardBorderSolid
import com.example.ui.theme.HeroCardSolid
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface
import com.example.ui.theme.NagadAccent
import com.example.ui.theme.NagadSurface

private val MONDAY_HOUR_SLOTS = listOf(9, 10, 11, 14, 16)

@Composable
fun AiCallCenterScreen(
    state: AiCallCenterUiState,
    onToggleMondayAutoCall: (Boolean) -> Unit,
    onSelectCallHour: (Int) -> Unit,
    onToggleAutoVoice: (Boolean) -> Unit,
    onToggleAlsoDialPhone: (Boolean) -> Unit,
    onSelectVoice: (FemaleAiVoiceOption) -> Unit,
    onStartMondayQueue: () -> Unit,
    onCallMember: (String) -> Unit,
    onDialMemberPhone: (String) -> Unit,
    onPlayMemberVoiceOnly: (String) -> Unit,
    onMarkCallStatus: (String, AiCallStatus) -> Unit,
    onResetQueue: () -> Unit,
    onStopVoice: () -> Unit,
    onClearError: () -> Unit,
    onConsumePendingDial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingCallAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        pendingCallAction?.invoke()
        pendingCallAction = null
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    fun launchWithCallPermissionIfNeeded(requireSimPermission: Boolean, action: () -> Unit) {
        if (!requireSimPermission) {
            action()
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            action()
        } else {
            pendingCallAction = action
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    LaunchedEffect(state.autoCallMondayEnabled, state.scheduledHour, state.scheduledMinute) {
        MondayAutoCallScheduler.syncSchedule(context)
    }

    LaunchedEffect(state.pendingDialNumber) {
        val number = state.pendingDialNumber ?: return@LaunchedEffect
        val hasDirectCall = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        val uri = Uri.parse("tel:$number")
        val placedDirect = if (hasDirectCall) {
            runCatching {
                val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)
                true
            }.getOrDefault(false)
        } else {
            false
        }
        if (!placedDirect) {
            runCatching {
                val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
            }
        }
        onConsumePendingDial()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_call_center_screen"),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (state.isCallActive) {
            item {
                LiveFreeAiCallCard(
                    state = state,
                    onStop = onStopVoice
                )
            }
        }

        item {
            SectionPanelCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_call_center_schedule_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HeroCardSolid,
                            border = BorderStroke(1.dp, HeroCardBorderSolid),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.PhoneInTalk,
                                    contentDescription = null,
                                    tint = IncomeEmerald,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Free AI Call Center",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GlassTextPrimary
                            )
                            Text(
                                text = "Female AI auto-calls Monday dues (Free)",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            state.isMondayToday && state.autoCallMondayEnabled -> IncomeSurface
                            state.autoCallMondayEnabled -> MaterialTheme.colorScheme.primaryContainer
                            else -> ExpenseSurface
                        }
                    ) {
                        Text(
                            text = when {
                                state.isMondayToday && state.autoCallMondayEnabled -> "Free • Active"
                                state.autoCallMondayEnabled -> "Free • Monday"
                                else -> "Paused"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                state.isMondayToday && state.autoCallMondayEnabled -> IncomeEmerald
                                state.autoCallMondayEnabled -> MaterialTheme.colorScheme.primary
                                else -> ExpenseCrimson
                            },
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardInnerTileSolid,
                    border = BorderStroke(1.dp, CardBorderSolid),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Call on Monday",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GlassTextPrimary
                                )
                                Text(
                                    text = "Next: ${state.nextScheduleLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GlassTextSecondary
                                )
                            }
                            Switch(
                                checked = state.autoCallMondayEnabled,
                                onCheckedChange = { enabled ->
                                    onToggleMondayAutoCall(enabled)
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val notifGranted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!notifGranted) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("ai_call_monday_switch")
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 0.8.dp,
                            color = CardBorderSolid.copy(alpha = 0.7f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Monday Call Time:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GlassTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MONDAY_HOUR_SLOTS.forEach { hour ->
                                val selected = state.scheduledHour == hour
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectCallHour(hour) },
                                    label = {
                                        Text(
                                            text = AiCallCenterPrefsStore.formatHourSlot(hour),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("ai_call_hour_${hour}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Female AI Speaker",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GlassTextPrimary
                                )
                                Text(
                                    text = "Female voice speaks automatically on every call",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GlassTextSecondary
                                )
                            }
                            Switch(
                                checked = state.autoVoiceGreeting,
                                onCheckedChange = onToggleAutoVoice,
                                modifier = Modifier.testTag("ai_call_auto_voice_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FemaleAiVoiceOption.entries.forEach { voice ->
                                val selected = state.selectedVoice == voice
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectVoice(voice) },
                                    label = {
                                        Text(
                                            text = voice.bnLabel,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("ai_call_voice_${voice.voiceId.lowercase()}")
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 0.8.dp,
                            color = CardBorderSolid.copy(alpha = 0.7f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Also Dial Phone SIM (Optional)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GlassTextPrimary
                                )
                                Text(
                                    text = "Keep OFF for 100% Free AI Voice Calls",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GlassTextSecondary
                                )
                            }
                            Switch(
                                checked = state.alsoDialPhoneNumber,
                                onCheckedChange = onToggleAlsoDialPhone,
                                modifier = Modifier.testTag("ai_call_sim_dial_switch")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            launchWithCallPermissionIfNeeded(state.alsoDialPhoneNumber, onStartMondayQueue)
                        },
                        enabled = state.dueQueue.isNotEmpty() && !state.isCallActive,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("ai_call_start_queue_button")
                    ) {
                        if (state.isCallActive) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (state.activeCallTotalCount > 1) {
                                    "Auto-Calling (${state.activeCallIndex}/${state.activeCallTotalCount})..."
                                } else {
                                    "Female AI Speaking..."
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.PhoneInTalk,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.pendingCount > 0) {
                                    "Start Free AI Auto-Call (${state.pendingCount})"
                                } else {
                                    "Re-Run Free AI Auto-Call"
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (state.isCallActive) {
                        OutlinedButton(
                            onClick = onStopVoice,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseCrimson),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("ai_call_stop_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Stop,
                                contentDescription = "Stop call",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (state.progressMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = IncomeSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.GraphicEq,
                                contentDescription = null,
                                tint = IncomeEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.progressMessage,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = IncomeEmerald
                            )
                        }
                    }
                }

                if (!state.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ExpenseSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_call_error_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = ExpenseCrimson,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onClearError) {
                                Text("Dismiss", color = ExpenseCrimson)
                            }
                        }
                    }
                }

                if (state.activeCallScript.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CardInnerTileSolid,
                        border = BorderStroke(1.dp, CardBorderSolid),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_call_script_box")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "${state.selectedVoice.agentName} (Female AI) Call Script",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.activeCallScript,
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTextPrimary
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionPanelCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_call_center_queue_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monday Dues Call Queue",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GlassTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.dueQueue.isNotEmpty()) {
                        TextButton(
                            onClick = onResetQueue,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("ai_call_reset_queue_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QueueStatBadge(
                        text = "Due Members: ${state.dueQueue.size}",
                        bg = ExpenseSurface,
                        fg = ExpenseCrimson
                    )
                    QueueStatBadge(
                        text = "Total Dues: ${BanglaFormatters.formatBdt(state.totalDuesAmount)}",
                        bg = NagadSurface,
                        fg = NagadAccent
                    )
                    QueueStatBadge(
                        text = "Called: ${state.calledCount}",
                        bg = IncomeSurface,
                        fg = IncomeEmerald
                    )
                }

                if (state.dueQueue.isEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = IncomeEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No members currently have pending dues.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassTextSecondary
                        )
                    }
                }
            }
        }

        items(state.dueQueue, key = { it.memberId }) { item ->
            SectionPanelCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_call_queue_item_${item.memberId}")
            ) {
                DueMemberCallCardContent(
                    item = item,
                    isActive = state.activeMemberId == item.memberId && state.isCallActive,
                    onFreeAiCall = { onCallMember(item.memberId) },
                    onDialSim = {
                        launchWithCallPermissionIfNeeded(true) { onDialMemberPhone(item.memberId) }
                    },
                    onPlayVoice = { onPlayMemberVoiceOnly(item.memberId) },
                    onToggleCalled = {
                        val next = if (item.callStatus == AiCallStatus.CALLED) {
                            AiCallStatus.PENDING
                        } else {
                            AiCallStatus.CALLED
                        }
                        onMarkCallStatus(item.memberId, next)
                    }
                )
            }
        }
    }
}

@Composable
private fun LiveFreeAiCallCard(
    state: AiCallCenterUiState,
    onStop: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = HeroCardSolid,
        border = BorderStroke(1.4.dp, IncomeEmerald.copy(alpha = 0.72f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_call_live_banner_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = IncomeSurface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhoneInTalk,
                            contentDescription = null,
                            tint = IncomeEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "FREE AI CALL • FEMALE VOICE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = IncomeEmerald
                        )
                    }
                }
                if (state.activeCallTotalCount > 0) {
                    Text(
                        text = "${state.activeCallIndex}/${state.activeCallTotalCount}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GlassTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = state.activeCallMemberName.ifBlank { "Connecting..." },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GlassTextPrimary
            )
            Text(
                text = listOfNotNull(
                    state.activeCallMemberPhone.takeIf { it.isNotBlank() },
                    if (state.activeCallDuesAmount > 0.0) {
                        "Due: ${BanglaFormatters.formatBdt(state.activeCallDuesAmount)}"
                    } else null
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GraphicEq,
                        contentDescription = null,
                        tint = IncomeEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.activeCallPhaseLabel.ifBlank {
                            "${state.selectedVoice.agentName} (Female) Speaking..."
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = IncomeEmerald,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onStop,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseCrimson),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Stop,
                        contentDescription = "Stop call",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("End", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun QueueStatBadge(
    text: String,
    bg: Color,
    fg: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DueMemberCallCardContent(
    item: AiCallQueueItem,
    isActive: Boolean,
    onFreeAiCall: () -> Unit,
    onDialSim: () -> Unit,
    onPlayVoice: () -> Unit,
    onToggleCalled: () -> Unit
) {
    val (statusBg, statusFg) = when (item.callStatus) {
        AiCallStatus.CALLED -> IncomeSurface to IncomeEmerald
        AiCallStatus.DIALING -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        AiCallStatus.NO_PHONE -> ExpenseSurface to ExpenseCrimson
        AiCallStatus.SKIPPED -> CardInnerTileSolid to GlassTextSecondary
        AiCallStatus.PENDING -> NagadSurface to NagadAccent
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (item.code.isNotBlank()) "${item.memberName} (${item.code})" else item.memberName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GlassTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val meta = listOfNotNull(
                item.area.takeIf { it.isNotBlank() },
                item.rawPhone.takeIf { it.isNotBlank() } ?: "In-App Free Voice"
            ).joinToString(" • ")
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = BanglaFormatters.formatBdt(item.duesAmount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ExpenseCrimson
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusBg
            ) {
                Text(
                    text = item.callStatus.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusFg,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onFreeAiCall,
            enabled = !isActive,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("ai_call_member_button_${item.memberId}")
        ) {
            Icon(
                imageVector = Icons.Outlined.PhoneInTalk,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (isActive) "Speaking..." else "Free AI Call",
                maxLines = 1
            )
        }

        if (item.hasCallablePhone) {
            OutlinedButton(
                onClick = onDialSim,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("ai_dial_sim_button_${item.memberId}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Call,
                    contentDescription = "Dial SIM phone",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("SIM", maxLines = 1)
            }
        }

        OutlinedButton(
            onClick = onPlayVoice,
            enabled = !isActive,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier
                .height(44.dp)
                .testTag("ai_voice_member_button_${item.memberId}")
        ) {
            Icon(
                imageVector = Icons.Outlined.RecordVoiceOver,
                contentDescription = "Play female AI voice",
                modifier = Modifier.size(16.dp)
            )
        }

        TextButton(
            onClick = onToggleCalled,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(44.dp)
        ) {
            Text(
                text = if (item.callStatus == AiCallStatus.CALLED) "Undo" else "Done",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
