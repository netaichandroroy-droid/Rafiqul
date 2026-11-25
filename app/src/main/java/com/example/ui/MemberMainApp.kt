package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AppLockCredentialStore
import com.example.data.CloudAutoSyncRepository
import com.example.data.CloudSyncPrefsStore
import com.example.data.LocalTransactionStore
import com.example.data.MemberStore
import com.example.data.MemberTakaJomaStore
import com.example.data.MonthlySalesStore
import com.example.data.OnlinePaymentStore
import com.example.model.Member
import com.example.model.TxType
import com.example.ui.components.AppFrostedGlassBackground
import com.example.ui.components.AppGlassSnackbarHost
import com.example.ui.components.GlassTopBarContainer
import com.example.ui.components.StylishBottomNavBar
import com.example.ui.components.StylishNavTab
import com.example.ui.screens.ChangePasswordDialog
import com.example.ui.screens.MemberAvatarView
import com.example.ui.screens.MemberHomeScreen
import com.example.ui.screens.MemberLendenScreen
import com.example.ui.screens.MemberProfileScreen
import com.example.ui.screens.MemberTakaJomaScreen
import com.example.ui.screens.OnlinePaymentScreen
import com.example.ui.screens.TransactionModalSheet

private enum class MemberTab(val label: String) {
    HOME("Home"),
    TRANSACTIONS("Ledger"),
    TAKA_JOMA("Deposit"),
    PAYMENT("Pay"),
    PROFILE("Profile")
}

