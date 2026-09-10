package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_config")
data class SystemConfigEntity(
    @PrimaryKey val configKey: String,
    val configValue: String,
    val description: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
