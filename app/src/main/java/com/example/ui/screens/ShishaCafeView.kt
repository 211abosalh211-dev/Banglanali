package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.currency.Money
import com.example.ui.viewmodel.ActiveHookahSession
import com.example.ui.viewmodel.ErpMasterViewModel
import com.example.ui.viewmodel.ErpUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ShishaMenuItem(
    val id: Long,
    val nameAr: String,
    val brand: String,
    val priceMinor: Long,
    val costMinor: Long,
    val description: String
)

val defaultShishaMenu = listOf(
    ShishaMenuItem(1, "رأس تفاحتين نخله فاخر", "نخلة أصلي", 250000L, 60000L, "النكهة الكلاسيكية الأكثر طلباً مع فحم طبيعي"),
    ShishaMenuItem(2, "رأس عنب توت ملكي", "الفاخر", 250000L, 60000L, "مزيج ملكي منعش من العنب البري والتوت"),
    ShishaMenuItem(3, "رأس ليمون نعناع مثلج", "الفاخر", 250000L, 60000L, "انتعاش الليمون الحامض مع أوراق النعناع البارد"),
    ShishaMenuItem(4, "رأس علكة مستكة مبرد", "مزاج خاص", 250000L, 60000L, "نكهة المستكة التراثية العطرية مع نفحة تبريد"),
    ShishaMenuItem(5, "رأس سلوم بلدي خاص", "سلوم مصري", 200000L, 50000L, "معسل سلوم خام ثقيل لعشاق النكهة التقليدية"),
    ShishaMenuItem(6, "رأس بلوبيري بلو آيس", "ستاربز", 250000L, 60000L, "نكهة التوت الأزرق الفاخر مع رذاذ مثلج")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShishaCafeView(
    state: ErpUiState,
    viewModel: ErpMasterViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("الجلسات المشتعلة", "قائمة المعسلات والنكهات", "معادلة الاستهلاك Recipe")
    var showNewOrderDialog by remember { mutableStateOf(false) }
    var preselectedFlavor by remember { mutableStateOf<ShishaMenuItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header & Quick Stats
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.LocalCafe, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "قسم الشيش والمعسلات والخدمات",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "إدارة الرؤوس، الفحم، الجلسات، واستهلاك المواد آلياً والربط المالي",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            preselectedFlavor = null
                            showNewOrderDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("btn_new_shisha_order")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("طلب رأس جديد")
                    }
                }

                Spacer(Modifier.height(14.dp))

                // KPI Metrics Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiShishaCard(
                        title = "الرؤوس اليوم",
                        value = "${state.shishaHeadsSoldToday} رأس",
                        color = Color(0xFF1565C0),
                        modifier = Modifier.weight(1f)
                    )
                    KpiShishaCard(
                        title = "إيرادات الشيشة",
                        value = Money.fromMinor(state.shishaRevenuesTodayMinor).formatted,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                    KpiShishaCard(
                        title = "تكلفة المواد",
                        value = Money.fromMinor(state.shishaCostTodayMinor).formatted,
                        color = Color(0xFFC62828),
                        modifier = Modifier.weight(1f)
                    )
                    val profitMinor = state.shishaRevenuesTodayMinor - state.shishaCostTodayMinor
                    KpiShishaCard(
                        title = "صافي الأرباح",
                        value = Money.fromMinor(profitMinor).formatted,
                        color = Color(0xFF6A1B9A),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> ActiveHookahsSection(
                    activeHookahs = state.activeHookahs,
                    onRefillCoal = { viewModel.refillShishaCoal(it) },
                    onCloseSession = { viewModel.closeShishaSession(it) },
                    onNewOrder = {
                        preselectedFlavor = null
                        showNewOrderDialog = true
                    }
                )
                1 -> ShishaMenuSection(
                    onOrderFlavor = { flavor ->
                        preselectedFlavor = flavor
                        showNewOrderDialog = true
                    }
                )
                2 -> RecipeConsumptionSection()
            }
        }
    }

    if (showNewOrderDialog) {
        OrderShishaDialog(
            preselected = preselectedFlavor,
            cashboxes = state.cashboxes,
            occupiedUnits = state.units.filter { it.status == "OCCUPIED" },
            onDismiss = { showNewOrderDialog = false },
            onConfirm = { flavor, loc, server, price, cost, boxId, isCash ->
                viewModel.orderShishaHead(
                    flavorName = flavor,
                    location = loc,
                    serverName = server,
                    priceMinor = price,
                    costMinor = cost,
                    cashboxId = boxId,
                    isPaidCash = isCash
                )
                showNewOrderDialog = false
            }
        )
    }
}

