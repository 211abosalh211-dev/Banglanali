package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Audit Log Entity strictly implementing Phase 01 governance:
 * "سجل العمليات الحساسة: من؟ متى؟ ماذا فعل؟ ما الذي تغير؟"
 * And Soft Delete:
 * "السجلات المرتبطة بالتاريخ المالي لا تحذف فعليًا... استخدم active, inactive, archived, deletedAt"
 */
@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["entityType", "entityId"]),
        Index(value = ["isDeleted"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val operatorName: String,
    val operatorRole: String,
    val actionType: String,
    val entityType: String,
    val entityId: String,
    val details: String,
    val previousValue: String? = null,
    val newValue: String? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
