package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.currency.Money
import com.example.data.local.HotelDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.SystemConfigEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FoundationUnitTest {

    private lateinit var database: HotelDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, HotelDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `test money precision zero floating point error`() {
        // 35,000.75 * 3 = 105,002.25
        val nightlyRate = Money.fromString("35000.75", "YER")
        val nights = 3L
        val totalStay = nightlyRate * nights
        assertEquals(10500225L, totalStay.minorUnits)
        assertEquals("105,002.25", totalStay.toDisplayString())

        // Minus discount 5,000.25 = 100,002.00
        val discount = Money.fromString("5000.25", "YER")
        val netStay = totalStay - discount
        assertEquals(10000200L, netStay.minorUnits)

        // Add tax 5,000.10 = 105,002.10
        val tax = Money.fromString("5000.10", "YER")
        val grandTotal = netStay + tax
        assertEquals(10500210L, grandTotal.minorUnits)
        assertEquals("105,002.10 ر.ي", grandTotal.formatWithCurrency(arabicSymbol = true))
    }

    @Test
    fun `test room database seeding and system config dao`() = runTest {
        database.seedDefaultConfigurations()

        val configs = database.systemConfigDao().getAllConfigs().first()
        assertTrue(configs.isNotEmpty())

        val hotelName = database.systemConfigDao().getValue("hotel_name")
        assertEquals("فندق البرج الذهبي الملكي", hotelName)

        val currency = database.systemConfigDao().getValue("currency_code")
        assertEquals("YER", currency)

        // Test updating config
        database.systemConfigDao().insertOrUpdate(
            SystemConfigEntity(
                configKey = "hotel_name",
                configValue = "فندق البرج الذهبي الملكي - الفرع الرئيسي",
                description = "تم التحديث"
            )
        )
        val updatedName = database.systemConfigDao().getValue("hotel_name")
        assertEquals("فندق البرج الذهبي الملكي - الفرع الرئيسي", updatedName)
    }

    @Test
    fun `test audit log dao and soft delete governance`() = runTest {
        val auditDao = database.auditLogDao()

        val log = AuditLogEntity(
            operatorName = "أحمد المحاسب",
            operatorRole = "ACCOUNTANT",
            actionType = "RECEIVE",
            entityType = "CASHBOX",
            entityId = "CB-001",
            details = "سند قبض رقم #1001 بمبلغ 25,000 ريال",
            previousValue = null,
            newValue = "25000.00 YER"
        )

        val id = auditDao.insertLog(log)
        assertTrue(id > 0)

        val activeLogsBefore = auditDao.getAllActiveLogs().first()
        assertEquals(1, activeLogsBefore.size)
        assertEquals("أحمد المحاسب", activeLogsBefore.first().operatorName)
        assertFalse(activeLogsBefore.first().isDeleted)

        // Test Soft Delete: Records are NOT permanently removed from DB
        auditDao.softDeleteLog(id, System.currentTimeMillis())

        val activeLogsAfter = auditDao.getAllActiveLogs().first()
        assertEquals(0, activeLogsAfter.size) // Hidden from active list

        val activeCount = auditDao.countActiveLogs().first()
        assertEquals(0, activeCount)
    }
}