@Composable
fun MemberMainApp(
    initialMember: Member,
    credentialStore: AppLockCredentialStore,
    onMemberLogout: () -> Unit,
    cloudSyncRepository: CloudAutoSyncRepository? = null,
    syncOwnerUid: String = "local"
) {
    val context = LocalContext.current
    val localStore = remember(context) { LocalTransactionStore(context) }
    val memberStore = remember(context) { MemberStore(context) }
    val onlinePaymentStore = remember(context, initialMember.id) {
        OnlinePaymentStore(context, ownerScope = "member_${initialMember.id}")
    }
    val takaJomaStore = remember(context) { MemberTakaJomaStore(context) }
    val monthlySalesStore = remember(context) { MonthlySalesStore(context) }
    val cloudSyncPrefs = remember(context) { CloudSyncPrefsStore(context) }
    val viewModel: LocalHisabViewModel = viewModel(
        key = "member_hisab_${syncOwnerUid}_${initialMember.id}",
        factory = viewModelFactory {
            initializer {
                LocalHisabViewModel(
                    localStore = localStore,
                    memberStore = memberStore,
                    onlinePaymentStore = onlinePaymentStore,
                    takaJomaStore = takaJomaStore,
                    monthlySalesStore = monthlySalesStore,
                    ownerUserId = "member_${initialMember.id}",
                    reservedAdminLoginId = credentialStore.getLoginId(),
                    cloudSyncRepository = cloudSyncRepository,
                    cloudSyncPrefs = cloudSyncPrefs
                )
            }
        }
    )

    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val onlinePaymentConfig by viewModel.onlinePaymentConfig.collectAsStateWithLifecycle()
    val takaJomaList by viewModel.takaJomaList.collectAsStateWithLifecycle()
    val monthlySales by viewModel.monthlySales.collectAsStateWithLifecycle()
    val controls by viewModel.controls.collectAsStateWithLifecycle()
    val cloudSyncState by viewModel.cloudSyncState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeMember by remember(initialMember) { mutableStateOf(initialMember) }
    var selectedTab by remember { mutableStateOf(MemberTab.HOME) }
    var showModal by remember { mutableStateOf(false) }
    var modalInitialType by remember { mutableStateOf(TxType.EXPENSE) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(activeMember.id, selectedTab, cloudSyncState.lastSyncedAtMs) {
        memberStore.findById(activeMember.id)?.let { latest ->
            if (latest != activeMember) {
                activeMember = latest
            }
        }
        viewModel.refreshSharedData()
    }

    LaunchedEffect(controls.snackbarMessage) {
        val msg = controls.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    if (selectedTab != MemberTab.HOME) {
        BackHandler {
            selectedTab = MemberTab.HOME
        }
    }

    val openNewModal: (TxType) -> Unit = { type ->
        modalInitialType = type
        showModal = true
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val useRail = maxWidth >= 600.dp

        AppFrostedGlassBackground()

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { AppGlassSnackbarHost(hostState = snackbarHostState) },
            topBar = {
                MemberTopAppBar(
                    member = activeMember,
                    onProfileClick = { selectedTab = MemberTab.PROFILE }
                )
            },
            bottomBar = {
                if (!useRail) {
                    StylishBottomNavBar(
                        items = MemberTab.entries.map { tab ->
                            val selected = selectedTab == tab
                            StylishNavTab(
                                id = tab.name,
                                label = tab.label,
                                icon = memberTabIcon(tab, selected),
                                selected = selected,
                                testTag = "member_nav_${tab.name.lowercase()}",
                                onClick = { selectedTab = tab }
                            )
                        }
                    )
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = Color(0xFF171024).copy(alpha = 0.80f)
                    ) {
                        MemberTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = memberTabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("member_rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (selectedTab) {
                        MemberTab.HOME -> MemberHomeScreen(
                            member = activeMember,
                            submissions = takaJomaList,
                            monthlySales = monthlySales,
                            selectedSalesMonth = controls.salesMonth,
                            onShiftSalesMonth = viewModel::shiftSalesMonth,
                            onOpenTakaJomaForm = { selectedTab = MemberTab.TAKA_JOMA }
                        )

                        MemberTab.TRANSACTIONS -> MemberLendenScreen(
                            submissions = takaJomaList,
                            transactions = transactions,
                            onOpenTakaJomaForm = { selectedTab = MemberTab.TAKA_JOMA },
                            onAddPersonalTx = openNewModal
                        )

                        MemberTab.TAKA_JOMA -> MemberTakaJomaScreen(
                            submissions = takaJomaList,
                            currentMember = activeMember,
                            isAdminMode = false,
                            onSubmit = viewModel::submitMemberTakaJoma,
                            onDeleteSubmission = { /* Members cannot delete */ }
                        )

                        MemberTab.PAYMENT -> OnlinePaymentScreen(
                            config = onlinePaymentConfig,
                            onToggleMasterEnabled = {},
                            onToggleAccountEnabled = { _, _ -> },
                            onUpdateAccount = { _, _, _, _ -> },
                            onRecordPaymentTransaction = viewModel::recordOnlinePaymentTransaction,
                            onShowMessage = viewModel::showMessage,
                            isAdminEditable = false
                        )

                        MemberTab.PROFILE -> MemberProfileScreen(
                            member = activeMember,
                            transactionCount = transactions.size,
                            onOpenChangePassword = { showChangePasswordDialog = true },
                            onLogout = onMemberLogout
                        )
                    }
                }
            }
        }
    }

    if (showModal) {
        TransactionModalSheet(
            editingTransaction = null,
            initialType = modalInitialType,
            isSaving = controls.isSaving,
            onDismiss = { showModal = false },
            onSave = { _, type, amount, category, note, date, account ->
                viewModel.saveTransaction(null, type, amount, category, note, date, account) { ok ->
                    if (ok) showModal = false
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            currentLoginId = activeMember.code.ifBlank { activeMember.name },
            allowLoginIdEdit = false,
            onDismiss = { showChangePasswordDialog = false },
            onChangePassword = { currentPw, newPw, _ ->
                val result = memberStore.changeMemberPassword(
                    memberId = activeMember.id,
                    currentPassword = currentPw,
                    newPassword = newPw
                )
                result.map { updated ->
                    activeMember = updated
                    viewModel.syncMemberToCloud(updated)
                }
            },
            onSuccess = {
                showChangePasswordDialog = false
                viewModel.showMessage("${activeMember.name} password updated successfully")
            }
        )
    }
}

@Composable
private fun MemberTopAppBar(
    member: Member,
    onProfileClick: () -> Unit
) {
    GlassTopBarContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .clickable(onClick = onProfileClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemberAvatarView(
                name = member.name,
                photoPath = member.profilePhotoPath,
                size = 34.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = member.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun memberTabIcon(tab: MemberTab, selected: Boolean): ImageVector = when (tab) {
    MemberTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    MemberTab.TRANSACTIONS -> if (selected) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet
    MemberTab.TAKA_JOMA -> if (selected) Icons.AutoMirrored.Filled.ReceiptLong else Icons.AutoMirrored.Outlined.ReceiptLong
    MemberTab.PAYMENT -> if (selected) Icons.Filled.Payments else Icons.Outlined.Payments
    MemberTab.PROFILE -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
}
