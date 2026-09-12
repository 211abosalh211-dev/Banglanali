package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.NavDestination
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.ErpMasterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErpAppRoot(
    viewModel: ErpMasterViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Android back button handling
    BackHandler {
        if (state.selectedDestination != NavDestination.DASHBOARD) {
            viewModel.selectDestination(NavDestination.DASHBOARD)
        } else {
            viewModel.setExitConfirmDialog(true)
        }
    }

    if (state.showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setExitConfirmDialog(false) },
            title = { Text("تأكيد الخروج من النظام", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد بالتأكيد إغلاق نظام HOTEL ERP PRO؟ جميع البيانات والعمليات محفوظة بأمان.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setExitConfirmDialog(false)
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("خروج")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setExitConfirmDialog(false) }) {
                    Text("إلغاء")
                }
            },
            modifier = Modifier.testTag("dialog_exit_confirmation")
        )
    }

    // Strict RTL Layout for Arabic Hotel Operations
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (state.session == null) {
            LoginScreen(viewModel = viewModel)
        } else {
            BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 600.dp

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = state.hotelName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
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
                                            text = "المستخدم: ${state.session?.user?.fullName} • متصل محلياً",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(onClick = { viewModel.logout() }) {
                                    Icon(
                                        imageVector = Icons.Default.ExitToApp,
                                        contentDescription = "تسجيل الخروج",
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
                        if (!isWideScreen) {
                            ScrollableTabRow(
                                selectedTabIndex = NavDestination.entries.indexOf(state.selectedDestination).coerceAtLeast(0),
                                edgePadding = 8.dp,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                            ) {
                                NavDestination.entries.forEach { dest ->
                                    val isSelected = state.selectedDestination == dest
                                    Tab(
                                        selected = isSelected,
                                        onClick = { viewModel.selectDestination(dest) },
                                        text = {
                                            Text(
                                                text = dest.titleAr,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.titleAr,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isWideScreen) {
                            NavigationRail(
                                modifier = Modifier.fillMaxHeight(),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                NavDestination.entries.forEach { dest ->
                                    val isSelected = state.selectedDestination == dest
                                    NavigationRailItem(
                                        selected = isSelected,
                                        onClick = { viewModel.selectDestination(dest) },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.titleAr
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = dest.titleAr,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            when (state.selectedDestination) {
                                NavDestination.DASHBOARD -> DashboardView(state = state, viewModel = viewModel)
                                NavDestination.HOTEL -> HotelUnitsView(state = state, viewModel = viewModel)
                                NavDestination.CUSTOMERS -> CustomersReservationsView(state = state, viewModel = viewModel)
                                NavDestination.ACCOUNTING -> AccountingCashboxView(state = state, viewModel = viewModel)
                                NavDestination.CASHBOX -> AccountingCashboxView(state = state, viewModel = viewModel)
                                NavDestination.INVENTORY -> OperationsPosView(state = state, viewModel = viewModel)
                                NavDestination.POS -> OperationsPosView(state = state, viewModel = viewModel)
                                NavDestination.REPORTS -> ReportsFinancialView(state = state, viewModel = viewModel)
                                NavDestination.SECURITY -> SecurityRbacView(state = state, viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
