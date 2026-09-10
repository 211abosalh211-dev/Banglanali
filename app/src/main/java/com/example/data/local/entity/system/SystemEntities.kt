package com.example.data.local.entity.system

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["email"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val fullName: String,
    val email: String? = null,
    val phone: String? = null,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "roles",
    indices = [Index(value = ["roleCode"], unique = true)]
)
data class RoleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val roleCode: String,
    val roleNameAr: String,
    val description: String? = null,
    val isSystemRole: Boolean = false,
    val isActive: Boolean = true
)

@Entity(
    tableName = "permissions",
    indices = [Index(value = ["permissionCode"], unique = true)]
)
data class PermissionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val permissionCode: String,
    val moduleName: String,
    val descriptionAr: String
)

@Entity(
    tableName = "role_permissions",
    primaryKeys = ["roleId", "permissionId"],
    indices = [
        Index(value = ["roleId"]),
        Index(value = ["permissionId"])
    ]
)
data class RolePermissionEntity(
    val roleId: Long,
    val permissionId: Long
)

@Entity(
    tableName = "user_roles",
    primaryKeys = ["userId", "roleId"],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["roleId"])
    ]
)
data class UserRoleEntity(
    val userId: Long,
    val roleId: Long
)
