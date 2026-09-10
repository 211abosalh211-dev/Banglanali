package com.example.data.local.entity.hotel

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "unit_types",
    indices = [Index(value = ["typeCode"], unique = true)]
)
data class UnitTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val typeCode: String, // e.g. SUITE_ROYAL, DELUXE_DOUBLE, CHALET_VIP
    val nameAr: String,
    val nameEn: String,
    val basePriceMinor: Long, // Nightly rate in minor units
    val defaultMaxGuests: Int = 2,
    val description: String? = null,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

@Entity(
    tableName = "units",
    foreignKeys = [
        ForeignKey(
            entity = UnitTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["unitTypeId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["unitNumber"], unique = true),
        Index(value = ["unitTypeId"]),
        Index(value = ["status"]),
        Index(value = ["floorNumber"])
    ]
)
data class UnitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val unitNumber: String, // e.g. "101", "205", "CH-12"
    val unitTypeId: Long,
    val floorNumber: Int = 1,
    val buildingWing: String? = "Main",
    val status: String = "VACANT_CLEAN", // VACANT_CLEAN, VACANT_DIRTY, OCCUPIED, UNDER_MAINTENANCE, OUT_OF_ORDER
    val customNightlyPriceMinor: Long? = null, // Overrides unit type price if set
    val capacityAdults: Int = 2,
    val capacityChildren: Int = 1,
    val notes: String? = null,
    val isSmoking: Boolean = false,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "unit_features",
    indices = [Index(value = ["featureCode"], unique = true)]
)
data class UnitFeatureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val featureCode: String, // e.g. SEA_VIEW, BALCONY, JACUZZI, HIGH_FLOOR
    val nameAr: String,
    val iconName: String? = null,
    val additionalChargeMinor: Long = 0L
)

@Entity(
    tableName = "unit_type_features",
    primaryKeys = ["unitTypeId", "featureId"],
    foreignKeys = [
        ForeignKey(
            entity = UnitTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["unitTypeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UnitFeatureEntity::class,
            parentColumns = ["id"],
            childColumns = ["featureId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["unitTypeId"]),
        Index(value = ["featureId"])
    ]
)
data class UnitTypeFeatureEntity(
    val unitTypeId: Long,
    val featureId: Long
)

@Entity(
    tableName = "unit_status_history",
    foreignKeys = [
        ForeignKey(
            entity = UnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["unitId"]),
        Index(value = ["timestamp"])
    ]
)
data class UnitStatusHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val unitId: Long,
    val previousStatus: String,
    val newStatus: String,
    val reasonOrNotes: String? = null,
    val changedBy: String,
    val timestamp: Long = System.currentTimeMillis()
)
