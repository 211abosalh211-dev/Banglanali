package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.SystemConfigEntity
import com.example.ui.navigation.NavDestination
import com.example.ui.theme.EmeraldFinance
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.FoundationUiState
import com.example.ui.viewmodel.FoundationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FoundationViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showEditHotelDialog by remember { mutableStateOf(false) }

    // Display snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Android Back Navigation governance
    BackHandler {
        if (uiState.selectedDestination != NavDestination.DASHBOARD) {
            viewModel.selectDestination(NavDestination.DASHBOARD)
        } else {
            viewModel.showExitConfirmation()
        }
    }

    // Exit confirmation dialog
    if (uiState.showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissExitConfirmation() },
            title = { Text("تأكيد الخروج من النظام", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد بالتأكيد إغلاق نظام الفندق؟ جميع العمليات والبيانات محفوظة محلياً بأمان.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissExitConfirmation()
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("خروج")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissExitConfirmation() }) {
                    Text("إلغاء")
                }
            },
            modifier = Modifier.testTag("dialog_exit_confirmation")
        )
    }

    // Edit Hotel Name Dialog
    if (showEditHotelDialog) {
        var tempName by remember { mutableStateOf(uiState.hotelName) }
        AlertDialog(
            onDismissRequest = { showEditHotelDialog = false },
            title = { Text("تعديل اسم المنشأة الفندقية", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("يتم حفظ التعديل مباشرة في قاعدة البيانات المحلية مع تسجيل قيد تدقيق تلقائي.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("اسم الفندق") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_hotel_name")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            viewModel.updateHotelConfig("hotel_name", tempName.trim(), "اسم المنشأة الفندقية")
                            showEditHotelDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditHotelDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Mandatory RTL Layout Direction Support
    val layoutDirection = if (uiState.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.hotelName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Text(
                                    text = "OFFLINE-FIRST • YER (ر.ي)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showEditHotelDialog = true },
                            modifier = Modifier.testTag("btn_edit_hotel_name")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل اسم الفندق",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.toggleLayoutDirection() },
                            modifier = Modifier.testTag("btn_toggle_rtl")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "تبديل اتجاه الواجهة",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NavyPrimary,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    NavDestination.entries.forEach { destination ->
                        val isSelected = uiState.selectedDestination == destination
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.selectDestination(destination) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.titleAr
                                )
                            },
                            label = {
                                Text(
                                    text = destination.titleAr,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                indicatorColor = NavyPrimary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.testTag(destination.testTag)
                        )
                    }
                }
            },
            floatingActionButton = {
                if (uiState.selectedDestination == NavDestination.DASHBOARD) {
                    FloatingActionButton(
                        onClick = { viewModel.addManualAuditVerification("فحص وتدقيق ميداني يدوي - المرحلة الأولى") },
                        containerColor = GoldAccent,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_add_audit_log")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAlert,
                            contentDescription = "تسجيل قيد تدقيق جديد"
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (uiState.selectedDestination) {
                    NavDestination.DASHBOARD -> DashboardScreen(uiState = uiState, viewModel = viewModel)
                    NavDestination.HOTEL -> RoomsOverviewScreen(uiState = uiState)
                    NavDestination.ACCOUNTING -> AccountingOverviewScreen(uiState = uiState, viewModel = viewModel)
                    NavDestination.REPORTS -> AuditSystemScreen(uiState = uiState, viewModel = viewModel)
                    else -> DashboardScreen(uiState = uiState, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    uiState: FoundationUiState,
    viewModel: FoundationViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Governance & Phase 01 Header Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_phase01_banner")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "HOTEL ERP PRO",
                            style = MaterialTheme.typography.labelLarge,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF064E3B), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "المرحلة 01: جاهز للاعتماد",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6EE7B7),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "أساس النظام المعماري وقاعدة البيانات",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "نظام محلي متكامل (Offline-First) مبني وفق معايير المحاسبة أولاً والحماية من فقد البيانات، بالريال اليمني (YER).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        // System Health Indicators
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Database Status Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_db_status"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldFinance,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Room DB",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "نشطة ومحمية",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmeraldFinance
                        )
                    }
                }

                // Audit Logs Count Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_audit_count"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${uiState.auditCount} قيود",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "سجل الرقابة النشط",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Currency Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_currency_status"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.defaultCurrency,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.currencyNameAr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Precision Financial Calculation Engine Test
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_precision_engine")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "محرك الدقة المالية (Zero Floating Point Error)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = EmeraldFinance
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "اختبار مطابقة العمليات بالريال اليمني (YER): 3 ليالٍ × 35,000.75 ر.ي - خصم 5,000.25 ر.ي + ضريبة 5,000.10 ر.ي:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الإجمالي الدقيق المطابق:",
                                fontWeight = FontWeight.SemiBold,
                                color = NavyPrimary
                            )
                            Text(
                                text = uiState.precisionTestResult.formatWithCurrency(arabicSymbol = true),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = EmeraldFinance
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.runPrecisionVerification() },
                        modifier = Modifier.align(Alignment.End).testTag("btn_recalculate_precision"),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Text("إعادة احتساب التحقق المالي")
                    }
                }
            }
        }

        // Phase 02 — Database & Data Integrity Engine Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_phase02_engine")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "محرك سلامة البيانات وقاعدة البيانات (المرحلة 02)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = NavyPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("إصدار المخطط", fontSize = 11.sp, color = Color.Gray)
                                Text("Schema v${uiState.databaseSchemaVersion}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                                Text("Room Migration 1->2", fontSize = 10.sp, color = EmeraldFinance)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("شجرة الحسابات COA", fontSize = 11.sp, color = Color.Gray)
                                Text("${uiState.accountsCount} حسابات", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                                Text("Debit = Credit صارم", fontSize = 10.sp, color = EmeraldFinance)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("الصناديق والوحدات", fontSize = 11.sp, color = Color.Gray)
                                Text("${uiState.cashboxesCount} صندوق / ${uiState.unitsCount} وحدة", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                                Text("Soft Delete مفعل", fontSize = 10.sp, color = EmeraldFinance)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    GovernanceItem(
                        title = "Double-Entry Accounting Balance Integrity",
                        desc = "منع أي قيد غير متوازن، التحقق الأتومي من توازن طرفي القيد، وتحديث أرصدة الحسابات بدقة."
                    )
                    GovernanceItem(
                        title = "Atomic Transactions (No-Data-Loss)",
                        desc = "استخدام database.withTransaction في إصدار الفواتير والسداد والتحويلات مع Rollback فوري عند الخطأ."
                    )
                    GovernanceItem(
                        title = "Foreign Key & Soft Delete Protection",
                        desc = "منع الحذف النهائي للعملاء والحسابات والمنتجات المرتبطة بحركات مالية لحماية التاريخ المالي."
                    )
                }
            }
        }

        // Principles Checklist Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "حوكمة المرحلة الأولى (Foundation Principles)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    GovernanceItem(
                        title = "Database-First & No Data Loss",
                        desc = "قاعدة بيانات Room محلية مع مبدأ Soft Delete وجداول الإعدادات والرقابة."
                    )
                    GovernanceItem(
                        title = "Accounting-First & Currency Accuracy",
                        desc = "حسابات مالية دقيقة بوحدات المائة الدقيقة للريال اليمني بدون أخطاء فاصلة عائمة."
                    )
                    GovernanceItem(
                        title = "Mobile-First & System UI Respect",
                        desc = "دعم كامل للـ Edge-to-Edge و Safe Insets وشاشات اللمس (48dp)."
                    )
                    GovernanceItem(
                        title = "Audit Trail Security",
                        desc = "تسجيل هوية المشغل، التاريخ، نوع الإجراء، والقيم السابقة والجديدة."
                    )
                }
            }
        }
    }
}

