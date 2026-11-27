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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import com.example.data.CallScheduleDay
import com.example.model.BanglaFormatters
import com.example.ui.AiCallCenterUiState
import com.example.ui.components.CleanHomeMetricTile
import com.example.ui.components.GlossyInnerMetricTile
import com.example.ui.components.GlossyNeonIconOrb
import com.example.ui.components.UnifiedPageCardSurface
import com.example.ui.components.UnifiedPageSectionDivider
import com.example.ui.components.hisab3DButtonSurface
import com.example.ui.components.hisabFilterChipBorder
import com.example.ui.components.hisabFilterChipColors
import com.example.ui.theme.CardBorderSolid
import com.example.ui.theme.CardInnerTileSolid
import com.example.ui.theme.CardSurfaceSolid
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface
import com.example.ui.theme.NagadAccent
import com.example.ui.theme.NagadSurface
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonPurple

private val QUICK_CALL_HOUR_SLOTS = listOf(9, 10, 11, 14, 16, 20)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AiCallCenterScreen(
    state: AiCallCenterUiState,
    onToggleMondayAutoCall: (Boolean) -> Unit,
    onToggleCallDay: (Int) -> Unit,
    onSelectCallHour: (Int) -> Unit,
    onUpdateCallTime: (Int, Int) -> Unit,
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
    var showTimePickerDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(
        state.autoCallMondayEnabled,
        state.enabledDays,
        state.scheduledHour,
        state.scheduledMinute
    ) {
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

    val femaleVoices = remember { FemaleAiVoiceOption.entries.filter { !it.isMale } }
    val maleVoices = remember { FemaleAiVoiceOption.entries.filter { it.isMale } }
    val activeDaysSummary = remember(state.enabledDays) {
        AiCallCenterPrefsStore.formatEnabledDaysSummary(state.enabledDays)
    }
    val formattedCurrentTime = remember(state.scheduledHour, state.scheduledMinute) {
        AiCallCenterPrefsStore.formatTimeSlot(state.scheduledHour, state.scheduledMinute)
    }

    UnifiedPageCardSurface(
        modifier = modifier.testTag("ai_call_center_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isCallActive) {
                item {
                    LiveFreeAiCallCard(
                        state = state,
                        onStop = onStopVoice
                    )
                    UnifiedPageSectionDivider(modifier = Modifier.padding(vertical = 14.dp))
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
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
                        GlossyNeonIconOrb(
                            icon = Icons.Outlined.PhoneInTalk,
                            contentDescription = null,
                            glowColor = NeonEmerald,
                            size = 42.dp,
                            iconSize = 21.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Free AI Call Center",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GlassTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            state.isMondayToday && state.autoCallMondayEnabled -> IncomeSurface
                            state.autoCallMondayEnabled && state.enabledDays.isNotEmpty() -> MaterialTheme.colorScheme.primaryContainer
                            else -> ExpenseSurface
                        }
                    ) {
                        Text(
                            text = when {
                                state.isMondayToday && state.autoCallMondayEnabled -> "Today • Active"
                                state.autoCallMondayEnabled && state.enabledDays.isNotEmpty() -> "Scheduled"
                                else -> "Paused"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                state.isMondayToday && state.autoCallMondayEnabled -> IncomeEmerald
                                state.autoCallMondayEnabled && state.enabledDays.isNotEmpty() -> MaterialTheme.colorScheme.primary
                                else -> ExpenseCrimson
                            },
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                GlossyInnerMetricTile(
                    accentColor = NeonCyan,
                    shape = RoundedCornerShape(14.dp),
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
                                    text = "Auto-Call Schedule",
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
                            color = Color.White.copy(alpha = 0.12f)
                        )

                        Text(
                            text = "Select Call Days (Turn ON days to call):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_call_seven_days_selector"),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CallScheduleDay.entries.forEach { day ->
                                val selected = day.calendarDay in state.enabledDays
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        onToggleCallDay(day.calendarDay)
                                        if (!selected && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            val notifGranted = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (!notifGranted) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = if (selected) "${day.fullLabel} ON" else day.fullLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = hisabFilterChipColors(accentColor = NeonEmerald),
                                    border = hisabFilterChipBorder(
                                        selected = selected,
                                        accentColor = NeonEmerald
                                    ),
                                    modifier = Modifier.testTag("ai_call_day_${day.name.lowercase()}")
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 0.8.dp,
                            color = Color.White.copy(alpha = 0.12f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Call Time",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GlassTextPrimary
                                    )
                                    Text(
                                        text = formattedCurrentTime,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan,
                                        modifier = Modifier.testTag("ai_call_current_time_text")
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { showTimePickerDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = CardSurfaceSolid,
                                    contentColor = NeonCyan
                                ),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.55f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .hisab3DButtonSurface(accentColor = NeonCyan, cornerRadius = 12.dp)
                                    .testTag("ai_call_update_time_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Update Time",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QUICK_CALL_HOUR_SLOTS.forEach { hour ->
                                val selected = state.scheduledHour == hour && state.scheduledMinute == 0
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectCallHour(hour) },
                                    label = {
                                        Text(
                                            text = AiCallCenterPrefsStore.formatHourSlot(hour),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = hisabFilterChipColors(accentColor = NeonCyan),
                                    border = hisabFilterChipBorder(
                                        selected = selected,
                                        accentColor = NeonCyan
                                    ),
                                    modifier = Modifier.testTag("ai_call_hour_${hour}")
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 0.8.dp,
                            color = Color.White.copy(alpha = 0.12f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AI Call Speaker",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GlassTextPrimary
                                )
                                Text(
                                    text = "Active: ${state.selectedVoice.agentName} (${state.selectedVoice.genderLabel})",
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Female AI Speaker:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            femaleVoices.forEach { voice ->
                                val selected = state.selectedVoice == voice
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectVoice(voice) },
                                    label = {
                                        Text(
                                            text = voice.bnLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = hisabFilterChipColors(accentColor = NeonPurple),
                                    border = hisabFilterChipBorder(
                                        selected = selected,
                                        accentColor = NeonPurple
                                    ),
                                    modifier = Modifier.testTag("ai_call_voice_${voice.voiceId.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Male AI Speaker:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeonGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_call_male_speakers_row"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            maleVoices.forEach { voice ->
                                val selected = state.selectedVoice == voice
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectVoice(voice) },
                                    label = {
                                        Text(
                                            text = voice.bnLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = hisabFilterChipColors(accentColor = NeonGold),
                                    border = hisabFilterChipBorder(
                                        selected = selected,
                                        accentColor = NeonGold
                                    ),
                                    modifier = Modifier.testTag("ai_call_voice_${voice.name.lowercase()}")
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 0.8.dp,
                            color = Color.White.copy(alpha = 0.12f)
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
                            .hisab3DButtonSurface(accentColor = NeonEmerald, cornerRadius = 12.dp)
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
                                    "${state.selectedVoice.agentName} Speaking..."
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
                                .hisab3DButtonSurface(accentColor = ExpenseCrimson, cornerRadius = 12.dp)
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
                    GlossyInnerMetricTile(
                        accentColor = NeonEmerald,
                        shape = RoundedCornerShape(10.dp),
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
                    GlossyInnerMetricTile(
                        accentColor = ExpenseCrimson,
                        shape = RoundedCornerShape(10.dp),
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
                    GlossyInnerMetricTile(
                        accentColor = NeonCyan,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_call_script_box")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "${state.selectedVoice.agentName} (${state.selectedVoice.genderLabel}) Call Script",
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
            UnifiedPageSectionDivider(modifier = Modifier.padding(vertical = 14.dp))
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .testTag("ai_call_center_queue_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scheduled Dues Call Queue",
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
            UnifiedPageSectionDivider(modifier = Modifier.padding(vertical = 12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .testTag("ai_call_queue_item_${item.memberId}")
            ) {
                DueMemberCallCardContent(
                    item = item,
                    isActive = state.activeMemberId == item.memberId && state.isCallActive,
                    speakerLabel = state.selectedVoice.genderLabel,
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

    if (showTimePickerDialog) {
        val pickerState = rememberTimePickerState(
            initialHour = state.scheduledHour,
            initialMinute = state.scheduledMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            containerColor = CardSurfaceSolid,
            titleContentColor = GlassTextPrimary,
            textContentColor = GlassTextSecondary,
            title = {
                Text(
                    text = "Update Call Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = pickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateCallTime(pickerState.hour, pickerState.minute)
                        showTimePickerDialog = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .hisab3DButtonSurface(accentColor = NeonCyan, cornerRadius = 10.dp)
                        .testTag("ai_call_confirm_time_button")
                ) {
                    Text("Update Time", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("Cancel", color = GlassTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun LiveFreeAiCallCard(
    state: AiCallCenterUiState,
    onStop: () -> Unit
) {
    CleanHomeMetricTile(
        accentColor = NeonEmerald,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
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
                            text = "FREE AI CALL • ${state.selectedVoice.genderLabel.uppercase()}",
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
                            "${state.selectedVoice.agentName} (${state.selectedVoice.genderLabel}) Speaking..."
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
                    modifier = Modifier
                        .height(38.dp)
                        .hisab3DButtonSurface(accentColor = ExpenseCrimson, cornerRadius = 10.dp)
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
    speakerLabel: String,
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
                .hisab3DButtonSurface(accentColor = NeonCyan, cornerRadius = 10.dp)
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
                    .hisab3DButtonSurface(accentColor = NeonBlue, cornerRadius = 10.dp)
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
                .hisab3DButtonSurface(accentColor = NeonPurple, cornerRadius = 10.dp)
                .testTag("ai_voice_member_button_${item.memberId}")
        ) {
            Icon(
                imageVector = Icons.Outlined.RecordVoiceOver,
                contentDescription = "Play $speakerLabel voice",
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
