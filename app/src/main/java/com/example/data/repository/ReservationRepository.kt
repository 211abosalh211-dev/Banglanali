package com.example.data.repository

import androidx.room.withTransaction
import com.example.core.result.Resource
import com.example.data.local.HotelDatabase
import com.example.data.local.entity.reservation.CheckInRecordEntity
import com.example.data.local.entity.reservation.ReservationEntity
import com.example.data.local.entity.reservation.ReservationItemEntity
import com.example.data.local.entity.reservation.StayEntity
import com.example.data.local.entity.reservation.StayGuestEntity
import com.example.data.local.entity.reservation.UnitAssignmentEntity
import com.example.data.local.relation.ReservationWithDetails
import com.example.data.local.relation.StayWithDetails
import kotlinx.coroutines.flow.Flow

class ReservationRepository(private val database: HotelDatabase) {

    private val reservationDao = database.reservationDao()
    private val hotelUnitDao = database.hotelUnitDao()

    fun getAllActiveReservations(): Flow<List<ReservationEntity>> = reservationDao.getAllActiveReservations()

    suspend fun getReservationWithDetails(id: Long): ReservationWithDetails? =
        reservationDao.getReservationWithDetails(id)

    suspend fun getStayWithDetails(id: Long): StayWithDetails? =
        reservationDao.getStayWithDetails(id)

    /**
     * Integrity rule:
     * Prevent double bookings for the same unit during overlapping time frames!
     */
    suspend fun createReservation(
        reservation: ReservationEntity,
        items: List<ReservationItemEntity>
    ): Resource<Long> {
        if (items.isEmpty()) {
            return Resource.Error("يجب أن يحتوي الحجز على وحدة واحدة على الأقل")
        }

        // Validate availability for each assigned unit
        for (item in items) {
            val assignedUnit = item.assignedUnitId
            if (assignedUnit != null) {
                val conflictingRes = reservationDao.countConflictingReservations(
                    unitId = assignedUnit,
                    startDate = item.checkInDate,
                    endDate = item.checkOutDate
                )
                val conflictingStay = reservationDao.countConflictingAssignments(
                    unitId = assignedUnit,
                    startDate = item.checkInDate,
                    endDate = item.checkOutDate
                )

                if (conflictingRes > 0 || conflictingStay > 0) {
                    val unit = hotelUnitDao.getUnitById(assignedUnit)
                    val unitName = unit?.unitNumber ?: "#$assignedUnit"
                    return Resource.Error("الوحدة $unitName محجوزة أو مشغولة بالفعل خلال هذه الفترة الزمنية! لا يمكن التعارض.")
                }
            }
        }

        return try {
            val id = database.withTransaction {
                val resId = reservationDao.insertReservation(reservation)
                val assignedItems = items.map { it.copy(reservationId = resId) }
                reservationDao.insertReservationItems(assignedItems)
                resId
            }
            Resource.Success(id)
        } catch (e: Exception) {
            Resource.Error("فشل في إنشاء الحجز: ${e.localizedMessage}")
        }
    }

    /**
     * Check-in Transaction:
     * Atomic creation of Stay, UnitAssignment, CheckInRecord, and updating Unit status to OCCUPIED.
     */
    suspend fun checkInGuest(
        stay: StayEntity,
        guestIds: List<Long>,
        primaryGuestId: Long,
        assignedUnitId: Long,
        startDate: Long,
        endDate: Long,
        ratePerNightMinor: Long,
        operatorName: String,
        depositCollectedMinor: Long
    ): Resource<Long> {
        return try {
            val stayId = database.withTransaction {
                // Verify unit is available for assignment
                val conflicts = reservationDao.countConflictingAssignments(assignedUnitId, startDate, endDate)
                if (conflicts > 0) {
                    throw IllegalStateException("الوحدة مشغولة بالفعل في هذا التوقيت")
                }

                val newStayId = reservationDao.insertStay(stay)

                // Link guests
                for (gId in guestIds) {
                    reservationDao.linkGuestToStay(
                        StayGuestEntity(
                            stayId = newStayId,
                            guestId = gId,
                            isPrimaryGuest = (gId == primaryGuestId)
                        )
                    )
                }

                // Create Unit Assignment
                reservationDao.insertUnitAssignment(
                    UnitAssignmentEntity(
                        stayId = newStayId,
                        unitId = assignedUnitId,
                        startDate = startDate,
                        endDate = endDate,
                        ratePerNightMinor = ratePerNightMinor,
                        status = "ACTIVE"
                    )
                )

                // Update Unit status to OCCUPIED
                hotelUnitDao.updateUnitStatus(assignedUnitId, "OCCUPIED")

                // Insert Check-in Record
                reservationDao.insertCheckInRecord(
                    CheckInRecordEntity(
                        stayId = newStayId,
                        operatorName = operatorName,
                        depositCollectedMinor = depositCollectedMinor
                    )
                )

                newStayId
            }
            Resource.Success(stayId)
        } catch (e: Exception) {
            Resource.Error("فشلت عملية تسكين النزيل: ${e.localizedMessage}")
        }
    }
}
