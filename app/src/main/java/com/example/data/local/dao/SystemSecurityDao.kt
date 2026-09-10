package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.system.PermissionEntity
import com.example.data.local.entity.system.RoleEntity
import com.example.data.local.entity.system.RolePermissionEntity
import com.example.data.local.entity.system.UserEntity
import com.example.data.local.entity.system.UserRoleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemSecurityDao {

    // Users
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE isDeleted = 0 ORDER BY id ASC")
    fun getAllActiveUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username AND isDeleted = 0")
    suspend fun getUserByUsername(username: String): UserEntity?

    // Roles
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRole(role: RoleEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRoles(roles: List<RoleEntity>)

    @Query("SELECT * FROM roles WHERE isActive = 1")
    fun getAllRoles(): Flow<List<RoleEntity>>

    // Permissions
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPermission(permission: PermissionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPermissions(permissions: List<PermissionEntity>)

    @Query("SELECT * FROM permissions ORDER BY moduleName ASC")
    fun getAllPermissions(): Flow<List<PermissionEntity>>

    // User Roles & Permissions
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun assignRoleToUser(userRole: UserRoleEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun assignPermissionToRole(rolePermission: RolePermissionEntity)

    @Query("""
        SELECT p.permissionCode FROM permissions p
        INNER JOIN role_permissions rp ON p.id = rp.permissionId
        INNER JOIN user_roles ur ON rp.roleId = ur.roleId
        WHERE ur.userId = :userId
    """)
    suspend fun getPermissionsForUser(userId: Long): List<String>

    @Query("""
        SELECT r.* FROM roles r
        INNER JOIN user_roles ur ON r.id = ur.roleId
        WHERE ur.userId = :userId AND r.isActive = 1
    """)
    suspend fun getRolesForUser(userId: Long): List<RoleEntity>
}
