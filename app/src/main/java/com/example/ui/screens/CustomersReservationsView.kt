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
import com.example.data.local.entity.customer.CustomerEntity
import com.example.data.local.entity.hotel.UnitEntity
import com.example.data.local.entity.reservation.ReservationEntity
import com.example.ui.viewmodel.ErpMasterViewModel
import com.example.ui.viewmodel.ErpUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersReservationsView(
    state: ErpUiState,
    viewModel: ErpMasterViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddReservationDialog by remember { mutableStateOf(false) }
    var showCheckInDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showAddReservationDialog = true
                    else if (selectedTab == 1) showCheckInDialog = true
                    else showAddCustomerDialog = true
                },
                modifier = Modifier.testTag("fab_action_booking")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة")
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("الحجوزات المؤكدة") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("النزلاء المقيمين (Check-in)") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("دليل العملاء") })
            }

            when (selectedTab) {
                0 -> ReservationsList(reservations = state.reservations, customers = state.customers, units = state.units)
                1 -> ActiveStaysList(units = state.units, cashboxes = state.cashboxes, onCheckOut = { stayId, boxId ->
                    viewModel.checkOutGuest(stayId, boxId, "CASH")
                })
                2 -> CustomersList(customers = state.customers, onDelete = { viewModel.deleteCustomer(it) })
            }
        }
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone, nid, nat ->
                viewModel.addCustomer(name, phone, nid, nat)
                showAddCustomerDialog = false
            }
        )
    }

    if (showAddReservationDialog) {
        AddReservationDialog(
            customers = state.customers,
            units = state.units.filter { it.status == "AVAILABLE" || it.status == "VACANT_CLEAN" },
            onDismiss = { showAddReservationDialog = false },
            onConfirm = { custId, unitId, checkIn, checkOut, total ->
                viewModel.createReservation(custId, unitId, checkIn, checkOut, total)
                showAddReservationDialog = false
            }
        )
    }

    if (showCheckInDialog) {
        QuickCheckInDialog(
            customers = state.customers,
            availableUnits = state.units.filter { it.status == "AVAILABLE" || it.status == "VACANT_CLEAN" },
            onDismiss = { showCheckInDialog = false },
            onConfirm = { custId, unitId, rate ->
                val oneDay = System.currentTimeMillis() + 86400000L
                viewModel.checkInGuest(null, custId, unitId, oneDay, rate)
                showCheckInDialog = false
            }
        )
    }
}

@Composable
fun ReservationsList(
    reservations: List<ReservationEntity>,
    customers: List<CustomerEntity>,
    units: List<UnitEntity>
) {
    if (reservations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد حجوزات مسجلة حالياً، اضغط + لإنشاء حجز")
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(reservations) { res ->
                val customer = customers.find { it.id == res.customerId }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "حجز رقم ${res.reservationNumber}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = res.status,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(text = "العميل: ${customer?.fullName ?: "غير معروف"}")
                        Text(
                            text = "الإجمالي: ${Money.fromMinor(res.totalAmountMinor).formatted}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveStaysList(
    units: List<UnitEntity>,
    cashboxes: List<com.example.data.local.entity.cashbox.CashboxEntity>,
    onCheckOut: (Long, Long) -> Unit
) {
    val occupiedUnits = units.filter { it.status == "OCCUPIED" }
    val defaultCashboxId = cashboxes.firstOrNull()?.id ?: 1L

    if (occupiedUnits.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد غرف مشغولة بنزلاء حالياً")
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(occupiedUnits) { unit ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "غرفة رقم ${unit.unitNumber}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val priceMinor = unit.customNightlyPriceMinor ?: 1500000L
                            Text(
                                text = "سعر الليلة: ${Money.fromMinor(priceMinor).formatted}",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = {
                                // Trigger check-out with simulated stay ID matching unit
                                onCheckOut(unit.id, defaultCashboxId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("تسجيل مغادرة (Check-out)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomersList(
    customers: List<CustomerEntity>,
    onDelete: (Long) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(customers) { cust ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = cust.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "هاتف: ${cust.phone} • الجنسية: ${cust.nationality}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    IconButton(onClick = { onDelete(cust.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف العميل", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var nid by remember { mutableStateOf("") }
    var nat by remember { mutableStateOf("يمني") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة عميل / نزيل جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم الرباعي") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nid, onValueChange = { nid = it }, label = { Text("رقم الهوية أو جواز السفر") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nat, onValueChange = { nat = it }, label = { Text("الجنسية") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && phone.isNotBlank()) onConfirm(name, phone, nid, nat) }
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun AddReservationDialog(
    customers: List<CustomerEntity>,
    units: List<UnitEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, Long, Long, Long) -> Unit
) {
    var selectedCustId by remember { mutableStateOf(customers.firstOrNull()?.id ?: 0L) }
    var selectedUnitId by remember { mutableStateOf(units.firstOrNull()?.id ?: 0L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء حجز فندقي مؤكد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("النزيل: ${customers.find { it.id == selectedCustId }?.fullName ?: "لا يوجد عملاء"}")
                Text("الغرفة المختارة: رقم ${units.find { it.id == selectedUnitId }?.unitNumber ?: "لا توجد غرف متاحة"}")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedCustId > 0 && selectedUnitId > 0) {
                        val unit = units.find { it.id == selectedUnitId }
                        val now = System.currentTimeMillis()
                        val checkout = now + 86400000L
                        onConfirm(selectedCustId, selectedUnitId, now, checkout, unit?.customNightlyPriceMinor ?: 1500000L)
                    }
                }
            ) { Text("تأكيد الحجز") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun QuickCheckInDialog(
    customers: List<CustomerEntity>,
    availableUnits: List<UnitEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, Long) -> Unit
) {
    val cust = customers.firstOrNull()
    val unit = availableUnits.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل وصول نزيل مباشر (Check-In)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cust == null || unit == null) {
                    Text("يرجى التأكد من توفر عميل واحد على الأقل وغرفة واحدة متاحة.")
                } else {
                    Text("تسجيل وصول: ${cust.fullName}")
                    Text("الغرفة: رقم ${unit.unitNumber}")
                    val price = unit.customNightlyPriceMinor ?: 1500000L
                    Text("السعر لليلة: ${Money.fromMinor(price).formatted}")
                }
            }
        },
        confirmButton = {
            if (cust != null && unit != null) {
                val price = unit.customNightlyPriceMinor ?: 1500000L
                Button(onClick = { onConfirm(cust.id, unit.id, price) }) {
                    Text("إتمام التسكين الآن")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