@Composable
fun KpiShishaCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = color)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ActiveHookahsSection(
    activeHookahs: List<ActiveHookahSession>,
    onRefillCoal: (Long) -> Unit,
    onCloseSession: (Long) -> Unit,
    onNewOrder: () -> Unit
) {
    if (activeHookahs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.LocalCafe, contentDescription = null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.outline)
                Text("لا توجد رؤوس شيشة مشتعلة حالياً", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("يمكنك تسجيل طلب جديد لرأس شيشة لجلسة، خيمة، أو غرفة نزيل", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Button(onClick = onNewOrder) {
                    Text("طلب رأس شيشة الآن")
                }
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(activeHookahs) { session ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (session.status == "COAL_REFILLED") Color(0xFFFFA000) else Color(0xFFD32F2F))
                                )
                                Text(
                                    text = session.location,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = session.flavorName,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        val sdf = SimpleDateFormat("HH:mm", Locale.ENGLISH)
                        val startStr = sdf.format(Date(session.startTime))
                        val lastCoalStr = sdf.format(Date(session.lastCoalRefreshTime))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "وقت البدء: $startStr • آخر فحم: $lastCoalStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "المقدّم: ${session.serverName}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "السعر: ${Money.fromMinor(session.priceMinor).formatted}",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onRefillCoal(session.id) },
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("تجديد فحم (3 قطع)", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { onCloseSession(session.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("إنهاء وتسوية", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShishaMenuSection(
    onOrderFlavor: (ShishaMenuItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 260.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(defaultShishaMenu) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.nameAr, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(item.brand, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "سعر البيع: ${Money.fromMinor(item.priceMinor).formatted}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "التكلفة التقديرية: ${Money.fromMinor(item.costMinor).formatted}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Button(
                            onClick = { onOrderFlavor(item) },
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("طلب الآن", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeConsumptionSection() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "معادلة استهلاك المواد المعيارية لكل رأس شيشة (Recipe Consumption):",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "يقوم نظام HOTEL ERP PRO عند طلب أو بيع أي رأس شيشة بحساب وخصم المواد المستهلكة آلياً وترحيل تكلفتها ومبيعاتها إلى دليل الحسابات والصناديق.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Consumption Breakdown Cards
        RecipeIngredientCard(
            icon = Icons.Default.Spa,
            title = "المعسل الخام (Tobacco)",
            quantityPerHead = "25 جرام / للرأس الواحد",
            costShare = "حوالي 350 ريال",
            description = "يتم خصمه من مستودع المعسلات تلقائياً (بكت 1 كجم يكفي لـ 40 رأساً)"
        )

        RecipeIngredientCard(
            icon = Icons.Default.LocalFireDepartment,
            title = "الفحم الطبيعي الفاخر (Charcoal)",
            quantityPerHead = "3 قطع مكعبات (~40 جرام) + تجديد عند الطلب",
            costShare = "حوالي 150 ريال",
            description = "فحم جوز الهند الطبيعي عديم الرائحة والدخان"
        )

        RecipeIngredientCard(
            icon = Icons.Default.CleanHands,
            title = "مبسم صحي معقم وقصدير (Sanitary Mouthpiece & Foil)",
            quantityPerHead = "1 مبسم صحي مفرد + 1 قطعة قصدير مخرّم",
            costShare = "حوالي 100 ريال",
            description = "استهلاك صحي مباشر لمرة واحدة لكل نزيل لضمان أعلى معايير النظافة"
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("الأثر المالي والمحاسبي المباشر:", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("• مدين: الصندوق الرئيسي أو حساب الغرفة (إجمالي سعر البيع 2,500 ريال).", style = MaterialTheme.typography.bodySmall)
                Text("• دائن: حساب 40201 (إيرادات خدمات وكافيه وشيشة).", style = MaterialTheme.typography.bodySmall)
                Text("• هامش ربح تشغيلي صافٍ يتجاوز 75% لكل رأس.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            }
        }
    }
}

@Composable
fun RecipeIngredientCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    quantityPerHead: String,
    costShare: String,
    description: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(costShare, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(2.dp))
                Text("الاستهلاك المعياري: $quantityPerHead", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderShishaDialog(
    preselected: ShishaMenuItem?,
    cashboxes: List<com.example.data.local.entity.cashbox.CashboxEntity>,
    occupiedUnits: List<com.example.data.local.entity.hotel.UnitEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Long, Long, Long, Boolean) -> Unit
) {
    var selectedFlavor by remember { mutableStateOf(preselected ?: defaultShishaMenu.first()) }
    var locationType by remember { mutableStateOf("ROOM") } // ROOM, TABLE, TENT
    var selectedRoomNumber by remember { mutableStateOf(occupiedUnits.firstOrNull()?.unitNumber ?: "101") }
    var tableOrTentNumber by remember { mutableStateOf("طاولة رقم 1") }
    var serverName by remember { mutableStateOf("معلم الشيشة") }
    var isCashPayment by remember { mutableStateOf(true) }
    val defaultCashboxId = cashboxes.firstOrNull()?.id ?: 1L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل طلب رأس شيشة جديد", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("اختر نكهة المعسل:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                defaultShishaMenu.forEach { flavor ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFlavor = flavor }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedFlavor.id == flavor.id, onClick = { selectedFlavor = flavor })
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(flavor.nameAr, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${Money.fromMinor(flavor.priceMinor).formatted} • ${flavor.brand}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                Divider()

                Text("موقع التقديم والخدمة:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = locationType == "ROOM",
                        onClick = { locationType = "ROOM" },
                        label = { Text("غرفة نزيل") }
                    )
                    FilterChip(
                        selected = locationType == "TABLE",
                        onClick = { locationType = "TABLE" },
                        label = { Text("طاولة صالة") }
                    )
                    FilterChip(
                        selected = locationType == "TENT",
                        onClick = { locationType = "TENT" },
                        label = { Text("خيمة / جلسة") }
                    )
                }

                if (locationType == "ROOM") {
                    OutlinedTextField(
                        value = selectedRoomNumber,
                        onValueChange = { selectedRoomNumber = it },
                        label = { Text("رقم الغرفة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = tableOrTentNumber,
                        onValueChange = { tableOrTentNumber = it },
                        label = { Text(if (locationType == "TABLE") "رقم الطاولة" else "اسم / رقم الخيمة والجلسة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = serverName,
                    onValueChange = { serverName = it },
                    label = { Text("اسم المباشر / مقدّم الخدمة") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الدفع نقداً كاش فوري:", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isCashPayment, onCheckedChange = { isCashPayment = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalLocation = if (locationType == "ROOM") "غرفة $selectedRoomNumber" else tableOrTentNumber
                    onConfirm(
                        selectedFlavor.nameAr,
                        finalLocation,
                        serverName,
                        selectedFlavor.priceMinor,
                        selectedFlavor.costMinor,
                        defaultCashboxId,
                        isCashPayment
                    )
                },
                modifier = Modifier.testTag("btn_confirm_shisha_order")
            ) {
                Text("تأكيد وتقديم الرأس")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
