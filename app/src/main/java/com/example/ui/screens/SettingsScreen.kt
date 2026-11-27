package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.AiVoiceScope
import com.example.ai.AiVoiceStatus
import com.example.ai.FemaleAiVoiceOption
import com.example.data.AiCallCenterPrefsStore
import com.example.data.AppUpdateStatus
import com.example.data.AppUpdateUiState
import com.example.data.CloudSyncUiState
import com.example.model.Member
import com.example.ui.AiCallCenterUiState
import com.example.ui.AiVoiceUiState
import com.example.ui.DriveUiState
import com.example.ui.components.AppAutoUpdateCard
import com.example.ui.components.CloudAutoSyncCard
import com.example.ui.components.GlossyInnerMetricTile
import com.example.ui.components.GlossyNeonIconOrb
import com.example.ui.components.SectionPanelCard
import com.example.ui.components.UnifiedPageCardSurface
import com.example.ui.components.hisab3DButtonSurface
import com.example.ui.theme.BrandDeep
import com.example.ui.theme.CardSurfaceSolid
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface
import com.example.ui.theme.NagadAccent
import com.example.ui.theme.NagadSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SettingsDetailSheet {
    CLOUD_SYNC,
    APP_UPDATE,
    VOICE_REPORT,
    GOOGLE_DRIVE
}

