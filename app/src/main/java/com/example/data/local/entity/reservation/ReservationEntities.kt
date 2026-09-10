package com.example.data.local.entity.reservation

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.local.entity.customer.CustomerEntity
import com.example.data.local.entity.hotel.UnitEntity
import com.example.data.local.entity.hotel.UnitTypeEntity

@Entity(
    tableName = "reservations",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["reservationNumber"], unique = true),
        Index(value = ["customerId"]),
        Index(value = ["status"]),
        Index(value = ["expectedCheckIn"]),
        Index(value = ["expectedCheckOut"])
    ]
)
data class ReservationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reservationNumber: String, // e.g. RES-000001
    val customerId: Long,
    val expectedCheckIn: Long,
    val expectedCheckOut: Long,
    val status: String = "CONFIRMED", // PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED, NO_SHOW
    val totalAmountMinor: Long = 0L,
    val depositRequiredMinor: Long = 0L,
    val depositPaidMinor: Long = 0L,
    val adultsCount: Int = 1,
    val childrenCount: Int = 0,
    val sourceChannel: String = "DESK", // DESK, PHONE, WEBSITE, BOOKING_COM, AGODA, AGENT
    val specialRequests: String? = null,
    val cancellationReason: String? = null,
    val createdBy: String,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reservation_items",
    foreignKeys = [
        ForeignKey(
            entity = ReservationEntity::class,
            parentColumns = ["id"],
            childColumns = ["reservationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UnitTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["unitTypeId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = UnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["assignedUnitId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["reservationId"]),
        Index(value = ["unitTypeId"]),
        Index(value = ["assignedUnitId"])
    ]
)
data class ReservationItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reservationId: Long,
    val unitTypeId: Long,
    val assignedUnitId: Long? = null,
    val checkInDate: Long,
    val checkOutDate: Long,
    val ratePerNightMinor: Long,
    val nightsCount: Int,
    val subtotalMinor: Long,
    val discountMinor: Long = 0L,
    val taxMinor: Long = 0L,
    val totalMinor: Long
)

@Entity(
    tableName = "guests",
    indices = [
        Index(value = ["nationalIdNumber"]),
        Index(value = ["phone"])
    ]
)
data class GuestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val nationalIdType: String = "NATIONAL_ID",
    val nationalIdNumber: String? = null,
    val nationality: String = "اليمن",
    val phone: String? = null,
    val email: String? = null,
    val gender: String = "MALE", // MALE, FEMALE
    val isBlacklisted: Boolean = false,
    val blacklistReason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "stays",
    foreignKeys = [
        ForeignKey(
            entity = ReservationEntity::class,
            parentColumns = ["id"],
            childColumns = ["reservationId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["stayNumber"], unique = true),
        Index(value = ["reservationId"]),
        Index(value = ["customerId"]),
        Index(value = ["status"]),
        Index(value = ["actualCheckIn"]),
        Index(value = ["actualCheckOut"])
    ]
)
data class StayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stayNumber: String, // e.g. STY-000001
    val reservationId: Long? = null,
    val customerId: Long,
    val actualCheckIn: Long = System.currentTimeMillis(),
    val actualCheckOut: Long? = null,
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, CANCELLED
    val totalFolioChargesMinor: Long = 0L,
    val totalPaidMinor: Long = 0L,
    val balanceDueMinor: Long = 0L,
    val notes: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "stay_guests",
    primaryKeys = ["stayId", "guestId"],
    foreignKeys = [
        ForeignKey(
            entity = StayEntity::class,
            parentColumns = ["id"],
            childColumns = ["stayId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["guestId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["stayId"]),
        Index(value = ["guestId"])
    ]
)
data class StayGuestEntity(
    val stayId: Long,
    val guestId: Long,
    val isPrimaryGuest: Boolean = false
)

@Entity(
    tableName = "unit_assignments",
    foreignKeys = [
        ForeignKey(
            entity = StayEntity::class,
            parentColumns = ["id"],
            childColumns = ["stayId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["stayId"]),
        Index(value = ["unitId"]),
        Index(value = ["unitId", "startDate", "endDate"])
    ]
)
data class UnitAssignmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stayId: Long,
    val unitId: Long,
    val startDate: Long,
    val endDate: Long,
    val ratePerNightMinor: Long,
    val status: String = "ACTIVE" // ACTIVE, TRANSFERRED, COMPLETED
)

@Entity(
    tableName = "check_in_records",
    foreignKeys = [
        ForeignKey(
            entity = StayEntity::class,
            parentColumns = ["id"],
            childColumns = ["stayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["stayId"])]
)
data class CheckInRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stayId: Long,
    val checkInTime: Long = System.currentTimeMillis(),
    val operatorName: String,
    val depositCollectedMinor: Long = 0L,
    val notes: String? = null
)

@Entity(
    tableName = "check_out_records",
    foreignKeys = [
        ForeignKey(
            entity = StayEntity::class,
            parentColumns = ["id"],
            childColumns = ["stayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["stayId"])]
)
data class CheckOutRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stayId: Long,
    val checkOutTime: Long = System.currentTimeMillis(),
    val operatorName: String,
    val settlementTotalMinor: Long = 0L,
    val refundAmountMinor: Long = 0L,
    val notes: String? = null
)
