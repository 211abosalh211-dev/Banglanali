package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.currency.Money
import com.example.data.local.entity.accounting.AccountEntity
import com.example.data.local.entity.cashbox.CashboxEntity
import com.example.ui.viewmodel.ErpMasterViewModel
import com.example.ui.viewmodel.ErpUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingCashboxView(
    state: ErpUiState,
    viewModel: ErpMasterViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showOpenShiftDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("الصناديق والورديات") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("سندات القبض والصرف") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("دليل الحسابات المالي") })
            }

            when (selectedTab) {
                0 -> CashboxesSection(
                    cashboxes = state.cashboxes,
                    onOpenShift = { showOpenShiftDialog = true },
                    onTransfer = { showTransferDialog = true }
                )
                1 -> VouchersSection(
                    onNewReceipt = { showReceiptDialog = true },
                    onNewPayment = { showPaymentDialog = true }
                )
                2 -> ChartOfAccountsSection(accounts = state.accounts)
            }
        }
    }

    if (showReceiptDialog) {
        ReceiptVoucherDialog(
            cashboxes = state.cashboxes,
            revenueAccounts = state.accounts.filter { it.accountType == "REVENUE" || it.accountCode.startsWith("103") },
            onDismiss = { showReceiptDialog = false },
            onConfirm = { from, amount, boxId, accId, notes ->
                viewModel.createReceipt(from, amount, boxId, accId, notes)
                showReceiptDialog = false
            }
        )
    }

    if (showPaymentDialog) {
        PaymentVoucherDialog(
            cashboxes = state.cashboxes,
            expenseAccounts = state.accounts.filter { it.accountType == "EXPENSE" || it.accountCode.startsWith("201") },
            onDismiss = { showPaymentDialog = false },
            onConfirm = { to, amount, boxId, accId, notes ->
                viewModel.createPayment(to, amount, boxId, accId, notes)
                showPaymentDialog = false
            }
        )
    }

    if (showTransferDialog) {
        CashboxTransferDialog(
            cashboxes = state.cashboxes,
            onDismiss = { showTransferDialog = false },
            onConfirm = { fromId, toId, amount, notes ->
                viewModel.transferCash(fromId, toId, amount, notes)
                showTransferDialog = false
            }
        )
    }

    if (showOpenShiftDialog) {
        OpenShiftDialog(
            cashboxes = state.cashboxes,
            onDismiss = { showOpenShiftDialog = false },
            onConfirm = { boxId, bal ->
                viewModel.openShift(boxId, bal)
                showOpenShiftDialog = false
            }
        )
    }
}

@Composable
fun CashboxesSection(
    cashboxes: List<CashboxEntity>,
    onOpenShift: () -> Unit,
    onTransfer: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onOpenShift, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.LockOpen, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("فتح وردية")
            }
            FilledTonalButton(onClick = onTransfer, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("تحويل بين الصناديق")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(cashboxes) { box ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = box.nameAr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = box.cashboxCode,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "الرصيد الفعلي: ${Money.fromMinor(box.currentBalanceMinor).formatted}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VouchersSection(
    onNewReceipt: () -> Unit,
    onNewPayment: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("سند قبض نقدي", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Text("إثبات إيراد أو سداد نزيل مع أثر محاسبي مباشر", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onNewReceipt,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("إصدار سند قبض")
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("سند صرف نقدي", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    Text("تسجيل مصروفات أو دفعات موردين من الصندوق", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onNewPayment,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                    ) {
                        Text("إصدار سند صرف")
                    }
                }
            }
        }
    }
}

@Composable
fun ChartOfAccountsSection(accounts: List<AccountEntity>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(accounts) { acc ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${acc.accountCode} - ${acc.accountNameAr}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "النوع: ${acc.accountType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        text = Money.fromMinor(acc.currentBalanceMinor).formatted,
                        fontWeight = FontWeight.Bold,
                        color = if (acc.currentBalanceMinor >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiptVoucherDialog(
    cashboxes: List<CashboxEntity>,
    revenueAccounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Long, Long, String) -> Unit
) {
    var from by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("25000") }
    var notes by remember { mutableStateOf("دفعة نقدية") }
    val defaultBoxId = cashboxes.firstOrNull()?.id ?: 1L
    val defaultAccId = revenueAccounts.firstOrNull()?.id ?: 8L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إصدار سند قبض نقدي") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = from, onValueChange = { from = it }, label = { Text("المستلم منه (الاسم)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("المبلغ (ريال)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("البيان / الملاحظات") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountMinor = (amountText.toDoubleOrNull() ?: 0.0).toLong() * 100
                    if (from.isNotBlank() && amountMinor > 0) {
                        onConfirm(from, amountMinor, defaultBoxId, defaultAccId, notes)
                    }
                }
            ) { Text("ترحيل وإصدار") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun PaymentVoucherDialog(
    cashboxes: List<CashboxEntity>,
    expenseAccounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Long, Long, String) -> Unit
) {
    var to by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("5000") }
    var notes by remember { mutableStateOf("مصروفات صيانة ونظافة") }
    val defaultBoxId = cashboxes.firstOrNull()?.id ?: 1L
    val defaultAccId = expenseAccounts.firstOrNull()?.id ?: 10L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إصدار سند صرف نقدي") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = to, onValueChange = { to = it }, label = { Text("يصرف إلى (المستفيد)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("المبلغ (ريال)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("البيان / السبب") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountMinor = (amountText.toDoubleOrNull() ?: 0.0).toLong() * 100
                    if (to.isNotBlank() && amountMinor > 0) {
                        onConfirm(to, amountMinor, defaultBoxId, defaultAccId, notes)
                    }
                }
            ) { Text("ترحيل وصرف") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun CashboxTransferDialog(
    cashboxes: List<CashboxEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, Long, String) -> Unit
) {
    var amountText by remember { mutableStateOf("10000") }
    var notes by remember { mutableStateOf("تحويل نقدية دوري") }
    val firstId = cashboxes.firstOrNull()?.id ?: 1L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحويل بين الصناديق") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("المصدر: الصندوق الرئيسي")
                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("المبلغ المحول (ريال)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("البيان") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountMinor = (amountText.toDoubleOrNull() ?: 0.0).toLong() * 100
                    if (amountMinor > 0) {
                        onConfirm(firstId, firstId, amountMinor, notes)
                    }
                }
            ) { Text("تنفيذ التحويل") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun OpenShiftDialog(
    cashboxes: List<CashboxEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    var balText by remember { mutableStateOf("50000") }
    val boxId = cashboxes.firstOrNull()?.id ?: 1L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("فتح وردية جديدة (Shift)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("الصندوق: ${cashboxes.firstOrNull()?.nameAr ?: "الصندوق الرئيسي"}")
                OutlinedTextField(value = balText, onValueChange = { balText = it }, label = { Text("رصيد العهدة الافتتاحي (ريال)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val balMinor = (balText.toDoubleOrNull() ?: 0.0).toLong() * 100
                    onConfirm(boxId, balMinor)
                }
            ) { Text("فتح الوردية الآن") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
