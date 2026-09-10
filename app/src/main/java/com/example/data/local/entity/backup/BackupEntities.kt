package com.example.data.local.entity.backup

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "backup_metadata",
    indices = [
        Index(value = ["backupFileName"], unique = true),
        Index(value = ["createdAt"])
    ]
)
data class BackupMetadataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val backupFileName: String,
    val backupSizeBytes: Long,
    val totalRecordsArchived: Long,
    val checksumSha256: String,
    val backupType: String = "FULL_OFFLINE", // FULL_OFFLINE, DIFFERENTIAL, SYSTEM_CONFIG
    val status: String = "COMPLETED", // IN_PROGRESS, COMPLETED, FAILED
    val notes: String? = null,
    val createdBy: String = "SYSTEM",
    val createdAt: Long = System.currentTimeMillis()
)
