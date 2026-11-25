package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BanglaFormatters
import com.example.model.HisabTransaction
import com.example.model.Member
import com.example.model.MemberTakaJoma
import com.example.model.MonthlySalesRecord
import com.example.model.TakaJomaStatus
import com.example.model.TxType
import com.example.ui.LocalHisabViewModel
import com.example.ui.components.AdminMemberTakaJomaTableCard
import com.example.ui.components.HeroBalanceCard
import com.example.ui.components.MemberSalesDuesCard
import com.example.ui.components.SectionPanelCard
import com.example.ui.theme.ExpenseCrimsonDark
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.IncomeEmeraldDark
import com.example.ui.theme.IncomeSurface
import com.example.ui.theme.NagadAccent
import com.example.ui.theme.NagadSurface

@Composable
fun HomeScreen(
    transactions: List<HisabTransaction>,
    modifier: Modifier = Modifier,
    members: List<Member> = emptyList(),
    monthlySales: List<MonthlySalesRecord> = emptyList(),
    memberTakaJomaList: List<MemberTakaJoma> = emptyList(),
    onApproveMemberTakaJoma: (MemberTakaJoma) -> Unit = {},
    onRejectMemberTakaJoma: (MemberTakaJoma) -> Unit = {},
    onDeleteMemberTakaJoma: (MemberTakaJoma) -> Unit = {},
    onOpenMemberTakaJoma: (() -> Unit)? = null
) {
    val totalIncome = remember(transactions) {
        transactions.filter { it.resolvedType == TxType.INCOME }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.resolvedType == TxType.EXPENSE }.sumOf { it.amount }
    }
    val balance = totalIncome - totalExpense

    val todayIso = remember { BanglaFormatters.todayIso() }
    val todayIncome = remember(transactions, todayIso) {
        transactions.filter { it.date == todayIso && it.resolvedType == TxType.INCOME }.sumOf { it.amount }
    }
    val todayExpense = remember(transactions, todayIso) {
        transactions.filter { it.date == todayIso && it.resolvedType == TxType.EXPENSE }.sumOf { it.amount }
    }
    val todayApprovedMemberJoma = remember(transactions, todayIso) {
        transactions
            .filter {
                it.date == todayIso &&
                    it.resolvedType == TxType.INCOME &&
                    it.id.startsWith(LocalHisabViewModel.TAKA_JOMA_TX_PREFIX)
            }
            .sumOf { it.amount }
    }
    val pendingMemberJomaList = remember(memberTakaJomaList) {
        memberTakaJomaList.filter { it.resolvedStatus == TakaJomaStatus.PENDING }
    }
    val pendingMemberJomaTotal = remember(pendingMemberJomaList) {
        pendingMemberJomaList.sumOf { it.amount }
    }

    val incomeSurface = IncomeSurface
    val incomeText = IncomeEmeraldDark
    val expenseSurface = ExpenseSurface
    val expenseText = ExpenseCrimsonDark

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            HeroBalanceCard(
                balance = balance,
                totalIncome = totalIncome,
                totalExpense = totalExpense
            )
        }

        // Today's BDT (৳) Income & Expense summary card
        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Today's Summary (${BanglaFormatters.formatBanglaDate(todayIso)})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = incomeSurface
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Today's Income",
                                style = MaterialTheme.typography.labelSmall,
                                color = incomeText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = BanglaFormatters.formatBdt(todayIncome),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = incomeText
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = expenseSurface
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Today's Expense",
                                style = MaterialTheme.typography.labelSmall,
                                color = expenseText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = BanglaFormatters.formatBdt(todayExpense),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = expenseText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("today_approved_member_joma_banner"),
                    shape = RoundedCornerShape(12.dp),
                    color = incomeSurface.copy(alpha = 0.72f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Approved Member Deposits (Today)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = incomeText,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = BanglaFormatters.formatBdt(todayApprovedMemberJoma),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = incomeText
                        )
                    }
                }

                if (pendingMemberJomaList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("today_pending_member_joma_banner"),
                        shape = RoundedCornerShape(12.dp),
                        color = NagadSurface.copy(alpha = 0.85f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pending (${pendingMemberJomaList.size} awaiting approval)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = NagadAccent,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = BanglaFormatters.formatBdt(pendingMemberJomaTotal),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = NagadAccent
                            )
                        }
                    }
                }
            }
        }

        // Member Taka Joma Table (pending submissions only — disappears once Admin approves)
        item {
            AdminMemberTakaJomaTableCard(
                submissions = memberTakaJomaList,
                onDeleteSubmission = onDeleteMemberTakaJoma,
                onApproveSubmission = onApproveMemberTakaJoma,
                onRejectSubmission = onRejectMemberTakaJoma,
                onOpenForm = onOpenMemberTakaJoma,
                pendingOnlyMode = true
            )
        }

        // Member Sales & Dues Summary (Dues decreases when Taka Joma is approved)
        item {
            MemberSalesDuesCard(
                members = members,
                monthlySales = monthlySales,
                submissions = memberTakaJomaList
            )
        }
    }
}
