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
import com.example.data.MemberAppLinkStore
import com.example.model.BanglaFormatters
import com.example.model.Member
import com.example.ui.AiVoiceUiState
import com.example.ui.DriveUiState
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
    onConnectGoogleDrive: () -> Unit = {},
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
                    title = "Member List (মেম্বার লিস্ট)",
                    subtitle = "মোট ${BanglaFormatters.toBanglaDigits(memberCount.toString())} জন • সক্রিয় ${BanglaFormatters.toBanglaDigits(activeMemberCount.toString())} জন",
                    badgeText = BanglaFormatters.toBanglaDigits(memberCount.toString()),
                    badgeBg = MaterialTheme.colorScheme.primaryContainer,
                    badgeFg = MaterialTheme.colorScheme.primary,
                    onClick = onOpenMemberList,
                    testTag = "settings_member_list_item"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "মেম্বার টাকা জমা ফর্ম",
                    subtitle = "নাম, কোড, ডিপো ও জমার তালিকা",
                    badgeText = BanglaFormatters.toBanglaDigits(takaJomaCount.toString()),
                    badgeBg = onBg,
                    badgeFg = onFg,
                    onClick = onOpenMemberTakaJoma,
                    testTag = "settings_member_taka_joma_item"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.TrendingUp,
                    title = "Sales Upload (মাস অনুযায়ী)",
                    subtitle = "মাসভিত্তিক code, name, TP sales ও TP+Vat আপলোড",
                    badgeText = BanglaFormatters.toBanglaDigits(salesCount.toString()),
                    badgeBg = MaterialTheme.colorScheme.primaryContainer,
                    badgeFg = MaterialTheme.colorScheme.primary,
                    onClick = onOpenSalesUpload,
                    testTag = "settings_sales_upload_item"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.Payments,
                    title = "Online Payment (অনলাইন পেমেন্ট)",
                    subtitle = "bKash, Nagad, Rocket ও ব্যাংক পেমেন্ট সেটিংস",
                    badgeText = if (isOnlinePaymentEnabled) "চালু" else "বন্ধ",
                    badgeBg = if (isOnlinePaymentEnabled) onBg else offBg,
                    badgeFg = if (isOnlinePaymentEnabled) onFg else offFg,
                    onClick = onOpenOnlinePayment,
                    testTag = "settings_online_payment_item"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.PhoneAndroid,
                    title = "মেম্বার মোবাইল অ্যাপ লিঙ্ক",
                    subtitle = memberAppDownloadUrl.ifBlank { MemberAppLinkStore.DEFAULT_MEMBER_APP_URL },
                    badgeText = if (memberAppDownloadUrl.isNotBlank()) "কাস্টম" else "লিংক রেডি",
                    badgeBg = MaterialTheme.colorScheme.primaryContainer,
                    badgeFg = MaterialTheme.colorScheme.primary,
                    onClick = onOpenMemberAppLink,
                    testTag = "settings_member_app_link_item"
                )
            }
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
                                text = "বাংলা AI ভয়েস (নারী কণ্ঠ)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "মেম্বারদের সেলস, জমা টাকা ও বাকি (Dues) মিষ্টি নারী কণ্ঠে বাংলায় শুনুন",
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
                                AiVoiceStatus.Playing -> "চলছে"
                                AiVoiceStatus.ComposingScript,
                                AiVoiceStatus.GeneratingAudio -> "প্রস্তুত হচ্ছে"
                                else -> "AI Voice"
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
                    text = "নারী কণ্ঠ নির্বাচন:",
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
                    text = "কোন হিসাব শুনবেন:",
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
                        text = "মেম্বার বাছাই করুন:",
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
                            Text("ভয়েস থামান", fontWeight = FontWeight.Bold)
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
                                        "রিপোর্ট লেখা হচ্ছে..."
                                    } else {
                                        "ভয়েস তৈরি হচ্ছে..."
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
                                Text("বাংলায় হিসাব শুনুন", fontWeight = FontWeight.Bold)
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
                                contentDescription = "আবার শুনুন",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("আবার")
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
                                    text = "${aiVoiceState.selectedVoice.bnLabel}-এ হিসাব শোনানো হচ্ছে...",
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
                                text = "AI ভয়েস বাংলা বিবরণ:",
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
                            text = "Google Drive ব্যাকআপ ও সিঙ্ক",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "লেনদেন, মেম্বার, টাকা জমা ও সেলসের সব ডেটা Google Drive-এ জমা থাকবে",
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
                                text = if (driveState.isConnected) "সংযুক্ত" else "সংযুক্ত নয়",
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
                            Text(
                                text = if (driveState.connectedEmail.isNotBlank()) {
                                    "Google অ্যাকাউন্ট: ${driveState.connectedEmail}"
                                } else {
                                    "Google Drive অ্যাকাউন্ট সংযুক্ত আছে"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = onFg
                            )
                            if (driveState.lastBackupAtMs > 0L) {
                                Text(
                                    text = "শেষ ব্যাকআপ: ${formatBackupTime(driveState.lastBackupAtMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onFg
                                )
                            } else {
                                Text(
                                    text = "এখনো Google Drive-এ ব্যাকআপ জমা হয়নি",
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
                                text = "স্বয়ংক্রিয় Google Drive ব্যাকআপ",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ডেটা পরিবর্তন হলেই নিজে থেকে ড্রাইভে সেভ হবে",
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
                        title = "সব ডেটা Google Drive-এ জমা দিন",
                        subtitle = "লেনদেন, মেম্বার, টাকা জমা, সেলস ও পেমেন্ট ব্যাকআপ",
                        enabled = !isBusy && !driveState.isSyncing,
                        iconBg = onBg,
                        iconTint = onFg,
                        onClick = onBackupToGoogleDrive,
                        testTag = "drive_backup_now_button"
                    )
                    SettingsListDivider()

                    SettingsListOptionRow(
                        icon = Icons.Outlined.CloudDownload,
                        title = "Google Drive থেকে রিস্টোর করুন",
                        subtitle = "ড্রাইভে সংরক্ষিত সব ডেটা অ্যাপে ফিরিয়ে আনুন",
                        enabled = !isBusy && !driveState.isSyncing,
                        onClick = { showDriveRestoreConfirm = true },
                        testTag = "drive_restore_button"
                    )
                    SettingsListDivider()

                    SettingsListOptionRow(
                        icon = Icons.Outlined.Sync,
                        title = "Google অ্যাকাউন্ট পরিবর্তন করুন",
                        subtitle = "অন্য Gmail বা Drive অ্যাকাউন্ট নির্বাচন করুন",
                        enabled = !isBusy && !driveState.isSyncing,
                        onClick = onConnectGoogleDrive,
                        testTag = "drive_switch_account_button"
                    )
                    SettingsListDivider()

                    SettingsListOptionRow(
                        icon = Icons.Outlined.LinkOff,
                        title = "Google Drive সংযোগ বিচ্ছিন্ন করুন",
                        subtitle = "এই ডিভাইস থেকে ড্রাইভ লিঙ্ক বন্ধ করুন",
                        enabled = !isBusy && !driveState.isSyncing,
                        iconBg = offBg,
                        iconTint = offFg,
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { showDriveDisconnectConfirm = true },
                        testTag = "drive_disconnect_button"
                    )
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingsListOptionRow(
                        icon = Icons.Outlined.CloudDone,
                        title = "Google Drive যোগ করুন",
                        subtitle = "সব ডেটা নিরাপদে গুগল ড্রাইভে সংরক্ষণ করতে সংযুক্ত করুন",
                        badgeText = "যোগ করুন",
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
                    text = "ব্যাকআপ ও রিপোর্ট",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "মোট ${BanglaFormatters.toBanglaDigits(transactionCount.toString())}টি লেনদেন সংরক্ষিত",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                SettingsListOptionRow(
                    icon = Icons.Outlined.Download,
                    title = stringResource(R.string.json_backup),
                    subtitle = "সব হিসাব, মেম্বার ও সেলস ডেটা JSON ফাইলে সংরক্ষণ করুন",
                    enabled = !isBusy,
                    onClick = onExportJson,
                    testTag = "export_json_button"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.FileUpload,
                    title = stringResource(R.string.restore_backup),
                    subtitle = "JSON ব্যাকআপ ফাইল থেকে ডেটা ফিরিয়ে আনুন",
                    enabled = !isBusy,
                    onClick = onRestoreJson,
                    testTag = "restore_json_button"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.Outlined.Image,
                    title = stringResource(R.string.picture_export),
                    subtitle = "হিসাব রিপোর্ট ছবি (PNG) হিসেবে ডাউনলোড করুন",
                    enabled = !isBusy,
                    onClick = onExportPicture,
                    testTag = "settings_export_picture_button"
                )
            }
        }

        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "অ্যাকাউন্ট ও নিরাপত্তা",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                SettingsListOptionRow(
                    icon = Icons.Outlined.Lock,
                    title = "পাসওয়ার্ড পরিবর্তন",
                    subtitle = "Login ID: $appLoginId",
                    onClick = onOpenChangePassword,
                    testTag = "change_password_button"
                )
                SettingsListDivider()

                SettingsListOptionRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "লগআউট",
                    subtitle = "অ্যাপ সেশন লক করে লগইন স্ক্রিনে ফিরুন",
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
            title = { Text("Google Drive থেকে রিস্টোর?") },
            text = {
                Text("Google Drive-এ সংরক্ষিত ব্যাকআপ থেকে আপনার সব লেনদেন, মেম্বার, টাকা জমা ও সেলস তথ্য ফিরিয়ে আনা হবে।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDriveRestoreConfirm = false
                        onRestoreFromGoogleDrive()
                    },
                    modifier = Modifier.testTag("confirm_drive_restore_button")
                ) {
                    Text("রিস্টোর করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDriveRestoreConfirm = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (showDriveDisconnectConfirm) {
        AlertDialog(
            onDismissRequest = { showDriveDisconnectConfirm = false },
            title = { Text("Google Drive বিচ্ছিন্ন করবেন?") },
            text = {
                Text("Google Drive সংযোগ বিচ্ছিন্ন করলে স্বয়ংক্রিয় ব্যাকআপ বন্ধ হবে, তবে ড্রাইভে থাকা ফাইলটি মুছবে না।")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDriveDisconnectConfirm = false
                        onDisconnectGoogleDrive()
                    },
                    modifier = Modifier.testTag("confirm_drive_disconnect_button")
                ) {
                    Text("বিচ্ছিন্ন করুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDriveDisconnectConfirm = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

private fun formatBackupTime(timestampMs: Long): String {
    if (timestampMs <= 0L) return "-"
    val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale("bn", "BD"))
    return BanglaFormatters.toBanglaDigits(sdf.format(Date(timestampMs)))
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
