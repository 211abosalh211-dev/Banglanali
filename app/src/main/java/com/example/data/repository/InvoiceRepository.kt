package com.example.data.repository

import androidx.room.withTransaction
import com.example.core.result.Resource
import com.example.data.local.HotelDatabase
import com.example.data.local.entity.accounting.JournalEntryEntity
import com.example.data.local.entity.accounting.JournalEntryLineEntity
import com.example.data.local.entity.cashbox.CashboxTransactionEntity
import com.example.data.local.entity.invoice.InvoiceEntity
import com.example.data.local.entity.invoice.InvoiceItemEntity
import com.example.data.local.entity.invoice.InvoicePaymentEntity
import com.example.data.local.relation.InvoiceWithDetails
import kotlinx.coroutines.flow.Flow

class InvoiceRepository(private val database: HotelDatabase) {

    private val invoiceDao = database.invoiceDao()
    private val cashboxDao = database.cashboxDao()
    private val accountingDao = database.accountingDao()

    fun getAllActiveInvoices(): Flow<List<InvoiceEntity>> = invoiceDao.getAllActiveInvoices()

    suspend fun getInvoiceWithDetails(id: Long): InvoiceWithDetails? =
        invoiceDao.getInvoiceWithDetails(id)

    /**
     * Composite Atomic Transaction:
     * 1. Create Invoice & Items
     * 2. If immediate payment provided:
     *    - Insert InvoicePayment
     *    - Record CashboxTransaction & update Cashbox balance
     *    - Create and Post Balanced Journal Entry (Debit: Cashbox GL, Credit: Revenue GL)
     * All steps succeed together or roll back completely without partial state!
     */
    suspend fun issueInvoiceAtomic(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        paidAmountMinor: Long = 0L,
        cashboxId: Long? = null,
        paymentMethod: String = "CASH",
        cashboxGlAccountId: Long? = null,
        revenueGlAccountId: Long? = null,
        operatorName: String = "ADMIN"
    ): Resource<Long> {
        if (items.isEmpty()) {
            return Resource.Error("لا يمكن إصدار فاتورة بدون بنود")
        }

        return try {
            val invoiceId = database.withTransaction {
                // 1. Insert Invoice
                val invId = invoiceDao.insertInvoice(invoice)

                // 2. Insert Items
                val assignedItems = items.map { it.copy(invoiceId = invId) }
                invoiceDao.insertInvoiceItems(assignedItems)

                // 3. Process Payment if paidAmount > 0
                if (paidAmountMinor > 0L) {
                    if (cashboxId == null) {
                        throw IllegalStateException("يجب تحديد الصندوق لاستلام المبلغ المدفوع")
                    }

                    val cashbox = cashboxDao.getCashboxById(cashboxId)
                        ?: throw IllegalStateException("الصندوق المحدد غير موجود")

                    val receiptNumber = "REC-${invoice.invoiceNumber}"

                    // Insert Invoice Payment
                    invoiceDao.insertInvoicePayment(
                        InvoicePaymentEntity(
                            invoiceId = invId,
                            paymentReferenceNumber = receiptNumber,
                            amountPaidMinor = paidAmountMinor,
                            paymentMethod = paymentMethod,
                            cashboxId = cashboxId,
                            receivedBy = operatorName,
                            notes = "سداد فاتورة رقم ${invoice.invoiceNumber}"
                        )
                    )

                    // Update invoice paid & balance due
                    invoiceDao.recordPaymentOnInvoice(invId, paidAmountMinor)

                    // Record Cashbox Transaction
                    val newCashboxBalance = cashbox.currentBalanceMinor + paidAmountMinor
                    cashboxDao.insertTransaction(
                        CashboxTransactionEntity(
                            transactionNumber = "TX-${invoice.invoiceNumber}",
                            cashboxId = cashboxId,
                            transactionType = "RECEIPT",
                            amountMinor = paidAmountMinor,
                            direction = "IN",
                            balanceAfterMinor = newCashboxBalance,
                            referenceType = "INVOICE",
                            referenceId = invoice.invoiceNumber,
                            description = "سداد فاتورة ${invoice.invoiceNumber}",
                            operatorName = operatorName
                        )
                    )
                    cashboxDao.updateBalance(cashboxId, paidAmountMinor)

                    // 4. Create Balanced Journal Entry
                    if (cashboxGlAccountId != null && revenueGlAccountId != null) {
                        val entryNumber = "JV-${invoice.invoiceNumber}"
                        val jvId = accountingDao.insertJournalEntry(
                            JournalEntryEntity(
                                entryNumber = entryNumber,
                                date = System.currentTimeMillis(),
                                description = "قيد استحقاق وسداد فاتورة رقم ${invoice.invoiceNumber}",
                                referenceType = "INVOICE",
                                referenceId = invoice.invoiceNumber,
                                status = "POSTED",
                                totalDebitMinor = paidAmountMinor,
                                totalCreditMinor = paidAmountMinor,
                                createdBy = operatorName
                            )
                        )

                        // Debit: Cashbox, Credit: Revenue
                        accountingDao.insertJournalLines(
                            listOf(
                                JournalEntryLineEntity(
                                    entryId = jvId,
                                    accountId = cashboxGlAccountId,
                                    debit = paidAmountMinor,
                                    credit = 0L,
                                    description = "سداد فاتورة نقداً - ${invoice.invoiceNumber}"
                                ),
                                JournalEntryLineEntity(
                                    entryId = jvId,
                                    accountId = revenueGlAccountId,
                                    debit = 0L,
                                    credit = paidAmountMinor,
                                    description = "إيرادات خدمات فندقية - فاتورة ${invoice.invoiceNumber}"
                                )
                            )
                        )

                        // Adjust GL balances
                        accountingDao.adjustAccountBalance(cashboxGlAccountId, paidAmountMinor)
                        accountingDao.adjustAccountBalance(revenueGlAccountId, paidAmountMinor)
                    }
                }

                invId
            }
            Resource.Success(invoiceId)
        } catch (e: Exception) {
            Resource.Error("فشلت عملية إصدار الفاتورة: ${e.localizedMessage}")
        }
    }
}
