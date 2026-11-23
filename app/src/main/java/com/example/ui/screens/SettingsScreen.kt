package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.AiVoiceScope
import com.example.ai.AiVoiceStatus
import com.example.ai.FemaleAiVoiceOption
import com.example.data.CloudSyncUiState
import com.example.data.MemberAppLinkStore
import com.example.model.Member
import com.example.ui.AiVoiceUiState
import com.example.ui.DriveUiState
import com.example.ui.components.CloudAutoSyncCard
import com.example.ui.components.SectionPanelCard
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    appLoginId: String,
    members: List<Member> = emptyList(),
    memberCount: Int,
    activeMemberCount: Int,
    isOnlinePaymentEnabled: Boolean,
    takaJomaCount: Int = 0,
    salesCount: Int = 0,
    memberAppDownloadUrl: String = "",
    transactionCount: Int,
    isBusy: Boolean,
    driveState: DriveUiState = DriveUiState(),
    cloudSyncState: CloudSyncUiState = CloudSyncUiState(),
    aiVoiceState: AiVoiceUiState = AiVoiceUiState(),
    onOpenMemberList: () -> Unit,
    onOpenMemberTakaJoma: () -> Unit = {},
    onOpenSalesUpload: () -> Unit = {},
    onOpenOnlinePayment: () -> Unit,
    onOpenMemberAppLink: () -> Unit = {},
    onOpenChangePassword: () -> Unit,
    onAppLogout: () -> Unit,
    onExportJson: () -> Unit,
    onRestoreJson: () -> Unit,
    onExportPicture: () -> Unit,
    onToggleCloudAutoSync: (Boolean) -> Unit = {},
    onSyncCloudNow: () -> Unit = {},
    onCloudSignOut: () -> Unit = {},
    isCloudSigningIn: Boolean = false,
    cloudAuthError: String? = null,
    onCloudSignIn: () -> Unit = {},
    onSwitchCloudAccount: () -> Unit = onCloudSignOut,
    onConnectGoogleDrive: () -> Unit = {},
    onLinkGoogleDriveFile: () -> Unit = {},
    onRestoreFromDriveFile: () -> Unit = {},
    onShowDriveAccessHelp: () -> Unit = {},
    driveAuthBlockedEmail: String? = null,
    onBackupToGoogleDrive: () -> Unit = {},
    onRestoreFromGoogleDrive: () -> Unit = {},
    onToggleDriveAutoBackup: (Boolean) -> Unit = {},
    onDisconnectGoogleDrive: () -> Unit = {},
    onSelectAiVoiceOption: (FemaleAiVoiceOption) -> Unit = {},
    onSelectAiVoiceScope: (AiVoiceScope) -> Unit = {},
    onSelectAiVoiceMember: (String) -> Unit = {},
    onSpeakAiVoiceReport: () -> Unit = {},
    onReplayAiVoiceReport: () -> Unit = {},
    onStopAiVoiceReport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val onBg = IncomeSurface
    val onFg = IncomeEmerald
    val offBg = ExpenseSurface
    val offFg = ExpenseCrimson

    var showDriveRestoreConfirm by remember { mutableStateOf(false) }
    var showDriveDisconnectConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingsListOptionRow(
                    icon = Icons.Outlined.Groups,
                    title = "Member List",
                    subtitle = "$memberCount total • $activeMemberCount active",
                    badgeText = memberCount.toString(),
                    badgeBg = MaterialTheme.colorScheme.primaryContainer,
                    badgeFg = MaterialTheme.colorScheme.primary,
                    onClick = onOpenMemberList,
                    testTag = "settings_member_list_item"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "Member Deposit Form",
                    subtitle = "Name, code, depot & deposit records",
                    badgeText = takaJomaCount.toString(),
                    badgeBg = onBg,
                    badgeFg = onFg,
                    onClick = onOpenMemberTakaJoma,
                    testTag = "settings_member_taka_joma_item"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.TrendingUp,
                    title = "Monthly Sales Upload",
                    subtitle = "Upload monthly code, name, TP sales & TP+VAT sales",
                    badgeText = salesCount.toString(),
                    badgeBg = MaterialTheme.colorScheme.primaryContainer,
                    badgeFg = MaterialTheme.colorScheme.primary,
                    onClick = onOpenSalesUpload,
                    testTag = "settings_sales_upload_item"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.Payments,
                    title = "Online Payment",
                    subtitle = "bKash, Nagad, Rocket & bank payment settings",
                    badgeText = if (isOnlinePaymentEnabled) "ON" else "OFF",
                    badgeBg = if (isOnlinePaymentEnabled) onBg else offBg,
                    badgeFg = if (isOnlinePaymentEnabled) onFg else offFg,
                    onClick = onOpenOnlinePayment,
                    testTag = "settings_online_payment_item"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.PhoneAndroid,
                    title = "Member App Link",
                    subtitle = memberAppDownloadUrl.ifBlank { MemberAppLinkStore.DEFAULT_MEMBER_APP_URL },
                    badgeText = if (memberAppDownloadUrl.isNotBlank()) "Custom" else "Ready",
                    badgeBg = MaterialTheme.colorScheme.primaryContainer,
                    badgeFg = MaterialTheme.colorScheme.primary,
                    onClick = onOpenMemberAppLink,
                    testTag = "settings_member_app_link_item"
                )
            }
        }

        // App Auto-Sync (Firestore Cloud Sync) Card (Admin Only)
        item {
            CloudAutoSyncCard(
                cloudSyncState = cloudSyncState,
                onToggleAutoSync = onToggleCloudAutoSync,
                onSyncNow = onSyncCloudNow,
                onCloudSignOut = onCloudSignOut,
                isSigningIn = isCloudSigningIn,
                authError = cloudAuthError,
                onCloudSignIn = onCloudSignIn,
                onSwitchCloudAccount = onSwitchCloudAccount
            )
        }

        // Female Bangla AI Voice Report Card
        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RecordVoiceOver,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Voice Summary Report",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Listen to member sales, deposits, and dues read aloud",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (aiVoiceState.isPlaying) onBg else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = when (aiVoiceState.status) {
                                AiVoiceStatus.Playing -> "Playing"
                                AiVoiceStatus.ComposingScript,
                                AiVoiceStatus.GeneratingAudio -> "Preparing"
                                else -> "Voice"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (aiVoiceState.isPlaying) onFg else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Select Voice:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FemaleAiVoiceOption.entries.forEach { voice ->
                        val selected = aiVoiceState.selectedVoice == voice
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectAiVoiceOption(voice) },
                            enabled = !aiVoiceState.isBusy && !aiVoiceState.isPlaying,
                            label = {
                                Text(
                                    text = voice.bnLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("ai_voice_option_${voice.voiceId.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Report Scope:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiVoiceScope.entries.forEach { scopeOption ->
                        val selected = aiVoiceState.selectedScope == scopeOption
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectAiVoiceScope(scopeOption) },
                            enabled = !aiVoiceState.isBusy && !aiVoiceState.isPlaying,
                            label = {
                                Text(
                                    text = scopeOption.bnLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("ai_voice_scope_${scopeOption.name.lowercase()}")
                        )
                    }
                }

                if (aiVoiceState.selectedScope == AiVoiceScope.SINGLE_MEMBER && members.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Select Member:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val effectiveSelectedId = aiVoiceState.selectedMemberId ?: members.firstOrNull()?.id
                        members.forEach { m ->
                            val selected = m.id == effectiveSelectedId
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectAiVoiceMember(m.id) },
                                enabled = !aiVoiceState.isBusy && !aiVoiceState.isPlaying,
                                label = {
                                    Text(
                                        text = if (m.code.isNotBlank()) "${m.name} (${m.code})" else m.name,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag("ai_voice_member_${m.id}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (aiVoiceState.isPlaying) {
                        Button(
                            onClick = onStopAiVoiceReport,
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseCrimson),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_ai_voice_stop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop Voice", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onSpeakAiVoiceReport,
                            enabled = !aiVoiceState.isBusy && !isBusy,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_ai_voice_speak_button")
                        ) {
                            if (aiVoiceState.isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (aiVoiceState.status == AiVoiceStatus.ComposingScript) {
                                        "Preparing report..."
                                    } else {
                                        "Generating voice..."
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Listen to Summary", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (aiVoiceState.hasCachedAudio && !aiVoiceState.isBusy && !aiVoiceState.isPlaying) {
                        OutlinedButton(
                            onClick = onReplayAiVoiceReport,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("admin_ai_voice_replay_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Replay,
                                contentDescription = "Replay summary",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Replay")
                        }
                    }
                }

                when (val voiceStatus = aiVoiceState.status) {
                    is AiVoiceStatus.Playing -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = onBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.GraphicEq,
                                    contentDescription = null,
                                    tint = onFg,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Playing summary with ${aiVoiceState.selectedVoice.bnLabel}...",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = onFg
                                )
                            }
                        }
                    }

                    is AiVoiceStatus.Error -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = offBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = voiceStatus.message,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = offFg,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                            )
                        }
                    }

                    else -> Unit
                }

                if (aiVoiceState.lastScript.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_ai_voice_transcript_box")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Voice Summary Transcript:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = aiVoiceState.lastScript,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Google Drive Full Backup & Cloud Sync Card
        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google Drive Backup & Sync",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Keep all transactions, members, deposits & sales backed up on Google Drive",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (driveState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (driveState.isConnected) onBg else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (driveState.isConnected) "Connected" else "Not Connected",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (driveState.isConnected) onFg else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (driveState.isConnected) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = onBg.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (driveState.connectedEmail.isNotBlank()) {
                                Text(
                                    text = "Google Account: ${driveState.connectedEmail}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = onFg
                                )
                            }
                            if (driveState.safDocumentUri.isNotBlank()) {
                                Text(
                                    text = "Linked Drive File: ${driveState.safDisplayName.ifBlank { "rafiqul-hisab-full-backup.json" }}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = onFg
                                )
                            } else if (driveState.connectedEmail.isBlank()) {
                                Text(
                                    text = "Google Drive Connected",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = onFg
                                )
                            }
                            if (driveState.lastBackupAtMs > 0L) {
                                Text(
                                    text = "Last Backup: ${formatBackupTime(driveState.lastBackupAtMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onFg
                                )
                            } else {
                                Text(
                                    text = "No backup saved to Google Drive yet",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (driveState.lastBackupSummary.isNotBlank()) {
                                Text(
                                    text = driveState.lastBackupSummary,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto Google Drive Backup",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Automatically save changes to Google Drive",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = driveState.autoBackupEnabled,
                            onCheckedChange = onToggleDriveAutoBackup,
                            enabled = !driveState.isSyncing,
                            modifier = Modifier.testTag("drive_auto_backup_switch")
                        )
                    }

                    SettingsListDivider()

                    SettingsListOptionRow(
                        icon = Icons.Outlined.CloudUpload,
                        title = "Back Up All Data to Google Drive",
                        subtitle = "Save transactions, members, deposits, sales & payments",
                        enabled = !isBusy && !driveState.isSyncing,
                        iconBg = onBg,
                        iconTint = onFg,
                        onClick = onBackupToGoogleDrive,
                        testTag = "drive_backup_now_button"
                    )
                    SettingsListDivider()

                    SettingsListOptionRow(
                        icon = Icons.Outlined.CloudDownload,
                        title = "Restore from Google Drive",
                        subtitle = "Restore all saved data from your Drive backup",
                        enabled = !isBusy && !driveState.isSyncing,
                        onClick = { showDriveRestoreConfirm = true },
                        testTag = "drive_restore_button"
                    )
                    SettingsListDivider()

                    SettingsListOptionRow(
                        icon = Icons.Outlined.CloudDone,
                        title = if (driveState.safDocumentUri.isNotBlank()) {
                            "Change Linked Drive File"
                        } else {
                            "Link Google Drive File (Any Gmail)"
                        },
                        subtitle = "Auto-save directly to your Google Drive folder",
                        enabled = !isBusy && !driveState.isSyncing,
                        onClick = onLinkGoogleDriveFile,
                        testTag = "drive_link_saf_file_button"
                    )
                    SettingsListDivider()

                    SettingsListOptionRow(
                        icon = Icons.Outlined.Sync,
                        title = if (driveState.oauthConnected) {
                            "Switch Google Account"
                        } else {
                            "Connect Google Account"
                        },
                        subtitle = "Connect another Gmail or Google Drive account",
                        enabled = !isBusy && !driveState.isSyncing,
                        onClick = onConnectGoogleDrive,
                        testTag = "drive_switch_account_button"
                    )
                    SettingsListDivider()

                    SettingsListOptionRow(
                        icon = Icons.Outlined.LinkOff,
                        title = "Disconnect Google Drive",
                        subtitle = "Remove Drive link from this device",
                        enabled = !isBusy && !driveState.isSyncing,
                        iconBg = offBg,
                        iconTint = offFg,
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { showDriveDisconnectConfirm = true },
                        testTag = "drive_disconnect_button"
                    )
                } else {
                    if (!driveAuthBlockedEmail.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = offBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onShowDriveAccessHelp)
                                .testTag("drive_403_notice_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Direct Drive Access Unavailable ($driveAuthBlockedEmail)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = offFg
                                )
                                Text(
                                    text = "Tap \"Link Google Drive File\" below for instant backup with any Gmail, or tap here for help.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    SettingsListOptionRow(
                        icon = Icons.Outlined.CloudUpload,
                        title = "Link Google Drive File (Any Gmail)",
                        subtitle = "Enable auto-backup directly to your Drive folder",
                        badgeText = "Recommended",
                        badgeBg = onBg,
                        badgeFg = onFg,
                        iconBg = onBg,
                        iconTint = onFg,
                        enabled = !isBusy && !driveState.isSyncing,
                        onClick = onLinkGoogleDriveFile,
                        testTag = "drive_link_saf_file_button"
                    )
                    SettingsListDivider()

                    SettingsListOptionRow(
                        icon = Icons.Outlined.CloudDownload,
                        title = "Restore from Google Drive File",
                        subtitle = "Select an existing backup file saved in Google Drive",
                        enabled = !isBusy && !driveState.isSyncing,
                        onClick = onRestoreFromDriveFile,
                        testTag = "drive_restore_saf_file_button"
                    )
                    SettingsListDivider()

                    SettingsListOptionRow(
                        icon = Icons.Outlined.CloudDone,
                        title = "Connect Google Drive Account",
                        subtitle = "Sign in directly with your Google account",
                        badgeText = "Connect",
                        badgeBg = MaterialTheme.colorScheme.primaryContainer,
                        badgeFg = MaterialTheme.colorScheme.primary,
                        enabled = !isBusy && !driveState.isSyncing,
                        onClick = onConnectGoogleDrive,
                        testTag = "drive_connect_button"
                    )
                }
            }
        }

        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Backup & Reports",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$transactionCount saved transaction(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                SettingsListOptionRow(
                    icon = Icons.Outlined.Download,
                    title = stringResource(R.string.json_backup),
                    subtitle = "Save all transactions, members & sales data to a JSON file",
                    enabled = !isBusy,
                    onClick = onExportJson,
                    testTag = "export_json_button"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.FileUpload,
                    title = stringResource(R.string.restore_backup),
                    subtitle = "Restore data from a saved JSON backup file",
                    enabled = !isBusy,
                    onClick = onRestoreJson,
                    testTag = "restore_json_button"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.Image,
                    title = stringResource(R.string.picture_export),
                    subtitle = "Download account report as a PNG image",
                    enabled = !isBusy,
                    onClick = onExportPicture,
                    testTag = "settings_export_picture_button"
                )
            }
        }

        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Account & Security",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                SettingsListOptionRow(
                    icon = Icons.Outlined.Lock,
                    title = "Change Password",
                    subtitle = "Login ID: $appLoginId",
                    onClick = onOpenChangePassword,
                    testTag = "change_password_button"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Log Out",
                    subtitle = "Lock session and return to sign-in screen",
                    iconBg = offBg,
                    iconTint = offFg,
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = onAppLogout,
                    testTag = "app_logout_button"
                )
            }
        }
    }

    if (showDriveRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showDriveRestoreConfirm = false },
            title = { Text("Restore from Google Drive?") },
            text = {
                Text("All saved transactions, members, deposits & sales will be restored from your Google Drive backup.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDriveRestoreConfirm = false
                        onRestoreFromGoogleDrive()
                    },
                    modifier = Modifier.testTag("confirm_drive_restore_button")
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDriveRestoreConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDriveDisconnectConfirm) {
        AlertDialog(
            onDismissRequest = { showDriveDisconnectConfirm = false },
            title = { Text("Disconnect Google Drive?") },
            text = {
                Text("Disconnecting stops automatic backups, while your existing backup file in Google Drive remains safe.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDriveDisconnectConfirm = false
                        onDisconnectGoogleDrive()
                    },
                    modifier = Modifier.testTag("confirm_drive_disconnect_button")
                ) {
                    Text("Disconnect", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDriveDisconnectConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatBackupTime(timestampMs: Long): String {
    if (timestampMs <= 0L) return "-"
    val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
    return sdf.format(Date(timestampMs))
}

@Composable
private fun SettingsListDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
    )
}

@Composable
private fun SettingsListOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
    enabled: Boolean = true,
    badgeText: String? = null,
    badgeBg: Color = MaterialTheme.colorScheme.primaryContainer,
    badgeFg: Color = MaterialTheme.colorScheme.primary,
    iconBg: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) titleColor else titleColor.copy(alpha = 0.5f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (badgeText != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = badgeBg
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeFg,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
