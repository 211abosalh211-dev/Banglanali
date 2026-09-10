package com.example.data.repository

import com.example.core.result.HotelErpException
import com.example.core.result.Resource
import com.example.core.security.PasswordHasher
import com.example.core.security.Permissions
import com.example.data.local.HotelDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.system.PermissionEntity
import com.example.data.local.entity.system.RoleEntity
import com.example.data.local.entity.system.RolePermissionEntity
import com.example.data.local.entity.system.UserEntity
import com.example.data.local.entity.system.UserRoleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class UserSession(
    val user: UserEntity,
    val roles: List<RoleEntity>,
    val permissions: Set<String>
) {
    fun hasPermission(permission: String): Boolean {
        if (roles.any { it.roleCode == "ADMIN" }) return true
        return permissions.contains(permission)
    }
}

class AuthRepository(private val database: HotelDatabase) {
    private val securityDao = database.systemSecurityDao()
    private val auditDao = database.auditLogDao()

    private val _currentSession = MutableStateFlow<UserSession?>(null)
    val currentSession: StateFlow<UserSession?> = _currentSession.asStateFlow()

    suspend fun initializeSecurityDefaults() = withContext(Dispatchers.IO) {
        // Ensure default roles exist
        val adminRole = RoleEntity(id = 1, roleCode = "ADMIN", roleNameAr = "مدير النظام الشامل", isSystemRole = true)
        val receptionistRole = RoleEntity(id = 2, roleCode = "RECEPTIONIST", roleNameAr = "موظف استقبال", isSystemRole = true)
        val accountantRole = RoleEntity(id = 3, roleCode = "ACCOUNTANT", roleNameAr = "محاسب مالي", isSystemRole = true)
        securityDao.insertRoles(listOf(adminRole, receptionistRole, accountantRole))

        // Ensure default permissions exist
        val allPermissions = listOf(
            PermissionEntity(permissionCode = Permissions.DASHBOARD_VIEW, moduleName = "Dashboard", descriptionAr = "عرض لوحة القيادة الرئيسية"),
            PermissionEntity(permissionCode = Permissions.USERS_VIEW, moduleName = "Users", descriptionAr = "عرض المستخدمين والصلاحيات"),
            PermissionEntity(permissionCode = Permissions.USERS_CREATE, moduleName = "Users", descriptionAr = "إنشاء مستخدمين جدد"),
            PermissionEntity(permissionCode = Permissions.USERS_UPDATE, moduleName = "Users", descriptionAr = "تعديل المستخدمين والصلاحيات"),
            PermissionEntity(permissionCode = Permissions.USERS_DELETE, moduleName = "Users", descriptionAr = "تعطيل أو حذف المستخدمين"),
            PermissionEntity(permissionCode = Permissions.UNITS_VIEW, moduleName = "Hotel", descriptionAr = "عرض وحدات وغرف الفندق"),
            PermissionEntity(permissionCode = Permissions.UNITS_CREATE, moduleName = "Hotel", descriptionAr = "إضافة وحدات وتصنيفات جديدة"),
            PermissionEntity(permissionCode = Permissions.UNITS_UPDATE, moduleName = "Hotel", descriptionAr = "تحديث حالة الغرف والنظافة والصيانة"),
            PermissionEntity(permissionCode = Permissions.CUSTOMERS_VIEW, moduleName = "Customers", descriptionAr = "عرض بيانات النزلاء والعملاء"),
            PermissionEntity(permissionCode = Permissions.CUSTOMERS_CREATE, moduleName = "Customers", descriptionAr = "إضافة عميل أو نزيل جديد"),
            PermissionEntity(permissionCode = Permissions.CUSTOMERS_UPDATE, moduleName = "Customers", descriptionAr = "تعديل بيانات العميل"),
            PermissionEntity(permissionCode = Permissions.RESERVATIONS_VIEW, moduleName = "Reservations", descriptionAr = "عرض الحجوزات"),
            PermissionEntity(permissionCode = Permissions.RESERVATIONS_CREATE, moduleName = "Reservations", descriptionAr = "إنشاء حجز جديد"),
            PermissionEntity(permissionCode = Permissions.STAYS_CHECKIN, moduleName = "Stays", descriptionAr = "تسجيل وصول نزيل Check-in"),
            PermissionEntity(permissionCode = Permissions.STAYS_CHECKOUT, moduleName = "Stays", descriptionAr = "تسجيل مغادرة نزيل Check-out"),
            PermissionEntity(permissionCode = Permissions.STAYS_TRANSFER, moduleName = "Stays", descriptionAr = "نقل نزيل بين الوحدات"),
            PermissionEntity(permissionCode = Permissions.ACCOUNTING_VIEW, moduleName = "Accounting", descriptionAr = "عرض دليل الحسابات والقوائم المالية"),
            PermissionEntity(permissionCode = Permissions.ACCOUNTING_CREATE, moduleName = "Accounting", descriptionAr = "إنشاء قيود اليومية"),
            PermissionEntity(permissionCode = Permissions.ACCOUNTING_POST, moduleName = "Accounting", descriptionAr = "ترحيل واعتماد القيود المحاسبية"),
            PermissionEntity(permissionCode = Permissions.CASHBOXES_VIEW, moduleName = "Cashbox", descriptionAr = "عرض الصناديق وحركات النقدية"),
            PermissionEntity(permissionCode = Permissions.CASHBOXES_TRANSFER, moduleName = "Cashbox", descriptionAr = "التحويل بين الصناديق"),
            PermissionEntity(permissionCode = Permissions.SHIFTS_MANAGE, moduleName = "Shifts", descriptionAr = "فتح وإغلاق واستلام الورديات"),
            PermissionEntity(permissionCode = Permissions.RECEIPTS_CREATE, moduleName = "Payments", descriptionAr = "إصدار سندات القبض"),
            PermissionEntity(permissionCode = Permissions.PAYMENTS_CREATE, moduleName = "Payments", descriptionAr = "إصدار سندات الصرف"),
            PermissionEntity(permissionCode = Permissions.INVENTORY_VIEW, moduleName = "Inventory", descriptionAr = "عرض المخزون والأصناف"),
            PermissionEntity(permissionCode = Permissions.INVENTORY_MANAGE, moduleName = "Inventory", descriptionAr = "إدارة المخزون والتسويات والتحويلات"),
            PermissionEntity(permissionCode = Permissions.PURCHASING_CREATE, moduleName = "Purchasing", descriptionAr = "تسجيل فواتير المشتريات"),
            PermissionEntity(permissionCode = Permissions.POS_SELL, moduleName = "POS", descriptionAr = "إجراء عمليات البيع المباشر"),
            PermissionEntity(permissionCode = Permissions.POS_RETURN, moduleName = "POS", descriptionAr = "إجراء مردودات المبيعات"),
            PermissionEntity(permissionCode = Permissions.REPORTS_VIEW, moduleName = "Reports", descriptionAr = "عرض التقارير واللوحات المالية"),
            PermissionEntity(permissionCode = Permissions.REPORTS_EXPORT, moduleName = "Reports", descriptionAr = "تصدير وطباعة التقارير المالية")
        )
        securityDao.insertPermissions(allPermissions)

        // Seed default Admin User if not present
        val existingAdmin = securityDao.getUserByUsername("admin")
        if (existingAdmin == null) {
            val adminId = securityDao.insertUser(
                UserEntity(
                    username = "admin",
                    passwordHash = PasswordHasher.hash("admin123"),
                    fullName = "مدير النظام العام",
                    email = "admin@hotelerp.local",
                    phone = "777000111",
                    isActive = true
                )
            )
            securityDao.assignRoleToUser(UserRoleEntity(userId = adminId, roleId = 1))
        }

        // Seed default Receptionist User if not present
        val existingRec = securityDao.getUserByUsername("reception")
        if (existingRec == null) {
            val recId = securityDao.insertUser(
                UserEntity(
                    username = "reception",
                    passwordHash = PasswordHasher.hash("rec123"),
                    fullName = "موظف الاستقبال الأول",
                    email = "rec@hotelerp.local",
                    phone = "777000222",
                    isActive = true
                )
            )
            securityDao.assignRoleToUser(UserRoleEntity(userId = recId, roleId = 2))
        }
    }

