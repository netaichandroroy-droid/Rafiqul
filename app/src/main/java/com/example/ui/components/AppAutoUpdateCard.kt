package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppUpdateStatus
import com.example.data.AppUpdateUiState
import com.example.ui.theme.CardBorderSolid
import com.example.ui.theme.CardInnerTileSolid
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import com.example.ui.theme.HeroCardBorderSolid
import com.example.ui.theme.HeroCardSolid
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface
import com.example.ui.theme.NagadAccent
import com.example.ui.theme.NagadSurface
import com.example.ui.theme.NeonCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppAutoUpdateCard(
    updateState: AppUpdateUiState,
    onToggleAutoUpdate: (Boolean) -> Unit,
    onCheckForUpdate: () -> Unit,
    onCompleteUpdate: () -> Unit,
    onOpenUpdateLink: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    embeddedInOuterCard: Boolean = false
) {
    val canDownloadGitHubUpdate = updateState.status == AppUpdateStatus.UPDATE_AVAILABLE &&
        !updateState.downloadUrl.isNullOrBlank()

    val badgeBg = when (updateState.status) {
        AppUpdateStatus.DOWNLOADED,
        AppUpdateStatus.UP_TO_DATE -> IncomeSurface
        AppUpdateStatus.UPDATE_AVAILABLE,
        AppUpdateStatus.DOWNLOADING -> NagadSurface
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val badgeFg = when (updateState.status) {
        AppUpdateStatus.DOWNLOADED,
        AppUpdateStatus.UP_TO_DATE -> IncomeEmerald
        AppUpdateStatus.UPDATE_AVAILABLE,
        AppUpdateStatus.DOWNLOADING -> NagadAccent
        else -> MaterialTheme.colorScheme.primary
    }
    val badgeText = when (updateState.status) {
        AppUpdateStatus.CHECKING -> "Checking"
        AppUpdateStatus.UP_TO_DATE -> "Latest"
        AppUpdateStatus.UPDATE_AVAILABLE -> updateState.availableVersionName
            ?.let { if (it.startsWith("v", ignoreCase = true)) it else "v$it" }
            ?: "Update Ready"
        AppUpdateStatus.DOWNLOADING -> "${updateState.downloadProgressPercent}%"
        AppUpdateStatus.DOWNLOADED -> "Restart App"
        AppUpdateStatus.PLAY_UNAVAILABLE,
        AppUpdateStatus.IDLE -> "v${updateState.currentVersionName}"
        AppUpdateStatus.ERROR -> "Retry"
    }

    val cardContent: @Composable () -> Unit = {
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
                        imageVector = Icons.Outlined.SystemUpdate,
                        contentDescription = "App Auto-Update",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Mobile App Auto-Update",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GlassTextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (updateState.status == AppUpdateStatus.CHECKING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (updateState.statusMessage.isNotBlank() || updateState.lastCheckedAtMs > 0L) {
            Spacer(modifier = Modifier.height(10.dp))
            CleanHomeMetricTile(
                accentColor = if (canDownloadGitHubUpdate) IncomeEmerald else NeonCyan,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (updateState.statusMessage.isNotBlank()) {
                        Text(
                            text = updateState.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassTextPrimary
                        )
                    }
                    if (updateState.releaseNotes.isNotBlank() &&
                        updateState.status == AppUpdateStatus.UPDATE_AVAILABLE
                    ) {
                        Text(
                            text = "Latest change: ${updateState.releaseNotes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlassTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (updateState.lastCheckedAtMs > 0L) {
                        Text(
                            text = "Last checked: ${formatUpdateCheckedTime(updateState.lastCheckedAtMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlassTextSecondary
                        )
                    }
                    if (updateState.status == AppUpdateStatus.DOWNLOADING) {
                        LinearProgressIndicator(
                            progress = { (updateState.downloadProgressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        )
                    }
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
                text = "Auto Check & Install Updates",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = GlassTextPrimary,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = updateState.autoUpdateEnabled,
                onCheckedChange = onToggleAutoUpdate,
                modifier = Modifier.testTag("app_auto_update_switch")
            )
        }

        HorizontalDivider(
            thickness = 0.75.dp,
            color = CardBorderSolid.copy(alpha = 0.35f)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                updateState.status == AppUpdateStatus.DOWNLOADED -> {
                    Button(
                        onClick = onCompleteUpdate,
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .hisab3DButtonSurface(accentColor = IncomeEmerald, cornerRadius = 12.dp)
                            .testTag("complete_app_update_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restart & Install", fontWeight = FontWeight.Bold)
                    }
                }

                canDownloadGitHubUpdate -> {
                    Button(
                        onClick = onCompleteUpdate,
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .hisab3DButtonSurface(accentColor = IncomeEmerald, cornerRadius = 12.dp)
                            .testTag("download_github_update_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download Update", fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onCheckForUpdate,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("check_app_update_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Re-check for updates",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                else -> {
                    Button(
                        onClick = onCheckForUpdate,
                        enabled = updateState.status != AppUpdateStatus.CHECKING,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .hisab3DButtonSurface(accentColor = NeonCyan, cornerRadius = 12.dp)
                            .testTag("check_app_update_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (updateState.status == AppUpdateStatus.CHECKING) {
                                "Checking..."
                            } else {
                                "Check for Update"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (onOpenUpdateLink != null) {
                OutlinedButton(
                    onClick = onOpenUpdateLink,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("open_app_update_link_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = "Open GitHub Releases",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Releases", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (embeddedInOuterCard) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .testTag("app_auto_update_card")
        ) {
            cardContent()
        }
    } else {
        SectionPanelCard(
            modifier = modifier
                .fillMaxWidth()
                .testTag("app_auto_update_card")
        ) {
            cardContent()
        }
    }
}

@Composable
fun AppUpdateReadyBanner(
    updateState: AppUpdateUiState,
    onRestartToInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasGitHubUpdate = updateState.status == AppUpdateStatus.UPDATE_AVAILABLE &&
        !updateState.downloadUrl.isNullOrBlank()
    val shouldShow = updateState.status == AppUpdateStatus.DOWNLOADED ||
        updateState.status == AppUpdateStatus.DOWNLOADING ||
        hasGitHubUpdate

    val updateKey = "${updateState.status}_${updateState.availableVersionCode}_${updateState.availableVersionName}"
    var dismissedKey by rememberSaveable { mutableStateOf("") }

    if (!shouldShow || dismissedKey == updateKey) {
        return
    }

    val titleText = when {
        updateState.status == AppUpdateStatus.DOWNLOADED -> "New App Update Ready"
        updateState.status == AppUpdateStatus.DOWNLOADING ->
            "Downloading App Update (${updateState.downloadProgressPercent}%)..."
        else -> {
            val ver = updateState.availableVersionName?.let {
                if (it.startsWith("v", ignoreCase = true)) it else "v$it"
            } ?: "Update"
            "New GitHub Update ($ver)"
        }
    }

    val subtitleText = when {
        updateState.status == AppUpdateStatus.DOWNLOADED ->
            "Tap Restart to install the latest version."
        hasGitHubUpdate ->
            updateState.releaseNotes.ifBlank { "Tap Update to download & install from GitHub." }
        else -> ""
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("app_update_ready_banner"),
        shape = RoundedCornerShape(14.dp),
        color = HeroCardSolid,
        border = BorderStroke(1.2.dp, HeroCardBorderSolid),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = GlassTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (updateState.status == AppUpdateStatus.DOWNLOADING) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (updateState.downloadProgressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (subtitleText.isNotBlank()) {
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (updateState.status == AppUpdateStatus.DOWNLOADED || hasGitHubUpdate) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onRestartToInstall,
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeEmerald),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("banner_restart_update_button")
                ) {
                    Text(
                        text = if (updateState.status == AppUpdateStatus.DOWNLOADED) "Restart" else "Update",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (hasGitHubUpdate) {
                IconButton(
                    onClick = { dismissedKey = updateKey },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("banner_dismiss_update_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss update banner",
                        tint = GlassTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatUpdateCheckedTime(timestampMs: Long): String {
    if (timestampMs <= 0L) return "-"
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
    return sdf.format(Date(timestampMs))
}
