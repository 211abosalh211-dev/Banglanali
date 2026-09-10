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
import com.example.data.local.entity.inventory.ProductEntity
import com.example.data.local.entity.sales.SalesInvoiceEntity
import com.example.data.local.entity.supplier.SupplierEntity
import com.example.ui.viewmodel.ErpMasterViewModel
import com.example.ui.viewmodel.ErpUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationsPosView(
    state: ErpUiState,
    viewModel: ErpMasterViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var showPosSaleDialog by remember { mutableStateOf(false) }
    var selectedProductForSale by remember { mutableStateOf<ProductEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showAddProductDialog = true
                    else if (selectedTab == 1) showPurchaseDialog = true
                    else showAddSupplierDialog = true
                }
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
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("المستودع والأصناف") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("نقطة البيع (POS)") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("الموردين والمشتريات") })
            }

            when (selectedTab) {
                0 -> InventoryProductsSection(
                    products = state.products,
                    onSell = { prod ->
                        selectedProductForSale = prod
                        showPosSaleDialog = true
                    }
                )
                1 -> PosSalesHistorySection(sales = state.sales)
                2 -> SuppliersSection(
                    suppliers = state.suppliers,
                    onAddSupplier = { showAddSupplierDialog = true },
                    onNewPurchase = { showPurchaseDialog = true }
                )
            }
        }
    }

    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onConfirm = { name, buy, sell, stock ->
                viewModel.addProduct(name, 1L, buy, sell, stock)
                showAddProductDialog = false
            }
        )
    }

    if (showAddSupplierDialog) {
        AddSupplierDialog(
            onDismiss = { showAddSupplierDialog = false },
            onConfirm = { name, phone, tax ->
                viewModel.addSupplier(name, phone, tax)
                showAddSupplierDialog = false
            }
        )
    }

    if (showPurchaseDialog) {
        PurchaseInvoiceDialog(
            suppliers = state.suppliers,
            products = state.products,
            onDismiss = { showPurchaseDialog = false },
            onConfirm = { supId, prodId, qty, price, isCash ->
                val boxId = state.cashboxes.firstOrNull()?.id ?: 1L
                val whId = state.warehouses.firstOrNull()?.id ?: 1L
                viewModel.recordPurchase(supId, whId, boxId, prodId, qty, price, isCash)
                showPurchaseDialog = false
            }
        )
    }

    if (showPosSaleDialog && selectedProductForSale != null) {
        PosSaleDialog(
            product = selectedProductForSale!!,
            onDismiss = {
                showPosSaleDialog = false
                selectedProductForSale = null
            },
            onConfirm = { qty, customer ->
                val boxId = state.cashboxes.firstOrNull()?.id ?: 1L
                val whId = state.warehouses.firstOrNull()?.id ?: 1L
                viewModel.makePosSale(boxId, whId, selectedProductForSale!!.id, qty, customer)
                showPosSaleDialog = false
                selectedProductForSale = null
            }
        )
    }
}

@Composable
fun InventoryProductsSection(
    products: List<ProductEntity>,
    onSell: (ProductEntity) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(products) { prod ->
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = prod.nameAr, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "سعر البيع: ${Money.fromMinor(prod.defaultSalePriceMinor).formatted} • التكلفة: ${Money.fromMinor(prod.defaultCostPriceMinor).formatted}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "الحد الأدنى للطلب: ${prod.minReorderLevel} قطعة",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { onSell(prod) },
                        enabled = true
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("بيع")
                    }
                }
            }
        }
    }
}

@Composable
fun PosSalesHistorySection(sales: List<SalesInvoiceEntity>) {
    if (sales.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لم تتم أي عمليات بيع POS حتى الآن. اضغط بيع بجانب أي صنف في تبويب الأصناف")
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(sales) { sale ->
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
                            Text(text = "فاتورة بيع رقم: ${sale.saleNumber}", fontWeight = FontWeight.Bold)
                            Text(text = "الحالة: ${sale.status} • التاريخ: ${sale.saleDate}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = Money.fromMinor(sale.totalAmountMinor).formatted,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuppliersSection(
    suppliers: List<SupplierEntity>,
    onAddSupplier: () -> Unit,
    onNewPurchase: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNewPurchase, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("تسجيل مشتريات")
            }
            FilledTonalButton(onClick = onAddSupplier, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("إضافة مورد")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(suppliers) { sup ->
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
                            Text(text = sup.companyName, fontWeight = FontWeight.Bold)
                            Text(text = "هاتف: ${sup.phone}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = "المستحق: ${Money.fromMinor(sup.currentBalanceMinor).formatted}",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Long, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var buyPriceText by remember { mutableStateOf("1000") }
    var sellPriceText by remember { mutableStateOf("1500") }
    var stockText by remember { mutableStateOf("50") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة صنف جديد للمخزون") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الصنف / المنتج") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = buyPriceText, onValueChange = { buyPriceText = it }, label = { Text("سعر التكلفة (ريال)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = sellPriceText, onValueChange = { sellPriceText = it }, label = { Text("سعر البيع (ريال)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = stockText, onValueChange = { stockText = it }, label = { Text("الكمية الأولية") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val buy = (buyPriceText.toDoubleOrNull() ?: 0.0).toLong() * 100
                    val sell = (sellPriceText.toDoubleOrNull() ?: 0.0).toLong() * 100
                    val stock = stockText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) onConfirm(name, buy, sell, stock)
                }
            ) { Text("حفظ الصنف") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun AddSupplierDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var tax by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مورد جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المورد أو الشركة") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = tax, onValueChange = { tax = it }, label = { Text("الرقم الضريبي (اختياري)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, phone, tax) }
            ) { Text("حفظ المورد") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun PurchaseInvoiceDialog(
    suppliers: List<SupplierEntity>,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, Double, Long, Boolean) -> Unit
) {
    val sup = suppliers.firstOrNull()
    val prod = products.firstOrNull()
    var qtyText by remember { mutableStateOf("20") }
    var isCash by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل فاتورة شراء بضاعة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("المورد: ${sup?.companyName ?: "لا يوجد موردين"}")
                Text("الصنف: ${prod?.nameAr ?: "لا توجد أصناف"}")
                OutlinedTextField(value = qtyText, onValueChange = { qtyText = it }, label = { Text("الكمية المشتراة") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isCash, onCheckedChange = { isCash = it })
                    Text("سداد نقدي مباشر من الصندوق (كاش)")
                }
            }
        },
        confirmButton = {
            if (sup != null && prod != null) {
                Button(
                    onClick = {
                        val qty = qtyText.toDoubleOrNull() ?: 1.0
                        onConfirm(sup.id, prod.id, qty, prod.defaultCostPriceMinor, isCash)
                    }
                ) { Text("اعتماد الفاتورة") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun PosSaleDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var qtyText by remember { mutableStateOf("1") }
    var customer by remember { mutableStateOf("نزيل فندقي") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("بيع مباشر في نقطة البيع (POS)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("الصنف: ${product.nameAr}")
                Text("السعر للقطعة: ${Money.fromMinor(product.defaultSalePriceMinor).formatted}")
                OutlinedTextField(value = qtyText, onValueChange = { qtyText = it }, label = { Text("الكمية") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = customer, onValueChange = { customer = it }, label = { Text("اسم العميل أو رقم الغرفة") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyText.toDoubleOrNull() ?: 1.0
                    onConfirm(qty, customer)
                }
            ) { Text("قبض وطباعة الإشعار") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
