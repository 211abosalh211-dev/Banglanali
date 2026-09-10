package com.example.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.data.local.entity.accounting.AccountEntity
import com.example.data.local.entity.accounting.JournalEntryEntity
import com.example.data.local.entity.accounting.JournalEntryLineEntity
import com.example.data.local.entity.customer.CustomerDocumentEntity
import com.example.data.local.entity.customer.CustomerEntity
import com.example.data.local.entity.customer.CustomerNoteEntity
import com.example.data.local.entity.hotel.UnitEntity
import com.example.data.local.entity.hotel.UnitFeatureEntity
import com.example.data.local.entity.hotel.UnitTypeEntity
import com.example.data.local.entity.hotel.UnitTypeFeatureEntity
import com.example.data.local.entity.inventory.CategoryEntity
import com.example.data.local.entity.inventory.ProductEntity
import com.example.data.local.entity.inventory.StockBalanceEntity
import com.example.data.local.entity.invoice.InvoiceEntity
import com.example.data.local.entity.invoice.InvoiceItemEntity
import com.example.data.local.entity.invoice.InvoicePaymentEntity
import com.example.data.local.entity.reservation.GuestEntity
import com.example.data.local.entity.reservation.ReservationEntity
import com.example.data.local.entity.reservation.ReservationItemEntity
import com.example.data.local.entity.reservation.StayEntity
import com.example.data.local.entity.reservation.StayGuestEntity
import com.example.data.local.entity.reservation.UnitAssignmentEntity

data class JournalEntryWithLines(
    @Embedded val entry: JournalEntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "entryId"
    )
    val lines: List<JournalEntryLineEntity>
)

data class InvoiceWithDetails(
    @Embedded val invoice: InvoiceEntity,
    @Relation(
        parentColumn = "customerId",
        entityColumn = "id"
    )
    val customer: CustomerEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<InvoiceItemEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val payments: List<InvoicePaymentEntity>
)

data class CustomerWithDetails(
    @Embedded val customer: CustomerEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "customerId"
    )
    val notes: List<CustomerNoteEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "customerId"
    )
    val documents: List<CustomerDocumentEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "customerId"
    )
    val invoices: List<InvoiceEntity>
)

data class ReservationWithDetails(
    @Embedded val reservation: ReservationEntity,
    @Relation(
        parentColumn = "customerId",
        entityColumn = "id"
    )
    val customer: CustomerEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "reservationId"
    )
    val items: List<ReservationItemEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "reservationId"
    )
    val stays: List<StayEntity>
)

data class StayWithDetails(
    @Embedded val stay: StayEntity,
    @Relation(
        parentColumn = "customerId",
        entityColumn = "id"
    )
    val customer: CustomerEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = StayGuestEntity::class,
            parentColumn = "stayId",
            entityColumn = "guestId"
        )
    )
    val guests: List<GuestEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "stayId"
    )
    val unitAssignments: List<UnitAssignmentEntity>
)

data class UnitTypeWithFeatures(
    @Embedded val unitType: UnitTypeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = UnitTypeFeatureEntity::class,
            parentColumn = "unitTypeId",
            entityColumn = "featureId"
        )
    )
    val features: List<UnitFeatureEntity>
)

data class ProductWithDetails(
    @Embedded val product: ProductEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "productId"
    )
    val balances: List<StockBalanceEntity>
)