@OptIn(ExperimentalMaterial3Api::class)
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
    aiCallCenterState: AiCallCenterUiState = AiCallCenterUiState(),
    onOpenMemberList: () -> Unit,
    onOpenMemberTakaJoma: () -> Unit = {},
    onOpenSalesUpload: () -> Unit = {},
    onOpenOnlinePayment: () -> Unit,
    onOpenAiCallCenter: () -> Unit = {},
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
    appUpdateState: AppUpdateUiState = AppUpdateUiState(),
    onToggleAutoUpdate: (Boolean) -> Unit = {},
    onCheckAppUpdate: () -> Unit = {},
    onCompleteAppUpdate: () -> Unit = {},
    onOpenAppUpdateLink: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val onBg = IncomeSurface
    val onFg = IncomeEmerald
    val offBg = ExpenseSurface
    val offFg = ExpenseCrimson

    var activeDetailSheet by remember { mutableStateOf<SettingsDetailSheet?>(null) }
    var showDriveRestoreConfirm by remember { mutableStateOf(false) }
    var showDriveDisconnectConfirm by remember { mutableStateOf(false) }

    val cloudSyncBadge = when {
        cloudSyncState.isSyncing || isCloudSigningIn -> "Syncing"
        !cloudSyncState.isConnected -> "Offline"
        cloudSyncState.autoSyncEnabled -> "Active"
        else -> "Paused"
    }
    val cloudSyncBadgeBg = when {
        !cloudSyncState.isConnected -> MaterialTheme.colorScheme.surfaceVariant
        cloudSyncState.autoSyncEnabled -> onBg
        else -> offBg
    }
    val cloudSyncBadgeFg = when {
        !cloudSyncState.isConnected -> MaterialTheme.colorScheme.onSurfaceVariant
        cloudSyncState.autoSyncEnabled -> onFg
        else -> offFg
    }

    val updateBadge = when (appUpdateState.status) {
        AppUpdateStatus.CHECKING -> "Checking"
        AppUpdateStatus.UP_TO_DATE -> "Latest"
        AppUpdateStatus.UPDATE_AVAILABLE -> "Update"
        AppUpdateStatus.DOWNLOADING -> "${appUpdateState.downloadProgressPercent}%"
        AppUpdateStatus.DOWNLOADED -> "Restart"
        AppUpdateStatus.PLAY_UNAVAILABLE,
        AppUpdateStatus.IDLE -> "v${appUpdateState.currentVersionName}"
        AppUpdateStatus.ERROR -> "Retry"
    }
    val updateBadgeBg = when (appUpdateState.status) {
        AppUpdateStatus.DOWNLOADED,
        AppUpdateStatus.UP_TO_DATE -> onBg
        AppUpdateStatus.UPDATE_AVAILABLE,
        AppUpdateStatus.DOWNLOADING -> NagadSurface
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val updateBadgeFg = when (appUpdateState.status) {
        AppUpdateStatus.DOWNLOADED,
        AppUpdateStatus.UP_TO_DATE -> onFg
        AppUpdateStatus.UPDATE_AVAILABLE,
        AppUpdateStatus.DOWNLOADING -> NagadAccent
        else -> MaterialTheme.colorScheme.primary
    }

    UnifiedPageCardSurface(
        modifier = modifier,
        accentColor = NeonPurple,
        secondaryGlow = NeonCyan
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .padding(horizontal = 14.dp)
                        .testTag("settings_single_card")
                ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                SettingsListOptionRow(
                    icon = Icons.Outlined.Groups,
                    title = "Member List",
                    badgeText = memberCount.toString(),
                    badgeBg = MaterialTheme.colorScheme.primaryContainer,
                    badgeFg = MaterialTheme.colorScheme.primary,
                    iconGlowColor = NeonCyan,
                    onClick = onOpenMemberList,
                    testTag = "settings_member_list_item"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "Member Deposit Form",
                    badgeText = takaJomaCount.toString(),
                    badgeBg = onBg,
                    badgeFg = onFg,
                    iconGlowColor = NeonEmerald,
                    onClick = onOpenMemberTakaJoma,
                    testTag = "settings_member_taka_joma_item"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.AutoMirrored.Outlined.TrendingUp,
                    title = "Monthly Sales Upload",
                    badgeText = salesCount.toString(),
                    badgeBg = MaterialTheme.colorScheme.primaryContainer,
                    badgeFg = MaterialTheme.colorScheme.primary,
                    iconGlowColor = NeonGold,
                    onClick = onOpenSalesUpload,
                    testTag = "settings_sales_upload_item"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.Payments,
                    title = "Online Payment",
                    badgeText = if (isOnlinePaymentEnabled) "ON" else "OFF",
                    badgeBg = if (isOnlinePaymentEnabled) onBg else offBg,
                    badgeFg = if (isOnlinePaymentEnabled) onFg else offFg,
                    iconGlowColor = if (isOnlinePaymentEnabled) NeonEmerald else NeonGold,
                    onClick = onOpenOnlinePayment,
                    testTag = "settings_online_payment_item"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.PhoneInTalk,
                    title = "AI Call Center",
                    badgeText = if (aiCallCenterState.dueQueue.isNotEmpty()) {
                        "${aiCallCenterState.dueQueue.size} Due"
                    } else if (aiCallCenterState.autoCallMondayEnabled && aiCallCenterState.enabledDays.isNotEmpty()) {
                        AiCallCenterPrefsStore.formatTimeSlot(
                            aiCallCenterState.scheduledHour,
                            aiCallCenterState.scheduledMinute
                        )
                    } else {
                        "Paused"
                    },
                    badgeBg = if (aiCallCenterState.dueQueue.isNotEmpty()) offBg else onBg,
                    badgeFg = if (aiCallCenterState.dueQueue.isNotEmpty()) offFg else onFg,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconGlowColor = NeonPurple,
                    onClick = onOpenAiCallCenter,
                    testTag = "settings_ai_call_center_item"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.PhoneAndroid,
                    title = "Member App Link",
                    badgeText = if (memberAppDownloadUrl.isNotBlank()) "Custom" else "Ready",
                    badgeBg = MaterialTheme.colorScheme.primaryContainer,
                    badgeFg = MaterialTheme.colorScheme.primary,
                    iconGlowColor = NeonCyan,
                    onClick = onOpenMemberAppLink,
                    testTag = "settings_member_app_link_item"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = if (cloudSyncState.isConnected) Icons.Outlined.CloudSync else Icons.Outlined.CloudOff,
                    title = "Google Account & Auto-Sync",
                    badgeText = cloudSyncBadge,
                    badgeBg = cloudSyncBadgeBg,
                    badgeFg = cloudSyncBadgeFg,
                    iconBg = if (cloudSyncState.isConnected && cloudSyncState.autoSyncEnabled) {
                        onBg
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    iconTint = if (cloudSyncState.isConnected && cloudSyncState.autoSyncEnabled) {
                        onFg
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    iconGlowColor = if (cloudSyncState.isConnected && cloudSyncState.autoSyncEnabled) {
                        NeonEmerald
                    } else {
                        NeonCyan
                    },
                    onClick = { activeDetailSheet = SettingsDetailSheet.CLOUD_SYNC },
                    testTag = "settings_cloud_sync_item"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.SystemUpdate,
                    title = "Mobile App Auto-Update",
                    badgeText = updateBadge,
                    badgeBg = updateBadgeBg,
                    badgeFg = updateBadgeFg,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconGlowColor = NeonGold,
                    onClick = { activeDetailSheet = SettingsDetailSheet.APP_UPDATE },
                    testTag = "settings_app_update_item"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.RecordVoiceOver,
                    title = "Voice Summary Report",
                    badgeText = when (aiVoiceState.status) {
                        AiVoiceStatus.Playing -> "Playing"
                        AiVoiceStatus.ComposingScript,
                        AiVoiceStatus.GeneratingAudio -> "Preparing"
                        else -> "Voice"
                    },
                    badgeBg = if (aiVoiceState.isPlaying) onBg else MaterialTheme.colorScheme.primaryContainer,
                    badgeFg = if (aiVoiceState.isPlaying) onFg else MaterialTheme.colorScheme.primary,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconGlowColor = NeonPurple,
                    onClick = { activeDetailSheet = SettingsDetailSheet.VOICE_REPORT },
                    testTag = "settings_voice_report_item"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.CloudDone,
                    title = "Google Drive Backup & Sync",
                    badgeText = if (driveState.isSyncing) {
                        "Syncing"
                    } else if (driveState.isConnected) {
                        "Connected"
                    } else {
                        "Setup"
                    },
                    badgeBg = if (driveState.isConnected) onBg else MaterialTheme.colorScheme.surfaceVariant,
                    badgeFg = if (driveState.isConnected) onFg else MaterialTheme.colorScheme.onSurfaceVariant,
                    iconBg = if (driveState.isConnected) onBg else MaterialTheme.colorScheme.primaryContainer,
                    iconTint = if (driveState.isConnected) onFg else MaterialTheme.colorScheme.primary,
                    iconGlowColor = if (driveState.isConnected) NeonEmerald else NeonCyan,
                    onClick = { activeDetailSheet = SettingsDetailSheet.GOOGLE_DRIVE },
                    testTag = "settings_google_drive_item"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.Download,
                    title = stringResource(R.string.json_backup),
                    iconGlowColor = NeonCyan,
                    enabled = !isBusy,
                    onClick = onExportJson,
                    testTag = "export_json_button"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.FileUpload,
                    title = stringResource(R.string.restore_backup),
                    iconGlowColor = NeonGold,
                    enabled = !isBusy,
                    onClick = onRestoreJson,
                    testTag = "restore_json_button"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.Image,
                    title = stringResource(R.string.picture_export),
                    iconGlowColor = NeonEmerald,
                    enabled = !isBusy,
                    onClick = onExportPicture,
                    testTag = "settings_export_picture_button"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.Lock,
                    title = "Change Password",
                    iconGlowColor = NeonPurple,
                    onClick = onOpenChangePassword,
                    testTag = "change_password_button"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Log Out",
                    iconBg = offBg,
                    iconTint = offFg,
                    iconGlowColor = ExpenseCrimson,
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = onAppLogout,
                    testTag = "app_logout_button"
                )
            }
        }
    }
}

    activeDetailSheet?.let { sheet ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeDetailSheet = null },
            sheetState = sheetState,
            containerColor = CardSurfaceSolid
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                when (sheet) {
                    SettingsDetailSheet.CLOUD_SYNC -> {
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

                    SettingsDetailSheet.APP_UPDATE -> {
                        AppAutoUpdateCard(
                            updateState = appUpdateState,
                            onToggleAutoUpdate = onToggleAutoUpdate,
                            onCheckForUpdate = onCheckAppUpdate,
                            onCompleteUpdate = onCompleteAppUpdate,
                            onOpenUpdateLink = onOpenAppUpdateLink
                        )
                    }

                    SettingsDetailSheet.VOICE_REPORT -> {
                        VoiceSummaryReportPanel(
                            members = members,
                            isBusy = isBusy,
                            aiVoiceState = aiVoiceState,
                            onSelectAiVoiceOption = onSelectAiVoiceOption,
                            onSelectAiVoiceScope = onSelectAiVoiceScope,
                            onSelectAiVoiceMember = onSelectAiVoiceMember,
                            onSpeakAiVoiceReport = onSpeakAiVoiceReport,
                            onReplayAiVoiceReport = onReplayAiVoiceReport,
                            onStopAiVoiceReport = onStopAiVoiceReport
                        )
                    }

                    SettingsDetailSheet.GOOGLE_DRIVE -> {
                        GoogleDriveBackupPanel(
                            isBusy = isBusy,
                            driveState = driveState,
                            driveAuthBlockedEmail = driveAuthBlockedEmail,
                            onConnectGoogleDrive = onConnectGoogleDrive,
                            onLinkGoogleDriveFile = onLinkGoogleDriveFile,
                            onRestoreFromDriveFile = onRestoreFromDriveFile,
                            onShowDriveAccessHelp = onShowDriveAccessHelp,
                            onBackupToGoogleDrive = onBackupToGoogleDrive,
                            onRequestDriveRestore = { showDriveRestoreConfirm = true },
                            onToggleDriveAutoBackup = onToggleDriveAutoBackup,
                            onRequestDriveDisconnect = { showDriveDisconnectConfirm = true }
                        )
                    }
                }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceSummaryReportPanel(
    members: List<Member>,
    isBusy: Boolean,
    aiVoiceState: AiVoiceUiState,
    onSelectAiVoiceOption: (FemaleAiVoiceOption) -> Unit,
    onSelectAiVoiceScope: (AiVoiceScope) -> Unit,
    onSelectAiVoiceMember: (String) -> Unit,
    onSpeakAiVoiceReport: () -> Unit,
    onReplayAiVoiceReport: () -> Unit,
    onStopAiVoiceReport: () -> Unit
) {
    val onBg = IncomeSurface
    val onFg = IncomeEmerald
    val offBg = ExpenseSurface
    val offFg = ExpenseCrimson

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
                GlossyNeonIconOrb(
                    icon = Icons.Outlined.RecordVoiceOver,
                    contentDescription = null,
                    glowColor = NeonPurple,
                    iconTint = Color.White,
                    size = 42.dp,
                    iconSize = 22.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Voice Summary Report",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
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
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                    modifier = Modifier.testTag("ai_voice_option_${voice.name.lowercase()}")
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
                        .hisab3DButtonSurface(accentColor = ExpenseCrimson, cornerRadius = 12.dp)
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
                        .hisab3DButtonSurface(accentColor = NeonPurple, cornerRadius = 12.dp)
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
                        .hisab3DButtonSurface(accentColor = NeonCyan, cornerRadius = 12.dp)
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
                GlossyInnerMetricTile(
                    accentColor = NeonEmerald,
                    shape = RoundedCornerShape(10.dp),
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
                GlossyInnerMetricTile(
                    accentColor = ExpenseCrimson,
                    shape = RoundedCornerShape(10.dp),
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
            GlossyInnerMetricTile(
                accentColor = NeonPurple,
                shape = RoundedCornerShape(12.dp),
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

@Composable
private fun GoogleDriveBackupPanel(
    isBusy: Boolean,
    driveState: DriveUiState,
    driveAuthBlockedEmail: String?,
    onConnectGoogleDrive: () -> Unit,
    onLinkGoogleDriveFile: () -> Unit,
    onRestoreFromDriveFile: () -> Unit,
    onShowDriveAccessHelp: () -> Unit,
    onBackupToGoogleDrive: () -> Unit,
    onRequestDriveRestore: () -> Unit,
    onToggleDriveAutoBackup: (Boolean) -> Unit,
    onRequestDriveDisconnect: () -> Unit
) {
    val onBg = IncomeSurface
    val onFg = IncomeEmerald
    val offBg = ExpenseSurface
    val offFg = ExpenseCrimson

    SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Google Drive Backup & Sync",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
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
            GlossyInnerMetricTile(
                accentColor = NeonEmerald,
                shape = RoundedCornerShape(12.dp),
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
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Auto Google Drive Backup",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
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
                enabled = !isBusy && !driveState.isSyncing,
                onClick = onRequestDriveRestore,
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
                enabled = !isBusy && !driveState.isSyncing,
                onClick = onConnectGoogleDrive,
                testTag = "drive_switch_account_button"
            )
            SettingsListDivider()

            SettingsListOptionRow(
                icon = Icons.Outlined.LinkOff,
                title = "Disconnect Google Drive",
                enabled = !isBusy && !driveState.isSyncing,
                iconBg = offBg,
                iconTint = offFg,
                titleColor = MaterialTheme.colorScheme.error,
                onClick = onRequestDriveDisconnect,
                testTag = "drive_disconnect_button"
            )
        } else {
            if (!driveAuthBlockedEmail.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                GlossyInnerMetricTile(
                    accentColor = ExpenseCrimson,
                    shape = RoundedCornerShape(12.dp),
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

            Spacer(modifier = Modifier.height(6.dp))
            SettingsListOptionRow(
                icon = Icons.Outlined.CloudUpload,
                title = "Link Google Drive File (Any Gmail)",
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
                enabled = !isBusy && !driveState.isSyncing,
                onClick = onRestoreFromDriveFile,
                testTag = "drive_restore_saf_file_button"
            )
            SettingsListDivider()

            SettingsListOptionRow(
                icon = Icons.Outlined.CloudDone,
                title = "Connect Google Drive Account",
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

private fun formatBackupTime(timestampMs: Long): String {
    if (timestampMs <= 0L) return "-"
    val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
    return sdf.format(Date(timestampMs))
}

@Composable
private fun SettingsListDivider() {
    HorizontalDivider(
        thickness = 0.8.dp,
        color = Color.White.copy(alpha = 0.14f)
    )
}

@Composable
private fun SettingsListOptionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    testTag: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    badgeText: String? = null,
    badgeBg: Color = Color.White.copy(alpha = 0.14f),
    badgeFg: Color = MaterialTheme.colorScheme.onSurface,
    iconBg: Color = Color.White.copy(alpha = 0.10f),
    iconTint: Color = BrandDeep,
    iconGlowColor: Color = NeonCyan,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 5.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlossyNeonIconOrb(
            icon = icon,
            contentDescription = null,
            glowColor = iconGlowColor,
            iconTint = Color.White,
            size = 38.dp,
            iconSize = 18.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) titleColor else titleColor.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
