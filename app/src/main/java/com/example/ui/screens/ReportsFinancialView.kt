package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import com.example.core.currency.Money
import com.example.ui.viewmodel.ErpMasterViewModel
import com.example.ui.viewmodel.ErpUiState

@Composable
fun ReportsFinancialView(
    state: ErpUiState,
    viewModel: ErpMasterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalAssets = state.accounts.filter { it.accountType == "ASSET" }.sumOf { it.currentBalanceMinor }
    val totalLiabilities = state.accounts.filter { it.accountType == "LIABILITY" }.sumOf { it.currentBalanceMinor }
    val totalEquity = state.accounts.filter { it.accountType == "EQUITY" }.sumOf { it.currentBalanceMinor }

    val totalRevenues = state.accounts.filter { it.accountType == "REVENUE" }.sumOf { it.currentBalanceMinor }
    val totalExpenses = state.accounts.filter { it.accountType == "EXPENSE" }.sumOf { it.currentBalanceMinor }
    val netProfit = totalRevenues - totalExpenses

    val totalCash = state.cashboxes.sumOf { it.currentBalanceMinor }
    val occupiedRooms = state.units.count { it.status == "OCCUPIED" }
    val occupancyRate = if (state.units.isNotEmpty()) (occupiedRooms.toDouble() / state.units.size * 100).toInt() else 0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hotel Key Metrics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("المؤشرات التشغيلية الفندقية (Hotel KPIs)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("نسبة إشغال الغرف:", style = MaterialTheme.typography.bodyMedium)
                            Text("$occupancyRate%", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text("الغرف المشغولة:", style = MaterialTheme.typography.bodyMedium)
                            Text("$occupiedRooms من ${state.units.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        }
                        Column {
                            Text("رصيد الخزينة المتاح:", style = MaterialTheme.typography.bodyMedium)
                            Text(Money.fromMinor(totalCash).formatted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }
        }

        // Income Statement Summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("قائمة الدخل والأرباح (Income Statement)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF2E7D32))
                    }
                    Divider(Modifier.padding(vertical = 10.dp))
                    FinancialRow("إجمالي الإيرادات الفندقية والمبيعات (+):", totalRevenues, Color(0xFF2E7D32))
                    FinancialRow("إجمالي المصروفات التشغيلية (-):", totalExpenses, Color(0xFFC62828))
                    Divider(Modifier.padding(vertical = 8.dp))
                    FinancialRow("صافي الربح التشغيلي للفندق (=):", netProfit, if (netProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828), isBold = true)
                }
            }
        }

        // Balance Sheet Summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المركز المالي والميزانية العمومية (Balance Sheet)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Divider(Modifier.padding(vertical = 10.dp))
                    FinancialRow("إجمالي الأصول والموجودات (Assets):", totalAssets, MaterialTheme.colorScheme.primary)
                    FinancialRow("إجمالي الالتزامات والخصوم (Liabilities):", totalLiabilities, MaterialTheme.colorScheme.error)
                    FinancialRow("حقوق الملكية ورأس المال (Equity):", totalEquity, Color(0xFF6A1B9A))
                    Divider(Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "المعادلة المحاسبية: الأصول = الالتزامات + حقوق الملكية (متطابقة تماماً)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Export & Print Actions
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.printFinancialReport(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("طباعة التقرير PDF")
                }
                FilledTonalButton(
                    onClick = { viewModel.printFinancialReport(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("تصدير ومشاركة PDF")
                }
            }
        }
    }
}

@Composable
fun FinancialRow(label: String, minorAmount: Long, color: Color, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = Money.fromMinor(minorAmount).formatted,
            color = color,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}
