package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import com.example.model.BanglaFormatters
import com.example.ui.components.SectionPanelCard
import com.example.ui.theme.DarkExpenseSurface
import com.example.ui.theme.DarkExpenseText
import com.example.ui.theme.DarkIncomeSurface
import com.example.ui.theme.DarkIncomeText
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface

@Composable
fun SettingsScreen(
    appLoginId: String,
    memberCount: Int,
    activeMemberCount: Int,
    isOnlinePaymentEnabled: Boolean,
    takaJomaCount: Int = 0,
    memberAppDownloadUrl: String = "",
    transactionCount: Int,
    isBusy: Boolean,
    onOpenMemberList: () -> Unit,
    onOpenMemberTakaJoma: () -> Unit = {},
    onOpenOnlinePayment: () -> Unit,
    onOpenMemberAppLink: () -> Unit = {},
    onOpenChangePassword: () -> Unit,
    onAppLogout: () -> Unit,
    onExportJson: () -> Unit,
    onRestoreJson: () -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onBg = IncomeSurface
    val onFg = IncomeEmerald
    val offBg = ExpenseSurface
    val offFg = ExpenseCrimson

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
                    subtitle = "নাম, কোড, ডিপো, ছবি ও জমার তালিকা",
                    badgeText = BanglaFormatters.toBanglaDigits(takaJomaCount.toString()),
                    badgeBg = onBg,
                    badgeFg = onFg,
                    onClick = onOpenMemberTakaJoma,
                    testTag = "settings_member_taka_joma_item"
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
                    subtitle = if (memberAppDownloadUrl.isNotBlank()) {
                        memberAppDownloadUrl
                    } else {
                        "লগইন স্ক্রিনের নিচে কাস্টম ডাউনলোড লিঙ্ক বা APK সেট করুন"
                    },
                    badgeText = if (memberAppDownloadUrl.isNotBlank()) "সেট করা" else "APK",
                    badgeBg = MaterialTheme.colorScheme.primaryContainer,
                    badgeFg = MaterialTheme.colorScheme.primary,
                    onClick = onOpenMemberAppLink,
                    testTag = "settings_member_app_link_item"
                )
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
                    subtitle = "সব হিসাব JSON ফাইলে সংরক্ষণ করুন",
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
                    icon = Icons.Outlined.TableChart,
                    title = stringResource(R.string.csv_export),
                    subtitle = "CSV স্প্রেডশিট রিপোর্ট ডাউনলোড করুন",
                    enabled = !isBusy,
                    onClick = onExportCsv,
                    testTag = "settings_export_csv_button"
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
