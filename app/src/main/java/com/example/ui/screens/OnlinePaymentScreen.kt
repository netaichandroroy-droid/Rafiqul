package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.OnlinePaymentAccount
import com.example.model.OnlinePaymentConfig
import com.example.model.PaymentChannel
import com.example.model.TxType
import com.example.ui.components.SectionPanelCard
import com.example.ui.theme.DarkIncomeSurface
import com.example.ui.theme.DarkIncomeText
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface

@Composable
fun OnlinePaymentScreen(
    config: OnlinePaymentConfig,
    onToggleMasterEnabled: (Boolean) -> Unit,
    onToggleAccountEnabled: (accountId: String, enabled: Boolean) -> Unit,
    onUpdateAccount: (accountId: String, accountNumber: String, accountType: String, isEnabled: Boolean) -> Unit,
    onRecordPaymentTransaction: (PaymentChannel, TxType, Double, String, (Boolean) -> Unit) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    isAdminEditable: Boolean = true
) {
    val context = LocalContext.current
    val activeBg = IncomeSurface
    val activeFg = IncomeEmerald

    var editingAccount by remember { mutableStateOf<OnlinePaymentAccount?>(null) }
    var selectedChannel by remember { mutableStateOf(PaymentChannel.BKASH) }
    var paymentTxType by remember { mutableStateOf(TxType.INCOME) }
    var paymentAmount by remember { mutableStateOf("") }
    var paymentNote by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    val displayedAccounts = remember(config, isAdminEditable) {
        if (isAdminEditable) {
            config.accounts
        } else {
            config.accounts.filter { it.isEnabled }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isAdminEditable) {
            item {
                SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Payments,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Online Payment",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (config.isOnlinePaymentEnabled) "Online payment is enabled" else "Online payment is disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.isOnlinePaymentEnabled,
                            onCheckedChange = onToggleMasterEnabled,
                            modifier = Modifier.testTag("online_payment_master_switch")
                        )
                    }
                }
            }
        }

        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Payment Methods",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isAdminEditable) {
                        "Tap any option to add or update its account number"
                    } else if (!config.isOnlinePaymentEnabled || displayedAccounts.isEmpty()) {
                        "No online payment methods are currently enabled"
                    } else {
                        "Tap the copy icon to copy a payment number"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                displayedAccounts.forEachIndexed { index, account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isAdminEditable) {
                                    Modifier.clickable { editingAccount = account }
                                } else {
                                    Modifier
                                }
                            )
                            .padding(vertical = 10.dp)
                            .testTag("payment_channel_row_${account.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (account.isEnabled) activeBg else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (account.channel == PaymentChannel.BANK) {
                                    Icons.Outlined.AccountBalance
                                } else {
                                    Icons.Outlined.Smartphone
                                },
                                contentDescription = null,
                                tint = if (account.isEnabled) activeFg else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = account.channel.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (account.isEnabled) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = activeBg
                                    ) {
                                        Text(
                                            text = "ON",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = activeFg,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (account.accountNumber.isNotBlank()) {
                                    "${account.accountNumber} • ${account.accountType}"
                                } else {
                                    "Number not set (${account.accountType})"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (account.accountNumber.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(
                                        ClipData.newPlainText(account.channel.title, account.accountNumber)
                                    )
                                    onShowMessage("${account.channel.title} number copied")
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (isAdminEditable) {
                            IconButton(
                                onClick = { editingAccount = account },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Switch(
                                checked = account.isEnabled,
                                onCheckedChange = { onToggleAccountEnabled(account.id, it) },
                                modifier = Modifier.testTag("payment_switch_${account.id}")
                            )
                        }
                    }

                    if (index < displayedAccounts.lastIndex) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }

        if (config.isOnlinePaymentEnabled) {
            item {
                SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Record Online Payment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentChannel.entries.forEach { channel ->
                            FilterChip(
                                selected = selectedChannel == channel,
                                onClick = { selectedChannel = channel },
                                label = { Text(channel.title) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = paymentTxType == TxType.INCOME,
                            onClick = { paymentTxType = TxType.INCOME },
                            label = { Text("Receive (Income)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = paymentTxType == TxType.EXPENSE,
                            onClick = { paymentTxType = TxType.EXPENSE },
                            label = { Text("Send (Expense)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = {
                            paymentAmount = it
                            formError = null
                        },
                        label = { Text("Amount (৳)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("online_payment_amount_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = paymentNote,
                        onValueChange = { paymentNote = it },
                        label = { Text("Reference / Note (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("online_payment_note_input")
                    )

                    if (!formError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val parsed = paymentAmount.trim().toDoubleOrNull()
                            if (parsed == null || parsed <= 0.0) {
                                formError = "Please enter a valid amount."
                                return@Button
                            }
                            onRecordPaymentTransaction(
                                selectedChannel,
                                paymentTxType,
                                parsed,
                                paymentNote
                            ) { ok ->
                                if (ok) {
                                    paymentAmount = ""
                                    paymentNote = ""
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("record_online_payment_button")
                    ) {
                        Text("Save Transaction")
                    }
                }
            }
        }
    }

    editingAccount?.let { account ->
        EditPaymentAccountDialog(
            account = account,
            onDismiss = { editingAccount = null },
            onSave = { number, accType, enabled ->
                onUpdateAccount(account.id, number, accType, enabled)
                editingAccount = null
            }
        )
    }
}

@Composable
private fun EditPaymentAccountDialog(
    account: OnlinePaymentAccount,
    onDismiss: () -> Unit,
    onSave: (accountNumber: String, accountType: String, isEnabled: Boolean) -> Unit
) {
    var number by remember(account) { mutableStateOf(account.accountNumber) }
    var accountType by remember(account) { mutableStateOf(account.accountType) }
    var isEnabled by remember(account) { mutableStateOf(account.isEnabled) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(account.channel.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = {
                        number = it
                        errorMessage = null
                    },
                    label = {
                        Text(
                            if (account.channel == PaymentChannel.BANK) {
                                "Bank Account Number"
                            } else {
                                "${account.channel.title} Number"
                            }
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = accountType,
                    onValueChange = { accountType = it },
                    label = {
                        Text(
                            if (account.channel == PaymentChannel.BANK) {
                                "Bank Name"
                            } else {
                                "Account Type (Personal / Agent / Merchant)"
                            }
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isEnabled && number.isBlank()) {
                        errorMessage = "Please enter an account number to enable this method."
                        return@Button
                    }
                    onSave(number, accountType, isEnabled)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
