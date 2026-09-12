package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Security
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavDestination(
    val route: String,
    val titleAr: String,
    val titleEn: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD(
        route = "dashboard",
        titleAr = "الرئيسية",
        titleEn = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_item_dashboard"
    ),
    HOTEL(
        route = "hotel",
        titleAr = "الفندق والغرف",
        titleEn = "Hotel",
        selectedIcon = Icons.Filled.Hotel,
        unselectedIcon = Icons.Outlined.Hotel,
        testTag = "nav_item_hotel"
    ),
    CUSTOMERS(
        route = "customers",
        titleAr = "الحجوزات والنزلاء",
        titleEn = "Guests & Bookings",
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People,
        testTag = "nav_item_customers"
    ),
    ACCOUNTING(
        route = "accounting",
        titleAr = "المحاسبة والمالية",
        titleEn = "Accounting",
        selectedIcon = Icons.Filled.AccountBalance,
        unselectedIcon = Icons.Outlined.AccountBalance,
        testTag = "nav_item_accounting"
    ),
    CASHBOX(
        route = "cashbox",
        titleAr = "الصناديق والورديات",
        titleEn = "Cash & Shifts",
        selectedIcon = Icons.Filled.AttachMoney,
        unselectedIcon = Icons.Outlined.AttachMoney,
        testTag = "nav_item_cashbox"
    ),
    INVENTORY(
        route = "inventory",
        titleAr = "المخزون والمشتريات",
        titleEn = "Inventory",
        selectedIcon = Icons.Filled.Inventory,
        unselectedIcon = Icons.Outlined.Inventory,
        testTag = "nav_item_inventory"
    ),
    POS(
        route = "pos",
        titleAr = "نقطة البيع POS",
        titleEn = "Point of Sale",
        selectedIcon = Icons.Filled.PointOfSale,
        unselectedIcon = Icons.Outlined.PointOfSale,
        testTag = "nav_item_pos"
    ),
    SHISHA(
        route = "shisha",
        titleAr = "الشيش والمعسلات",
        titleEn = "Shisha & Cafe",
        selectedIcon = Icons.Filled.LocalCafe,
        unselectedIcon = Icons.Outlined.LocalCafe,
        testTag = "nav_item_shisha"
    ),
    REPORTS(
        route = "reports",
        titleAr = "التقارير المالية",
        titleEn = "Financial Reports",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
        testTag = "nav_item_reports"
    ),
    SECURITY(
        route = "security",
        titleAr = "المستخدمين والنظام",
        titleEn = "Users & RBAC",
        selectedIcon = Icons.Filled.Security,
        unselectedIcon = Icons.Outlined.Security,
        testTag = "nav_item_security"
    )
}