    suspend fun login(username: String, password: String): Resource<UserSession> = withContext(Dispatchers.IO) {
        val user = securityDao.getUserByUsername(username.trim())
            ?: return@withContext Resource.Error("اسم المستخدم أو كلمة المرور غير صحيحة")

        if (!user.isActive) {
            return@withContext Resource.Error("هذا الحساب معطل، يرجى مراجعة مسؤول النظام")
        }

        val inputHash = PasswordHasher.hash(password)
        if (user.passwordHash != inputHash) {
            return@withContext Resource.Error("اسم المستخدم أو كلمة المرور غير صحيحة")
        }

        val roles = securityDao.getRolesForUser(user.id)
        val permissions = securityDao.getPermissionsForUser(user.id).toSet()
        val session = UserSession(user = user, roles = roles, permissions = permissions)
        _currentSession.value = session

        auditDao.insertLog(
            AuditLogEntity(
                operatorName = user.fullName,
                operatorRole = roles.firstOrNull()?.roleCode ?: "USER",
                actionType = "LOGIN",
                entityType = "AUTH",
                entityId = user.id.toString(),
                details = "تسجيل دخول ناجح للمستخدم ${user.username}"
            )
        )

        Resource.Success(session)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        val user = _currentSession.value?.user
        if (user != null) {
            auditDao.insertLog(
                AuditLogEntity(
                    operatorName = user.fullName,
                    operatorRole = "USER",
                    actionType = "LOGOUT",
                    entityType = "AUTH",
                    entityId = user.id.toString(),
                    details = "تسجيل خروج للمستخدم ${user.username}"
                )
            )
        }
        _currentSession.value = null
    }

