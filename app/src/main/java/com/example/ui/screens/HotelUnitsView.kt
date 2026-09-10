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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.currency.Money
import com.example.data.local.entity.hotel.UnitEntity
import com.example.ui.viewmodel.ErpMasterViewModel
import com.example.ui.viewmodel.ErpUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelUnitsView(
    state: ErpUiState,
    viewModel: ErpMasterViewModel,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredUnits = remember(state.units, selectedFilter) {
        when (selectedFilter) {
            "ALL" -> state.units
            "AVAILABLE" -> state.units.filter { it.status == "AVAILABLE" || it.status == "VACANT_CLEAN" }
            "OCCUPIED" -> state.units.filter { it.status == "OCCUPIED" }
            "CLEANING" -> state.units.filter { it.status == "CLEANING" || it.status == "VACANT_DIRTY" }
            else -> state.units
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("fab_add_unit")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة غرفة جديدة")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("الكل (${state.units.size})") }
                )
                FilterChip(
                    selected = selectedFilter == "AVAILABLE",
                    onClick = { selectedFilter = "AVAILABLE" },
                    label = { Text("متاحة") }
                )
                FilterChip(
                    selected = selectedFilter == "OCCUPIED",
                    onClick = { selectedFilter = "OCCUPIED" },
                    label = { Text("مشغولة") }
                )
                FilterChip(
                    selected = selectedFilter == "CLEANING",
                    onClick = { selectedFilter = "CLEANING" },
                    label = { Text("نظافة") }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredUnits) { unit ->
                    UnitCard(
                        unit = unit,
                        onStatusChange = { newStatus ->
                            viewModel.setUnitStatus(unit.id, newStatus)
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddUnitDialog(
            unitTypes = state.unitTypes,
            onDismiss = { showAddDialog = false },
            onConfirm = { unitNumber, floor, typeId, price, desc ->
                viewModel.addUnit(unitNumber, floor, typeId, price, desc)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun UnitCard(
    unit: UnitEntity,
    onStatusChange: (String) -> Unit
) {
    val (statusText, statusColor) = when (unit.status) {
        "AVAILABLE", "VACANT_CLEAN" -> "متاحة للحجز" to Color(0xFF2E7D32)
        "OCCUPIED" -> "مشغولة بنزيل" to Color(0xFFC62828)
        "CLEANING", "VACANT_DIRTY" -> "تحت التنظيف والتجهيز" to Color(0xFFF57F17)
        "MAINTENANCE", "UNDER_MAINTENANCE" -> "تحت الصيانة" to Color(0xFF6A1B9A)
        "OUT_OF_ORDER" -> "خارج الخدمة" to Color.DarkGray
        else -> unit.status to Color.Gray
    }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = unit.unitNumber,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "غرفة رقم ${unit.unitNumber} (الدور ${unit.floorNumber})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val priceMinor = unit.customNightlyPriceMinor ?: 1500000L
                        Text(
                            text = Money.fromMinor(priceMinor).formatted + " / ليلة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action buttons for fast status toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onStatusChange("AVAILABLE") },
                    enabled = unit.status != "AVAILABLE",
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إتاحة")
                }
                OutlinedButton(
                    onClick = { onStatusChange("CLEANING") },
                    enabled = unit.status != "CLEANING",
                    modifier = Modifier.weight(1f)
                ) {
                    Text("تنظيف")
                }
                OutlinedButton(
                    onClick = { onStatusChange("MAINTENANCE") },
                    enabled = unit.status != "MAINTENANCE",
                    modifier = Modifier.weight(1f)
                ) {
                    Text("صيانة")
                }
            }
        }
    }
}

@Composable
fun AddUnitDialog(
    unitTypes: List<com.example.data.local.entity.hotel.UnitTypeEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Long, Long, String) -> Unit
) {
    var unitNumber by remember { mutableStateOf("") }
    var floorText by remember { mutableStateOf("1") }
    var priceText by remember { mutableStateOf("15000") }
    var desc by remember { mutableStateOf("") }
    val defaultTypeId = unitTypes.firstOrNull()?.id ?: 1L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة غرفة أو وحدة فندقية جديدة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = unitNumber,
                    onValueChange = { unitNumber = it },
                    label = { Text("رقم الغرفة (مثال: 103)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = floorText,
                    onValueChange = { floorText = it },
                    label = { Text("رقم الطابق") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("سعر الليلة (ريال)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("الوصف أو المميزات") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (unitNumber.isNotBlank()) {
                        val floor = floorText.toIntOrNull() ?: 1
                        val priceMinor = (priceText.toDoubleOrNull() ?: 15000.0 * 100).toLong() * 100
                        onConfirm(unitNumber, floor, defaultTypeId, priceMinor, desc)
                    }
                }
            ) {
                Text("حفظ الغرفة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
