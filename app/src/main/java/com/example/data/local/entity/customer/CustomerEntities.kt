package com.example.data.local.entity.customer

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["customerCode"], unique = true),
        Index(value = ["phone"]),
        Index(value = ["nationalIdNumber"]),
        Index(value = ["isDeleted"])
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerCode: String, // e.g. CUST-0001
    val fullName: String,
    val phone: String,
    val email: String? = null,
    val nationalIdType: String = "NATIONAL_ID", // NATIONAL_ID, PASSPORT, RESIDENCE
    val nationalIdNumber: String? = null,
    val nationality: String = "اليمن",
    val address: String? = null,
    val companyName: String? = null,
    val taxNumber: String? = null,
    val currentBalanceMinor: Long = 0L, // Positive = Receivable (دين على العميل), Negative = Credit (رصيد دائن للعميل)
    val creditLimitMinor: Long = 0L,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customer_notes",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class CustomerNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val noteText: String,
    val noteType: String = "GENERAL", // GENERAL, PREFERENCE, VIP, ALERT
    val createdBy: String = "USER",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customer_documents",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class CustomerDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val docType: String, // PASSPORT, ID_CARD, DRIVING_LICENSE, CONTRACT
    val docNumber: String,
    val fileUri: String? = null,
    val issueDate: String? = null,
    val expiryDate: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