    fun getAllUsers(): Flow<List<UserEntity>> = securityDao.getAllActiveUsers()

    suspend fun createUser(username: String, fullName: String, password: String, roleId: Long): Resource<Long> = withContext(Dispatchers.IO) {
        val current = _currentSession.value
        if (current == null || !current.hasPermission(Permissions.USERS_CREATE)) {
            return@withContext Resource.Error("غير مصرح لك بإنشاء مستخدمين جدد")
        }
        try {
            val user = UserEntity(
                username = username.trim(),
                passwordHash = PasswordHasher.hash(password),
                fullName = fullName.trim(),
                isActive = true
            )
            val newId = securityDao.insertUser(user)
            securityDao.assignRoleToUser(UserRoleEntity(userId = newId, roleId = roleId))
            Resource.Success(newId)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "فشل في إنشاء المستخدم")
        }
    }

    suspend fun changePassword(userId: Long, oldPass: String, newPass: String): Resource<Unit> = withContext(Dispatchers.IO) {
        val user = securityDao.getUserById(userId) ?: return@withContext Resource.Error("المستخدم غير موجود")
        if (user.passwordHash != PasswordHasher.hash(oldPass)) {
            return@withContext Resource.Error("كلمة المرور الحالية غير صحيحة")
        }
        val updated = user.copy(passwordHash = PasswordHasher.hash(newPass), updatedAt = System.currentTimeMillis())
        securityDao.updateUser(updated)
        Resource.Success(Unit)
    }

    suspend fun toggleUserActivation(userId: Long): Resource<Boolean> = withContext(Dispatchers.IO) {
        val current = _currentSession.value
        if (current == null || !current.hasPermission(Permissions.USERS_UPDATE)) {
            return@withContext Resource.Error("غير مصرح لك بتعديل حالة المستخدم")
        }
        val user = securityDao.getUserById(userId) ?: return@withContext Resource.Error("المستخدم غير موجود")
        val updated = user.copy(isActive = !user.isActive, updatedAt = System.currentTimeMillis())
        securityDao.updateUser(updated)
        Resource.Success(updated.isActive)
    }
}
