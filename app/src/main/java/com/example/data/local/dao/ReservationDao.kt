package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.reservation.CheckInRecordEntity
import com.example.data.local.entity.reservation.CheckOutRecordEntity
import com.example.data.local.entity.reservation.GuestEntity
import com.example.data.local.entity.reservation.ReservationEntity
import com.example.data.local.entity.reservation.ReservationItemEntity
import com.example.data.local.entity.reservation.StayEntity
import com.example.data.local.entity.reservation.StayGuestEntity
import com.example.data.local.entity.reservation.UnitAssignmentEntity
import com.example.data.local.relation.ReservationWithDetails
import com.example.data.local.relation.StayWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ReservationDao {

    // Reservations
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReservation(reservation: ReservationEntity): Long

    @Update
    suspend fun updateReservation(reservation: ReservationEntity)

    @Query("SELECT * FROM reservations WHERE isDeleted = 0 ORDER BY id DESC")
    fun getAllActiveReservations(): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM reservations WHERE id = :id")
    suspend fun getReservationById(id: Long): ReservationEntity?

    @Query("SELECT * FROM reservations WHERE reservationNumber = :number")
    suspend fun getReservationByNumber(number: String): ReservationEntity?

    @Transaction
    @Query("SELECT * FROM reservations WHERE id = :id")
    suspend fun getReservationWithDetails(id: Long): ReservationWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservationItem(item: ReservationItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservationItems(items: List<ReservationItemEntity>)

    @Query("SELECT * FROM reservation_items WHERE reservationId = :resId")
    suspend fun getItemsForReservation(resId: Long): List<ReservationItemEntity>

    // Conflict and Availability Check
    @Query("""
        SELECT COUNT(*) FROM unit_assignments ua
        WHERE ua.unitId = :unitId
          AND ua.status = 'ACTIVE'
          AND NOT (ua.endDate <= :startDate OR ua.startDate >= :endDate)
    """)
    suspend fun countConflictingAssignments(unitId: Long, startDate: Long, endDate: Long): Int

    @Query("""
        SELECT COUNT(*) FROM reservation_items ri
        INNER JOIN reservations r ON ri.reservationId = r.id
        WHERE ri.assignedUnitId = :unitId
          AND r.status IN ('CONFIRMED', 'CHECKED_IN')
          AND r.isDeleted = 0
          AND NOT (ri.checkOutDate <= :startDate OR ri.checkInDate >= :endDate)
          AND (:excludeReservationId IS NULL OR r.id != :excludeReservationId)
    """)
    suspend fun countConflictingReservations(unitId: Long, startDate: Long, endDate: Long, excludeReservationId: Long? = null): Int

    // Stays
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStay(stay: StayEntity): Long

    @Update
    suspend fun updateStay(stay: StayEntity)

    @Query("SELECT * FROM stays WHERE id = :id")
    suspend fun getStayById(id: Long): StayEntity?

    @Query("SELECT * FROM stays WHERE status = 'ACTIVE'")
    fun getActiveStays(): Flow<List<StayEntity>>

    @Transaction
    @Query("SELECT * FROM stays WHERE id = :id")
    suspend fun getStayWithDetails(id: Long): StayWithDetails?

    // Guests
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuest(guest: GuestEntity): Long

    @Query("SELECT * FROM guests WHERE id = :id")
    suspend fun getGuestById(id: Long): GuestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun linkGuestToStay(stayGuest: StayGuestEntity)

    // Unit Assignments
    @Query("SELECT * FROM unit_assignments WHERE unitId = :unitId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveAssignmentByUnit(unitId: Long): UnitAssignmentEntity?

    @Query("SELECT * FROM unit_assignments WHERE stayId = :stayId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveAssignmentForStay(stayId: Long): UnitAssignmentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUnitAssignment(assignment: UnitAssignmentEntity): Long

    @Update
    suspend fun updateUnitAssignment(assignment: UnitAssignmentEntity)

    // Check-in and Check-out Records
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckInRecord(record: CheckInRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckOutRecord(record: CheckOutRecordEntity): Long
}
