package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AccountType
import com.example.model.BanglaFormatters
import com.example.model.HisabCategories
import com.example.model.HisabTransaction
import com.example.model.TxType
import com.example.ui.components.TransactionFormFields

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionModalSheet(
    editingTransaction: HisabTransaction?,
    initialType: TxType,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        editingId: String?,
        type: TxType,
        amount: Double,
        category: String,
        note: String,
        date: String,
        account: AccountType
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember(editingTransaction, initialType) {
        mutableStateOf(editingTransaction?.resolvedType ?: initialType)
    }
    var amountText by remember(editingTransaction) {
        mutableStateOf(
            editingTransaction?.let {
                if (it.amount % 1.0 == 0.0) it.amount.toLong().toString() else it.amount.toString()
            } ?: ""
        )
    }
    var selectedCategory by remember(editingTransaction, initialType) {
        mutableStateOf(
            editingTransaction?.category?.takeIf { it.isNotBlank() }
                ?: HisabCategories.forType(editingTransaction?.resolvedType ?: initialType).first()
        )
    }
    var noteText by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.note ?: "")
    }
    var dateIso by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.date?.takeIf { it.isNotBlank() } ?: BanglaFormatters.todayIso())
    }
    var selectedAccount by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.resolvedAccount ?: AccountType.CASH)
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (editingTransaction == null) "নতুন হিসাব যোগ করুন" else "হিসাব সম্পাদনা",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            TransactionFormFields(
                selectedType = selectedType,
                onTypeChange = { txType ->
                    selectedType = txType
                    val list = HisabCategories.forType(txType)
                    if (selectedCategory !in list) {
                        selectedCategory = list.first()
                    }
                },
                amountText = amountText,
                onAmountChange = {
                    amountText = it
                    validationError = null
                },
                selectedCategory = selectedCategory,
                onCategoryChange = { selectedCategory = it },
                noteText = noteText,
                onNoteChange = { noteText = it },
                dateIso = dateIso,
                onDateChange = { dateIso = it },
                selectedAccount = selectedAccount,
                onAccountChange = { selectedAccount = it },
                validationError = validationError,
                isSaving = isSaving,
                submitLabel = stringResource(R.string.save_transaction),
                tagPrefix = "modal",
                onSubmit = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0 || amount > 1_000_000_000.0) {
                        validationError = "অনুগ্রহ করে সঠিক টাকার পরিমাণ লিখুন।"
                        return@TransactionFormFields
                    }
                    onSave(
                        editingTransaction?.id,
                        selectedType,
                        amount,
                        selectedCategory,
                        noteText,
                        dateIso,
                        selectedAccount
                    )
                }
            )
        }
    }
}
