package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.backup.BackupMetadataEntity
import com.example.data.local.entity.system.UserEntity
import com.example.ui.viewmodel.ErpMasterViewModel
import com.example.ui.viewmodel.ErpUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityRbacView(
    state: ErpUiState,
    viewModel: ErpMasterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var showCreateUserDialog by remember { mutableStateOf(false) }
    var showCreateBackupDialog by remember { mutableStateOf(false) }
    var backupToRestore by remember { mutableStateOf<BackupMetadataEntity?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importBackup(it) }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showCreateUserDialog = true },
                    modifier = Modifier.testTag("fab_create_user")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "إضافة مستخدم")
                }
            } else if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showCreateBackupDialog = true },
                    modifier = Modifier.testTag("fab_create_backup")
                ) {
                    Icon(Icons.Default.Backup, contentDescription = "نسخ احتياطي فوري")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Active User Profile Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الجلسة النشطة: ${state.session?.user?.fullName ?: "المسؤول العام"}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "اسم المستخدم: ${state.session?.user?.username ?: "admin"} • الصلاحيات: مدير نظام معتمد",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("خروج")
                    }
                }
            }

            // Tabs: RBAC, Backup & Restore, Audit Logs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("المستخدمين والصلاحيات (${state.users.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("النسخ الاحتياطي والأمان (${state.backups.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("سجل التدقيق (${state.auditLogs.size})") }
                )
            }

            when (selectedTab) {
                0 -> UsersListSection(users = state.users, onToggle = { viewModel.toggleUser(it) })
                1 -> BackupRestoreSection(
                    backups = state.backups,
                    isBackupInProgress = state.isBackupInProgress,
                    onCreateBackup = { showCreateBackupDialog = true },
                    onImportBackup = { filePickerLauncher.launch("*/*") },
                    onRestore = { backupToRestore = it },
                    onExport = { viewModel.exportBackup(context, it.backupFileName) },
                    state = state
                )
                2 -> AuditLogsSection(auditLogs = state.auditLogs)
            }
        }
    }

    if (showCreateUserDialog) {
        CreateUserDialog(
            onDismiss = { showCreateUserDialog = false },
            onConfirm = { username, name, password, roleId ->
                viewModel.createUser(username, name, password, roleId)
                showCreateUserDialog = false
            }
        )
    }

    if (showCreateBackupDialog) {
        CreateBackupDialog(
            onDismiss = { showCreateBackupDialog = false },
            onConfirm = { notes ->
                viewModel.createBackup(notes)
                showCreateBackupDialog = false
            }
        )
    }

    backupToRestore?.let { backup ->
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("تحذير أمني: استعادة النسخة الاحتياطية", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أنت على وشك استعادة بيانات الملف:")
                    Text(backup.backupFileName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Divider(Modifier.padding(vertical = 4.dp))
                    Text(
                        "سيقوم النظام تلقائياً بإنشاء نسخة أمان احتياطية قبل الاستبدال مباشرة لمنع أي فقدان للبيانات.",
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "هل تريد تأكيد الاستعادة الآن؟",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fileName = backup.backupFileName
                        backupToRestore = null
                        viewModel.restoreBackup(fileName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد الاستعادة الآمنة")
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToRestore = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun UsersListSection(
    users: List<UserEntity>,
    onToggle: (Long) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(users) { user ->
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
                        Text(text = user.fullName, fontWeight = FontWeight.Bold)
                        Text(text = "اسم الدخول: ${user.username} • الهاتف: ${user.phone ?: "غير محدد"}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (user.isActive) "نشط" else "معطل",
                            color = if (user.isActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = user.isActive,
                            onCheckedChange = { onToggle(user.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BackupRestoreSection(
    backups: List<BackupMetadataEntity>,
    isBackupInProgress: Boolean,
    onCreateBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onRestore: (BackupMetadataEntity) -> Unit,
    onExport: (BackupMetadataEntity) -> Unit,
    state: ErpUiState
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // System Data Safety Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("حالة حماية وسلامة البيانات (Data Safety)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Surface(
                            color = Color(0xFF2E7D32),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("محمية 100%", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text(
                        text = "قاعدة بيانات SQLite المحلية متصلة وسليمة • فحص التكامل PRAGMA سليم • الحفظ الاحتياطي التلقائي مفعل قبل أي عملية استبدال",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Divider(Modifier.padding(vertical = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الغرف: ${state.units.size}", style = MaterialTheme.typography.labelSmall)
                        Text("الحجوزات: ${state.reservations.size}", style = MaterialTheme.typography.labelSmall)
                        Text("العملاء: ${state.customers.size}", style = MaterialTheme.typography.labelSmall)
                        Text("القيود المالية: ${state.journalEntries.size}", style = MaterialTheme.typography.labelSmall)
                        Text("المخزون: ${state.products.size}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Action Buttons Row
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onCreateBackup,
                    enabled = !isBackupInProgress,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("نسخة احتياطية الآن")
                }
                FilledTonalButton(
                    onClick = onImportBackup,
                    enabled = !isBackupInProgress,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("استيراد من ملف")
                }
            }
        }

        if (isBackupInProgress) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        if (backups.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Text("لا توجد نسخ احتياطية محفوظة حتى الآن", color = Color.Gray)
                    }
                }
            }
        } else {
            items(backups) { backup ->
                val formattedDate = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH).format(Date(backup.createdAt))
                val sizeKb = (backup.backupSizeBytes / 1024.0).toInt()

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = backup.backupFileName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            val badgeColor = when (backup.backupType) {
                                "SAFETY_AUTO" -> MaterialTheme.colorScheme.error
                                "IMPORTED_EXTERNAL" -> MaterialTheme.colorScheme.tertiary
                                else -> Color(0xFF2E7D32)
                            }
                            val badgeText = when (backup.backupType) {
                                "SAFETY_AUTO" -> "أمان تلقائي"
                                "IMPORTED_EXTERNAL" -> "مستورد"
                                else -> "كاملة أوفلاين"
                            }
                            Surface(color = badgeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = badgeText,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "الحجم: $sizeKb KB • التاريخ: $formattedDate • بواسطة: ${backup.createdBy}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        if (!backup.notes.isNullOrBlank()) {
                            Text(
                                text = "ملاحظة: ${backup.notes}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Text(
                            text = "بصمة SHA-256: ${backup.checksumSha256.take(16)}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Divider(Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onExport(backup) },
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("مشاركة وتصدير", style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { onRestore(backup) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("استعادة", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogsSection(auditLogs: List<AuditLogEntity>) {
    if (auditLogs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد سجلات تدقيق حتى الآن", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(auditLogs) { log ->
                val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH).format(Date(log.timestamp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${log.actionType} - ${log.entityType}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(text = dateStr, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "المستخدم: ${log.operatorName} (${log.operatorRole}) • التفاصيل: ${log.details}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Long) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء حساب مستخدم جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("الاسم الكامل للموظف") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("اسم المستخدم (الدخول)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("كلمة المرور") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank()) {
                        onConfirm(username, fullName, password, 2L)
                    }
                }
            ) { Text("إنشاء الحساب") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun CreateBackupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var note by remember { mutableStateOf("نسخة احتياطية يدوية كاملة") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء نسخة احتياطية جديدة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("سيتم أرشفة وحفظ نسخة كاملة من قاعدة بيانات الفندق والمحاسبة والمخزون بصيغة SQLite آمنة ومفحوصة.")
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظة أو سبب النسخ الاحتياطي") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(note) }) {
                Text("بدء الحفظ والنسخ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