@Composable
private fun GovernanceItem(title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = EmeraldFinance,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun RoomsOverviewScreen(uiState: FoundationUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "إدارة الغرف والإشغال (تأسيس المرحلة)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "هيكل توزيع وتصنيف وحدات الإقامة الفندقية",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("card_room_foundation_preview")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "مخطط إدارة الغرف (Room Management Structure)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تم تجهيز بنية الكيانات والعلاقات الفندقية لربط الحجوزات، الإقامات، تسعير الليلة بالريال اليمني (YER)، وحالات النظافة والتجهيز في المراحل القادمة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }

        items(
            listOf(
                Triple("جناح ملكي فاخر (Royal Suite)", "101", "شاغرة - جاهزة للنزول"),
                Triple("غرفة ديلوكس مفردة (Deluxe Single)", "102", "شاغرة - تحت الصيانة"),
                Triple("جناح عائلي تنفيذي (Executive Suite)", "201", "محجوزة - مؤكدة"),
                Triple("غرفة قياسية مزدوجة (Standard Double)", "202", "شاغرة - جاهزة للنزول")
            )
        ) { (roomType, roomNumber, status) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hotel,
                            contentDescription = null,
                            tint = NavyPrimary
                        )
                        Column {
                            Text(text = roomType, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "رقم الغرفة: $roomNumber", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                if (status.contains("جاهزة")) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = status,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (status.contains("جاهزة")) Color(0xFF065F46) else Color(0xFF92400E)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountingOverviewScreen(
    uiState: FoundationUiState,
    viewModel: FoundationViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "النظام المالي والمحاسبي (Accounting Engine)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "قواعد القيود المزدوجة، الصناديق، والتحقق الحسابي الدقيق",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("card_accounting_rules")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "المعايير المالية المطبقة:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("1. كل عملية مالية (قبض، صرف، حجز) ترتبط مباشرة بقيد وتحديث الصندوق.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("2. لا يسمح بعمليات مالية غير متوازنة (Debit = Credit).", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("3. التعامل المالي مبني على أصغر وحدة نقدية صلبة بدون تقريب عشوائي.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("4. العملة الأساسية الافتراضية: الريال اليمني (YER).", fontSize = 13.sp)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "محاكاة الحركات المالية وتوازن القيود",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("العملية: إيداع تأمين إقامة نزيل", fontSize = 13.sp)
                        Text("50,000.00 ر.ي", fontWeight = FontWeight.Bold, color = EmeraldFinance)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الطرف المدين: الصندوق الرئيسي", fontSize = 12.sp, color = Color.Gray)
                        Text("+50,000.00 ر.ي", fontSize = 12.sp, color = EmeraldFinance)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الطرف الدائن: أمانات النزلاء", fontSize = 12.sp, color = Color.Gray)
                        Text("-50,000.00 ر.ي", fontSize = 12.sp, color = NavyPrimary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFD1FAE5), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "القيد متوازن تماماً (الفارق = 0.00 ر.ي)",
                            color = Color(0xFF065F46),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditSystemScreen(
    uiState: FoundationUiState,
    viewModel: FoundationViewModel
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "سجل الرقابة والتدقيق (Audit Trail)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "جميع العمليات الحساسة مسجلة في قاعدة بيانات Room مع تفاصيل المشغل والتوقيت",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        item {
            Text(
                text = "إعدادات النظام المحفوظة في قاعدة البيانات:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(uiState.systemConfigs) { config ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = config.configKey, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = config.description, fontSize = 11.sp, color = Color.Gray)
                    }
                    Text(
                        text = config.configValue,
                        fontWeight = FontWeight.SemiBold,
                        color = NavyPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "سجل الحركات والتدقيق النشطة (${uiState.auditLogs.size} قيد):",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.auditLogs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد قيود مسجلة بعد. استخدم الزر العائم لإضافة قيد فحص.")
                    }
                }
            }
        } else {
            items(uiState.auditLogs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("item_audit_log_${log.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(NavyPrimary, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = log.actionType,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = log.entityType,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = dateFormat.format(Date(log.timestamp)),
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = log.details,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "المشغل: ${log.operatorName} (${log.operatorRole})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            if (log.newValue != null) {
                                Text(
                                    text = "القيمة: ${log.newValue}",
                                    fontSize = 11.sp,
                                    color = EmeraldFinance,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
