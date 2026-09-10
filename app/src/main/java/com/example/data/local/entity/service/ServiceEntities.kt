package com.example.data.local.entity.service

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.local.entity.reservation.StayEntity

@Entity(
    tableName = "service_categories",
    indices = [Index(value = ["categoryCode"], unique = true)]
)
data class ServiceCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryCode: String,
    val nameAr: String,
    val nameEn: String? = null,
    val isActive: Boolean = true
)

@Entity(
    tableName = "services",
    foreignKeys = [
        ForeignKey(
            entity = ServiceCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["serviceCode"], unique = true),
        Index(value = ["categoryId"])
    ]
)
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serviceCode: String,
    val categoryId: Long,
    val nameAr: String,
    val nameEn: String? = null,
    val defaultPriceMinor: Long,
    val taxPercentage: Double = 0.0,
    val isAvailable: Boolean = true,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

@Entity(
    tableName = "service_transactions",
    foreignKeys = [
        ForeignKey(
            entity = StayEntity::class,
            parentColumns = ["id"],
            childColumns = ["stayId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ServiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["serviceId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["stayId"]),
        Index(value = ["serviceId"]),
        Index(value = ["transactionTimestamp"])
    ]
)
data class ServiceTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stayId: Long,
    val serviceId: Long,
    val quantity: Int = 1,
    val unitPriceMinor: Long,
    val totalAmountMinor: Long,
    val notes: String? = null,
    val servedBy: String? = null,
    val transactionTimestamp: Long = System.currentTimeMillis()
)
