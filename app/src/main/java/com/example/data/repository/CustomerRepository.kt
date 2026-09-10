package com.example.data.repository

import com.example.core.result.Resource
import com.example.data.local.HotelDatabase
import com.example.data.local.entity.customer.CustomerEntity
import com.example.data.local.relation.CustomerWithDetails
import kotlinx.coroutines.flow.Flow

class CustomerRepository(private val database: HotelDatabase) {

    private val customerDao = database.customerDao()

    fun getAllActiveCustomers(): Flow<List<CustomerEntity>> = customerDao.getAllActiveCustomers()

    suspend fun getCustomerById(id: Long): CustomerEntity? = customerDao.getCustomerById(id)

    suspend fun getCustomerWithDetails(id: Long): CustomerWithDetails? =
        customerDao.getCustomerWithDetails(id)

    suspend fun createCustomer(customer: CustomerEntity): Resource<Long> {
        return try {
            val id = customerDao.insertCustomer(customer)
            Resource.Success(id)
        } catch (e: Exception) {
            Resource.Error("فشل في إضافة العميل: ${e.localizedMessage}")
        }
    }

    /**
     * Financial Protection:
     * If customer has invoices or reservations, REJECT hard delete.
     * Soft-delete instead.
     */
    suspend fun safeDeleteCustomer(customerId: Long): Resource<Boolean> {
        val invoiceCount = customerDao.countCustomerInvoices(customerId)
        val reservationCount = customerDao.countCustomerReservations(customerId)

        return if (invoiceCount > 0 || reservationCount > 0) {
            customerDao.softDeleteCustomer(customerId)
            Resource.Error(
                "تم رفض الحذف النهائي للعميل لوجود $invoiceCount فواتير و $reservationCount حجوزات مرتبطة به. تم تحويل العميل إلى مؤرشف (Soft Delete) لحماية السجلات المالية."
            )
        } else {
            try {
                customerDao.hardDeleteCustomer(customerId)
                Resource.Success(true)
            } catch (e: Exception) {
                // In case Foreign Key constraint in SQLite caught it
                customerDao.softDeleteCustomer(customerId)
                Resource.Error("تم رفض الحذف النهائي بسبب ارتباطات في قاعدة البيانات: ${e.localizedMessage}")
            }
        }
    }
}
