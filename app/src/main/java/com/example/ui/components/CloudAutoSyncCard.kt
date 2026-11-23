package com.example.ui.components

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
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CloudSyncUiState
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudAutoSyncCard(
    cloudSyncState: CloudSyncUiState,
    onToggleAutoSync: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onCloudSignOut: () -> Unit,
    isSigningIn: Boolean = false,
    authError: String? = null,
    onCloudSignIn: () -> Unit = {},
    onSwitchCloudAccount: () -> Unit = onCloudSignOut,
    modifier: Modifier = Modifier
) {
    var showAccountMenuDialog by remember { mutableStateOf(false) }

    SectionPanelCard(modifier = modifier.fillMaxWidth()) {
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
                        .background(
                            if (cloudSyncState.isConnected && cloudSyncState.autoSyncEnabled) {
                                IncomeSurface
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            !cloudSyncState.isConnected -> Icons.Outlined.CloudOff
                            cloudSyncState.autoSyncEnabled -> Icons.Outlined.CloudSync
                            else -> Icons.Outlined.CloudDone
                        },
                        contentDescription = "Cloud Auto-Sync",
                        tint = if (cloudSyncState.isConnected && cloudSyncState.autoSyncEnabled) {
                            IncomeEmerald
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Google Account & Auto-Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Sign in with any Gmail to keep your records synced across devices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (cloudSyncState.isSyncing || isSigningIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        !cloudSyncState.isConnected -> MaterialTheme.colorScheme.surfaceVariant
                        cloudSyncState.autoSyncEnabled -> IncomeSurface
                        else -> ExpenseSurface
                    }
                ) {
                    Text(
                        text = when {
                            !cloudSyncState.isConnected -> "Offline"
                            cloudSyncState.autoSyncEnabled -> "Active"
                            else -> "Paused"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            !cloudSyncState.isConnected -> MaterialTheme.colorScheme.onSurfaceVariant
                            cloudSyncState.autoSyncEnabled -> IncomeEmerald
                            else -> ExpenseCrimson
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (cloudSyncState.isConnected) {
                IncomeSurface.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (cloudSyncState.accountEmail.isNotBlank()) {
                        "Google Account: ${cloudSyncState.accountEmail}"
                    } else if (cloudSyncState.isConnected) {
                        "Google account connected"
                    } else {
                        "No Google account connected"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (cloudSyncState.isConnected) {
                        IncomeEmerald
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (cloudSyncState.isConnected) {
                        if (cloudSyncState.lastSyncedAtMs > 0L) {
                            "Last synced: ${formatCloudSyncTime(cloudSyncState.lastSyncedAtMs)}"
                        } else {
                            "Ready to sync"
                        }
                    } else {
                        "Sign in with any Gmail account to turn on automatic cloud sync"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val displayError = authError?.takeIf { it.isNotBlank() } ?: cloudSyncState.errorMessage
        if (!displayError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ExpenseSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = displayError,
                    style = MaterialTheme.typography.bodySmall,
                    color = ExpenseCrimson,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (!cloudSyncState.isConnected) {
            Button(
                onClick = onCloudSignIn,
                enabled = !isSigningIn,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("cloud_google_sign_in_button")
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Signing in...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign in with Google",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Real-time Auto-Sync",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Automatically syncs every update to cloud storage",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = cloudSyncState.autoSyncEnabled,
                    onCheckedChange = onToggleAutoSync,
                    enabled = !cloudSyncState.isSyncing,
                    modifier = Modifier.testTag("cloud_auto_sync_switch")
                )
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSyncNow,
                    enabled = !cloudSyncState.isSyncing && !isSigningIn,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("cloud_sync_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sync Now",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = { showAccountMenuDialog = true },
                    enabled = !cloudSyncState.isSyncing && !isSigningIn,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("cloud_google_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SwitchAccount,
                        contentDescription = "Manage Google Account",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Account",
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }

    if (showAccountMenuDialog) {
        AlertDialog(
            onDismissRequest = { showAccountMenuDialog = false },
            title = { Text("Manage Google Account") },
            text = {
                Text("Switch to another Gmail account or sign out of the current Google account.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccountMenuDialog = false
                        onSwitchCloudAccount()
                    },
                    modifier = Modifier.testTag("confirm_cloud_switch_account_button")
                ) {
                    Text("Switch Gmail")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            showAccountMenuDialog = false
                            onCloudSignOut()
                        },
                        modifier = Modifier.testTag("confirm_cloud_sign_out_button")
                    ) {
                        Text("Sign Out", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showAccountMenuDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

private fun formatCloudSyncTime(timestampMs: Long): String {
    if (timestampMs <= 0L) return "-"
    val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
    return sdf.format(Date(timestampMs))
}
