package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CloudSyncUiState
import com.example.model.BanglaFormatters
import com.example.model.HisabTransaction
import com.example.model.Member
import com.example.model.MemberSalesDuesCalculator
import com.example.model.MemberTakaJoma
import com.example.model.MonthlySalesRecord
import com.example.model.TakaJomaStatus
import com.example.model.TxType
import com.example.ui.LocalHisabViewModel
import com.example.ui.components.AdminMemberTakaJomaTableCard
import com.example.ui.components.CleanHomeMetricTile
import com.example.ui.components.DepositsSalesNetBalanceCard
import com.example.ui.components.HeroBalanceCard
import com.example.ui.components.MemberSalesDuesCard
import com.example.ui.components.glossyMetallicSurface
import com.example.ui.components.rememberLiveTodayIso
import com.example.ui.theme.CardBorderSolid
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.ExpenseCrimsonDark
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.IncomeEmeraldDark
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald

@Composable
fun HomeScreen(
    transactions: List<HisabTransaction>,
    modifier: Modifier = Modifier,
    members: List<Member> = emptyList(),
    monthlySales: List<MonthlySalesRecord> = emptyList(),
    memberTakaJomaList: List<MemberTakaJoma> = emptyList(),
    cloudSyncState: CloudSyncUiState = CloudSyncUiState(),
    onConnectCloudSync: (() -> Unit)? = null,
    onApproveMemberTakaJoma: (MemberTakaJoma) -> Unit = {},
    onRejectMemberTakaJoma: (MemberTakaJoma) -> Unit = {},
    onDeleteMemberTakaJoma: (MemberTakaJoma) -> Unit = {},
    onOpenMemberTakaJoma: (() -> Unit)? = null
) {
    val depositsSalesSummary = remember(members, monthlySales, memberTakaJomaList) {
        MemberSalesDuesCalculator.calculateDepositsSalesNetSummary(
            members = members,
            monthlySales = monthlySales,
            submissions = memberTakaJomaList
        )
    }

    val totalIncome = remember(transactions) {
        transactions.filter { it.resolvedType == TxType.INCOME }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.resolvedType == TxType.EXPENSE }.sumOf { it.amount }
    }
    val balance = totalIncome - totalExpense

    val todayIso = rememberLiveTodayIso()
    val todayIncome = remember(transactions, todayIso) {
        transactions.filter { it.date == todayIso && it.resolvedType == TxType.INCOME }.sumOf { it.amount }
    }
    val todayExpense = remember(transactions, todayIso) {
        transactions.filter { it.date == todayIso && it.resolvedType == TxType.EXPENSE }.sumOf { it.amount }
    }

    val incomeText = IncomeEmeraldDark
    val expenseText = ExpenseCrimsonDark

    Box(
        modifier = modifier
            .fillMaxSize()
            .glossyMetallicSurface(
                shape = RoundedCornerShape(0.dp),
                accentColor = NeonCyan,
                secondaryGlow = NeonBlue,
                borderWidth = 1.dp,
                streakIntensity = 0.25f,
                isHero = true
            )
            .testTag("home_unified_card")
    ) {
        CompositionLocalProvider(LocalContentColor provides GlassTextPrimary) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    DepositsSalesNetBalanceCard(
                        summary = depositsSalesSummary,
                        cloudSyncState = cloudSyncState,
                        onConnectFirestore = onConnectCloudSync,
                        embeddedInOuterCard = true
                    )
                }

                item { HomeSectionDivider() }

                item {
                    HeroBalanceCard(
                        balance = balance,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        embeddedInOuterCard = true
                    )
                }

                item { HomeSectionDivider() }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = "Today's Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CleanHomeMetricTile(
                                modifier = Modifier.weight(1f),
                                accentColor = NeonEmerald,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(
                                        text = "Today's Income",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = incomeText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = BanglaFormatters.formatBdt(todayIncome),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = incomeText
                                    )
                                }
                            }

                            CleanHomeMetricTile(
                                modifier = Modifier.weight(1f),
                                accentColor = ExpenseCrimson,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(
                                        text = "Today's Expense",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = expenseText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = BanglaFormatters.formatBdt(todayExpense),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = expenseText
                                    )
                                }
                            }
                        }
                    }
                }

                item { HomeSectionDivider() }

                item {
                    AdminMemberTakaJomaTableCard(
                        submissions = memberTakaJomaList,
                        onDeleteSubmission = onDeleteMemberTakaJoma,
                        onApproveSubmission = onApproveMemberTakaJoma,
                        onRejectSubmission = onRejectMemberTakaJoma,
                        onOpenForm = onOpenMemberTakaJoma,
                        pendingOnlyMode = true,
                        embeddedInOuterCard = true
                    )
                }

                item { HomeSectionDivider() }

                item {
                    MemberSalesDuesCard(
                        members = members,
                        monthlySales = monthlySales,
                        submissions = memberTakaJomaList,
                        embeddedInOuterCard = true
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSectionDivider() {
    HorizontalDivider(
        thickness = 0.75.dp,
        color = CardBorderSolid.copy(alpha = 0.35f),
        modifier = Modifier.padding(horizontal = 14.dp)
    )
}
