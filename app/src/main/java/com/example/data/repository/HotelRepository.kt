package com.example.data.repository

import androidx.room.withTransaction
import com.example.core.currency.Money
import com.example.core.result.Resource
import com.example.data.local.HotelDatabase
import com.example.data.local.entity.customer.CustomerEntity
import com.example.data.local.entity.hotel.UnitEntity
import com.example.data.local.entity.hotel.UnitStatusHistoryEntity
import com.example.data.local.entity.hotel.UnitTypeEntity
import com.example.data.local.entity.invoice.InvoiceEntity
import com.example.data.local.entity.invoice.InvoiceItemEntity
import com.example.data.local.entity.invoice.InvoicePaymentEntity
import com.example.data.local.entity.reservation.CheckInRecordEntity
import com.example.data.local.entity.reservation.CheckOutRecordEntity
import com.example.data.local.entity.reservation.GuestEntity
import com.example.data.local.entity.reservation.ReservationEntity
import com.example.data.local.entity.reservation.ReservationItemEntity
import com.example.data.local.entity.reservation.StayEntity
import com.example.data.local.entity.reservation.StayGuestEntity
import com.example.data.local.entity.reservation.UnitAssignmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class HotelRepository(
    private val database: HotelDatabase,
    private val accountingEngine: AccountingEngine
) {
    private val hotelUnitDao = database.hotelUnitDao()
    private val customerDao = database.customerDao()
    private val reservationDao = database.reservationDao()
    private val invoiceDao = database.invoiceDao()
    private val auditDao = database.auditLogDao()

    // 1. Units & Status
    fun getAllUnits(): Flow<List<UnitEntity>> = hotelUnitDao.getAllUnits()
    fun getAllUnitTypes(): Flow<List<UnitTypeEntity>> = hotelUnitDao.getAllUnitTypes()

    suspend fun createUnit(
        unitNumber: String,
        floor: Int,
        unitTypeId: Long,
        basePriceMinor: Long,
        description: String = ""
    ): Resource<Long> = withContext(Dispatchers.IO) {
        try {
            val unit = UnitEntity(
                unitNumber = unitNumber.trim(),
                unitTypeId = unitTypeId,
                floorNumber = floor,
                customNightlyPriceMinor = basePriceMinor,
                status = "VACANT_CLEAN",
                notes = description
            )
            val id = hotelUnitDao.insertUnit(unit)
            Resource.Success(id)
        } catch (e: Exception) {
            Resource.Error("فشل إنشاء الوحدة: ${e.message}")
        }
    }

    suspend fun updateUnitStatus(unitId: Long, newStatus: String, notes: String = ""): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val unit = hotelUnitDao.getUnitById(unitId) ?: return@withContext Resource.Error("الوحدة غير موجودة")
            hotelUnitDao.updateUnitStatus(unitId, newStatus)
            hotelUnitDao.insertStatusHistory(
                UnitStatusHistoryEntity(
                    unitId = unitId,
                    previousStatus = unit.status,
                    newStatus = newStatus,
                    changedBy = "موظف النظام",
                    reasonOrNotes = notes
                )
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("فشل تحديث حالة الوحدة: ${e.message}")
        }
    }

    // 2. Customers
    fun getAllCustomers(): Flow<List<CustomerEntity>> = customerDao.getAllActiveCustomers()

    suspend fun createCustomer(
        fullName: String,
        phone: String,
        nationalId: String = "",
        nationality: String = "يمني",
        email: String = ""
    ): Resource<Long> = withContext(Dispatchers.IO) {
        try {
            val code = "CUST-${System.currentTimeMillis() % 100000}"
            val customer = CustomerEntity(
                customerCode = code,
                fullName = fullName.trim(),
                phone = phone.trim(),
                nationalIdNumber = nationalId.trim(),
                nationality = nationality.trim(),
                email = email.trim()
            )
            val id = customerDao.insertCustomer(customer)
            Resource.Success(id)
        } catch (e: Exception) {
            Resource.Error("فشل إضافة العميل: ${e.message}")
        }
    }

    suspend fun deleteCustomerSafely(customerId: Long): Resource<Unit> = withContext(Dispatchers.IO) {
        val invoicesCount = customerDao.countCustomerInvoices(customerId)
        val reservationsCount = customerDao.countCustomerReservations(customerId)
        if (invoicesCount > 0 || reservationsCount > 0) {
            // Soft delete only - protects financial history!
            customerDao.softDeleteCustomer(customerId)
            Resource.Success(Unit)
        } else {
            customerDao.hardDeleteCustomer(customerId)
            Resource.Success(Unit)
        }
    }

    // 3. Reservations & Strict Double-Booking Prevention
    fun getAllReservations(): Flow<List<ReservationEntity>> = reservationDao.getAllActiveReservations()

    suspend fun createReservation(
        customerId: Long,
        unitId: Long,
        checkInDate: Long,
        checkOutDate: Long,
        totalAmountMinor: Long,
        depositAmountMinor: Long = 0L,
        notes: String = ""
    ): Resource<Long> = withContext(Dispatchers.IO) {
        // Prevent Double-Booking!
        val conflicts = reservationDao.countConflictingReservations(unitId, checkInDate, checkOutDate)
        val assignmentConflicts = reservationDao.countConflictingAssignments(unitId, checkInDate, checkOutDate)
        if (conflicts > 0 || assignmentConflicts > 0) {
            return@withContext Resource.Error("عذراً، الوحدة محجوزة أو مشغولة بالفعل خلال هذه الفترة الزمنية المحددة")
        }

        try {
            database.withTransaction {
                val resNumber = "RES-${System.currentTimeMillis() % 100000}"
                val reservation = ReservationEntity(
                    reservationNumber = resNumber,
                    customerId = customerId,
                    expectedCheckIn = checkInDate,
                    expectedCheckOut = checkOutDate,
                    status = "CONFIRMED",
                    totalAmountMinor = totalAmountMinor,
                    depositPaidMinor = depositAmountMinor,
                    specialRequests = notes,
                    createdBy = "SYSTEM"
                )
                val resId = reservationDao.insertReservation(reservation)
                val item = ReservationItemEntity(
                    reservationId = resId,
                    unitTypeId = 1L,
                    assignedUnitId = unitId,
                    checkInDate = checkInDate,
                    checkOutDate = checkOutDate,
                    ratePerNightMinor = totalAmountMinor,
                    nightsCount = 1,
                    subtotalMinor = totalAmountMinor,
                    totalMinor = totalAmountMinor
                )
                reservationDao.insertReservationItem(item)
                resId
            }.let { Resource.Success(it) }
        } catch (e: Exception) {
            Resource.Error("فشل إنشاء الحجز: ${e.message}")
        }
    }

    // 4. Check-in (تسجيل الوصول)
    suspend fun performCheckIn(
        reservationId: Long?,
        customerId: Long,
        unitId: Long,
        expectedCheckOutDate: Long,
        rateMinor: Long,
        operatorName: String
    ): Resource<Long> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                val unit = hotelUnitDao.getUnitById(unitId)
                    ?: throw IllegalStateException("الوحدة غير موجودة")
                if (unit.status == "OCCUPIED") {
                    throw IllegalStateException("الوحدة مشغولة بالفعل حالياً")
                }

                val stay = StayEntity(
                    stayNumber = "STY-${System.currentTimeMillis() % 100000}",
                    reservationId = reservationId,
                    customerId = customerId,
                    actualCheckIn = System.currentTimeMillis(),
                    status = "ACTIVE",
                    createdBy = operatorName
                )
                val stayId = reservationDao.insertStay(stay)

                val assignment = UnitAssignmentEntity(
                    stayId = stayId,
                    unitId = unitId,
                    startDate = System.currentTimeMillis(),
                    endDate = expectedCheckOutDate,
                    ratePerNightMinor = rateMinor,
                    status = "ACTIVE"
                )
                reservationDao.insertUnitAssignment(assignment)

                reservationDao.insertCheckInRecord(
                    CheckInRecordEntity(
                        stayId = stayId,
                        checkInTime = System.currentTimeMillis(),
                        operatorName = operatorName
                    )
                )

                // Update Unit Status to OCCUPIED
                hotelUnitDao.updateUnitStatus(unitId, "OCCUPIED")
                hotelUnitDao.insertStatusHistory(
                    UnitStatusHistoryEntity(
                        unitId = unitId,
                        previousStatus = unit.status,
                        newStatus = "OCCUPIED",
                        reasonOrNotes = "تسجيل وصول للإقامة رقم $stayId",
                        changedBy = operatorName
                    )
                )

                if (reservationId != null) {
                    val res = reservationDao.getReservationById(reservationId)
                    if (res != null) {
                        reservationDao.updateReservation(res.copy(status = "CHECKED_IN"))
                    }
                }

                stayId
            }.let { Resource.Success(it) }
        } catch (e: Exception) {
            Resource.Error("فشل تسجيل الوصول: ${e.message}")
        }
    }

    // 5. Transfer Stay (نقل النزيل بين الغرف مع الحفاظ على التاريخ)
    suspend fun transferGuestStay(
        stayId: Long,
        oldUnitId: Long,
        newUnitId: Long,
        newRateMinor: Long,
        operatorName: String,
        reason: String
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                val newUnit = hotelUnitDao.getUnitById(newUnitId)
                    ?: throw IllegalStateException("الوحدة الجديدة غير موجودة")
                if (newUnit.status == "OCCUPIED") {
                    throw IllegalStateException("الوحدة الجديدة مشغولة حالياً")
                }

                val now = System.currentTimeMillis()
                // Complete old assignment
                database.openHelper.writableDatabase.execSQL(
                    "UPDATE unit_assignments SET status = 'COMPLETED', endDate = $now WHERE stayId = $stayId AND unitId = $oldUnitId AND status = 'ACTIVE'"
                )

                // Insert new assignment
                val newAssignment = UnitAssignmentEntity(
                    stayId = stayId,
                    unitId = newUnitId,
                    startDate = now,
                    endDate = now + 86400000L,
                    ratePerNightMinor = newRateMinor,
                    status = "ACTIVE"
                )
                reservationDao.insertUnitAssignment(newAssignment)

                // Update unit statuses
                hotelUnitDao.updateUnitStatus(oldUnitId, "CLEANING")
                hotelUnitDao.updateUnitStatus(newUnitId, "OCCUPIED")

                auditDao.insertLog(
                    com.example.data.local.entity.AuditLogEntity(
                        operatorName = operatorName,
                        operatorRole = "RECEPTIONIST",
                        actionType = "TRANSFER",
                        entityType = "STAY",
                        entityId = stayId.toString(),
                        details = "نقل النزيل من الوحدة $oldUnitId إلى $newUnitId: $reason"
                    )
                )
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("فشل نقل النزيل: ${e.message}")
        }
    }

    // 6. Check-Out & Final Financial Settlement
    suspend fun performCheckOut(
        stayId: Long,
        cashboxId: Long,
        paymentMethodCode: String,
        operatorName: String
    ): Resource<InvoiceEntity> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                val stay = reservationDao.getStayById(stayId)
                    ?: reservationDao.getActiveAssignmentByUnit(stayId)?.let { reservationDao.getStayById(it.stayId) }
                    ?: throw IllegalStateException("الإقامة غير موجودة")
                val resolvedStayId = stay.id
                val activeAssignment = reservationDao.getActiveAssignmentForStay(resolvedStayId)
                val unitId = activeAssignment?.unitId ?: (if (stayId != resolvedStayId) stayId else 1L)
                val unit = hotelUnitDao.getUnitById(unitId)
                    ?: throw IllegalStateException("الوحدة غير موجودة")

                val durationMillis = System.currentTimeMillis() - stay.actualCheckIn
                val oneDayMillis = 1000L * 60L * 60L * 24L
                val fullDays = durationMillis / oneDayMillis
                val remainder = if (durationMillis % oneDayMillis > 0L) 1L else 0L
                val nights = maxOf(1L, fullDays + remainder)
                val nightlyRate = activeAssignment?.ratePerNightMinor ?: unit.customNightlyPriceMinor ?: 1500000L
                val totalRoomChargesMinor = nightlyRate * nights

                // Create Final Invoice
                val invoiceNumber = "INV-${System.currentTimeMillis() % 100000}"
                val invoice = InvoiceEntity(
                    invoiceNumber = invoiceNumber,
                    customerId = stay.customerId,
                    stayId = stayId,
                    issueDate = System.currentTimeMillis(),
                    dueDate = System.currentTimeMillis(),
                    subtotalMinor = totalRoomChargesMinor,
                    totalAmountMinor = totalRoomChargesMinor,
                    paidAmountMinor = totalRoomChargesMinor,
                    balanceDueMinor = 0L,
                    status = "PAID",
                    createdBy = operatorName
                )
                val invoiceId = invoiceDao.insertInvoice(invoice)

                val item = InvoiceItemEntity(
                    invoiceId = invoiceId,
                    itemType = "ROOM_STAY",
                    description = "إقامة الغرفة ${unit.unitNumber} لمدة $nights ليالٍ",
                    quantity = nights.toInt(),
                    unitPriceMinor = nightlyRate,
                    subtotalMinor = totalRoomChargesMinor,
                    totalMinor = totalRoomChargesMinor
                )
                invoiceDao.insertInvoiceItems(listOf(item))

                val payment = InvoicePaymentEntity(
                    invoiceId = invoiceId,
                    paymentReferenceNumber = "REC-CHK-$invoiceNumber",
                    amountPaidMinor = totalRoomChargesMinor,
                    paymentMethod = paymentMethodCode,
                    cashboxId = cashboxId,
                    paymentDate = System.currentTimeMillis(),
                    receivedBy = operatorName
                )
                invoiceDao.insertInvoicePayment(payment)

                // Receipt voucher with accounting & cashbox
                accountingEngine.createReceiptVoucher(
                    receiptNumber = "REC-CHK-$invoiceNumber",
                    customerId = stay.customerId,
                    receivedFrom = "تسوية مغادرة إقامة $stayId",
                    amountMinor = totalRoomChargesMinor,
                    paymentMethodCode = paymentMethodCode,
                    cashboxId = cashboxId,
                    creditAccountId = 8L, // 40101 إيرادات إقامة الغرف
                    referenceNo = invoiceNumber,
                    notes = "سداد فاتورة المغادرة $invoiceNumber",
                    invoiceId = invoiceId
                )

                // Complete stay & release unit
                reservationDao.updateStay(
                    stay.copy(
                        status = "COMPLETED",
                        actualCheckOut = System.currentTimeMillis()
                    )
                )
                if (activeAssignment != null) {
                    reservationDao.updateUnitAssignment(
                        activeAssignment.copy(
                            status = "COMPLETED",
                            endDate = System.currentTimeMillis()
                        )
                    )
                }
                reservationDao.insertCheckOutRecord(
                    CheckOutRecordEntity(
                        stayId = stayId,
                        checkOutTime = System.currentTimeMillis(),
                        operatorName = operatorName,
                        settlementTotalMinor = totalRoomChargesMinor,
                        refundAmountMinor = 0L,
                        notes = "مغادرة النزيل وتسوية الحساب بالكامل"
                    )
                )

                // Unit goes to CLEANING status
                hotelUnitDao.updateUnitStatus(unit.id, "CLEANING")
                hotelUnitDao.insertStatusHistory(
                    UnitStatusHistoryEntity(
                        unitId = unit.id,
                        previousStatus = "OCCUPIED",
                        newStatus = "CLEANING",
                        reasonOrNotes = "مغادرة النزيل وتسوية الحساب بالكامل",
                        changedBy = operatorName
                    )
                )

                invoice.copy(id = invoiceId)
            }.let { Resource.Success(it) }
        } catch (e: Exception) {
            Resource.Error("فشل إنهاء الإقامة والمغادرة: ${e.message}")
        }
    }
}
