package com.example.data.repository

import androidx.room.withTransaction
import com.example.core.result.Resource
import com.example.data.local.HotelDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.accounting.*
import com.example.data.local.entity.cashbox.*
import com.example.data.local.entity.payment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * AccountingEngine:
 * Central Financial Authority for HOTEL ERP PRO.
 * Strictly adheres to double-entry bookkeeping, atomicity via transactions,
 * and maintains financial integrity across shifts, cashboxes, vouchers, and general ledger.
 */
class AccountingEngine(
    private val database: HotelDatabase,
    private val accountingDao: AccountingDao,
    private val cashboxDao: CashboxDao,
    private val paymentDao: PaymentDao
) {
    constructor(database: HotelDatabase) : this(
        database = database,
        accountingDao = database.accountingDao(),
        cashboxDao = database.cashboxDao(),
        paymentDao = database.paymentDao()
    )
    fun getAllAccounts(): Flow<List<AccountEntity>> = accountingDao.getAllActiveAccounts()
    fun getAllJournalEntries(): Flow<List<JournalEntryEntity>> = accountingDao.getAllJournalEntries()
    fun getStatementLinesForAccount(accountId: Long): Flow<List<AccountStatementLineTuple>> = accountingDao.getStatementLinesForAccount(accountId)
    fun getAllStatementLines(): Flow<List<AccountStatementLineTuple>> = accountingDao.getAllStatementLines()

    /**
     * Central Posting Engine: Strict Balanced Double-Entry Rule.
     * Total Debit must equal Total Credit.
     */
    suspend fun postJournalEntry(
        entryNumber: String,
        entryDate: Long = System.currentTimeMillis(),
        description: String,
        referenceType: String? = null,
        referenceId: String? = null,
        createdBy: String = "SYSTEM",
        lines: List<JournalEntryLineEntity>
    ): Resource<Long> = withContext(Dispatchers.IO) {
        if (lines.size < 2) {
            return@withContext Resource.Error("القيد المحاسبي يجب أن يحتوي على طرفين على الأقل (مدين ودائن)")
        }

        val totalDebit = lines.sumOf { it.debit }
        val totalCredit = lines.sumOf { it.credit }

        if (totalDebit != totalCredit) {
            return@withContext Resource.Error(
                "القيد المحاسبي غير متوازن! إجمالي المدين ($totalDebit) لا يساوي إجمالي الدائن ($totalCredit)"
            )
        }

        try {
            database.withTransaction {
                val entry = JournalEntryEntity(
                    entryNumber = entryNumber,
                    date = entryDate,
                    description = description,
                    referenceType = referenceType,
                    referenceId = referenceId,
                    totalDebitMinor = totalDebit,
                    totalCreditMinor = totalCredit,
                    status = "POSTED",
                    createdBy = createdBy
                )
                val entryId = accountingDao.insertJournalEntry(entry)
                val linesWithId = lines.map { it.copy(entryId = entryId) }
                accountingDao.insertJournalLines(linesWithId)

                // Update accounts balances atomically
                for (line in linesWithId) {
                    val balanceDiff = line.debit - line.credit
                    accountingDao.adjustAccountBalance(line.accountId, balanceDiff)
                }

                entryId
            }.let { Resource.Success(it) }
        } catch (e: Exception) {
            Resource.Error("فشل ترحيل القيد المحاسبي: ${e.message}", e)
        }
    }

    /**
     * Shift Management: Open Shift
     */
    suspend fun openShift(
        shiftCode: String,
        cashboxId: Long,
        cashierName: String,
        openingBalanceMinor: Long
    ): Resource<Long> = withContext(Dispatchers.IO) {
        val existing = cashboxDao.getOpenShiftForCashbox(cashboxId)
        if (existing != null) {
            return@withContext Resource.Error("يوجد بالفعل وردية مفتوحة لهذا الصندوق حالياً (${existing.shiftNumber})")
        }

        try {
            val shift = ShiftEntity(
                shiftNumber = shiftCode,
                cashboxId = cashboxId,
                operatorName = cashierName,
                startTime = System.currentTimeMillis(),
                openingBalanceMinor = openingBalanceMinor,
                status = "OPEN"
            )
            val id = cashboxDao.insertShift(shift)
            Resource.Success(id)
        } catch (e: Exception) {
            Resource.Error("فشل فتح الوردية: ${e.message}")
        }
    }

    /**
     * Shift Management: Close Shift with Reconciliation
     */
    suspend fun closeShift(
        shiftId: Long,
        closedByName: String,
        actualCashCountMinor: Long,
        notes: String
    ): Resource<ShiftClosureEntity> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                val shift = cashboxDao.getShiftById(shiftId)
                    ?: throw IllegalStateException("الوردية غير موجودة")

                val expected = shift.openingBalanceMinor
                val diff = actualCashCountMinor - expected

                val closure = ShiftClosureEntity(
                    shiftId = shiftId,
                    actualCashCountMinor = actualCashCountMinor,
                    systemCalculatedMinor = expected,
                    varianceMinor = diff,
                    closureNotes = notes,
                    closedBy = closedByName,
                    closureTimestamp = System.currentTimeMillis()
                )
                cashboxDao.insertShiftClosure(closure)
                cashboxDao.updateShiftStatus(
                    shiftId = shiftId,
                    status = "CLOSED",
                    endTime = System.currentTimeMillis(),
                    closingBalance = actualCashCountMinor,
                    difference = diff
                )
                closure
            }.let { Resource.Success(it) }
        } catch (e: Exception) {
            Resource.Error("فشل إغلاق الوردية: ${e.message}")
        }
    }

    /**
     * Receipt Voucher (سند قبض) with Cashbox + Accounting Updates
     */
    suspend fun createReceiptVoucher(
        receiptNumber: String,
        customerId: Long = 1L,
        receivedFrom: String,
        amountMinor: Long,
        paymentMethodCode: String,
        cashboxId: Long,
        creditAccountId: Long,
        referenceNo: String?,
        notes: String,
        invoiceId: Long? = null,
        operatorName: String = "SYSTEM"
    ): Resource<Long> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                val cashbox = cashboxDao.getCashboxById(cashboxId)
                    ?: throw IllegalStateException("الصندوق المحدد غير موجود")

                val receipt = ReceiptEntity(
                    receiptNumber = receiptNumber,
                    customerId = customerId,
                    cashboxId = cashboxId,
                    amountMinor = amountMinor,
                    paymentMethodCode = paymentMethodCode,
                    referenceNumber = referenceNo,
                    receivedFrom = receivedFrom,
                    description = notes,
                    receivedBy = operatorName
                )
                val receiptId = paymentDao.insertReceipt(receipt)

                if (invoiceId != null) {
                    paymentDao.insertReceiptAllocation(
                        ReceiptAllocationEntity(
                            receiptId = receiptId,
                            invoiceId = invoiceId,
                            allocatedAmountMinor = amountMinor
                        )
                    )
                }

                // Cashbox transaction
                val txNumber = "CBT-REC-$receiptNumber"
                cashboxDao.insertTransaction(
                    CashboxTransactionEntity(
                        transactionNumber = txNumber,
                        cashboxId = cashboxId,
                        transactionType = "RECEIPT",
                        amountMinor = amountMinor,
                        direction = "IN",
                        balanceAfterMinor = cashbox.currentBalanceMinor + amountMinor,
                        referenceType = "RECEIPT",
                        referenceId = receiptNumber,
                        description = "سند قبض: $receivedFrom - $notes",
                        operatorName = operatorName
                    )
                )
                cashboxDao.updateBalance(cashboxId, amountMinor)

                // Balanced Journal Entry: Debit Cashbox GL Account, Credit selected account
                val lines = listOf(
                    JournalEntryLineEntity(
                        entryId = 0,
                        accountId = cashbox.glAccountId,
                        debit = amountMinor,
                        credit = 0,
                        description = "قبض نقدية: $receiptNumber"
                    ),
                    JournalEntryLineEntity(
                        entryId = 0,
                        accountId = creditAccountId,
                        debit = 0,
                        credit = amountMinor,
                        description = "من: $receivedFrom"
                    )
                )
                val journalRes = postJournalEntry(
                    entryNumber = "JV-REC-$receiptNumber",
                    description = "سند قبض رقم $receiptNumber من $receivedFrom",
                    referenceType = "RECEIPT",
                    referenceId = receiptNumber,
                    createdBy = operatorName,
                    lines = lines
                )
                if (journalRes is Resource.Error) {
                    throw IllegalStateException(journalRes.message)
                }

                receiptId
            }.let { Resource.Success(it) }
        } catch (e: Exception) {
            Resource.Error("فشل إنشاء سند القبض: ${e.message}")
        }
    }

    /**
     * Payment Voucher (سند صرف) with Cashbox + Accounting Updates
     */
    suspend fun createPaymentVoucher(
        paymentNumber: String,
        supplierId: Long? = null,
        paidTo: String,
        amountMinor: Long,
        paymentMethodCode: String,
        cashboxId: Long,
        debitAccountId: Long,
        referenceNo: String?,
        notes: String,
        operatorName: String = "SYSTEM"
    ): Resource<Long> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                val cashbox = cashboxDao.getCashboxById(cashboxId)
                    ?: throw IllegalStateException("الصندوق المحدد غير موجود")

                if (cashbox.currentBalanceMinor < amountMinor) {
                    throw IllegalStateException("رصيد الصندوق الحالي لا يكفي لإتمام عملية الصرف")
                }

                val payment = PaymentVoucherEntity(
                    paymentNumber = paymentNumber,
                    supplierId = supplierId,
                    cashboxId = cashboxId,
                    amountMinor = amountMinor,
                    paymentMethodCode = paymentMethodCode,
                    referenceNumber = referenceNo,
                    paidTo = paidTo,
                    description = notes,
                    paidBy = operatorName
                )
                val paymentId = paymentDao.insertPayment(payment)

                // Cashbox transaction
                val txNumber = "CBT-PAY-$paymentNumber"
                cashboxDao.insertTransaction(
                    CashboxTransactionEntity(
                        transactionNumber = txNumber,
                        cashboxId = cashboxId,
                        transactionType = "PAYMENT",
                        amountMinor = amountMinor,
                        direction = "OUT",
                        balanceAfterMinor = cashbox.currentBalanceMinor - amountMinor,
                        referenceType = "PAYMENT",
                        referenceId = paymentNumber,
                        description = "سند صرف: $paidTo - $notes",
                        operatorName = operatorName
                    )
                )
                cashboxDao.updateBalance(cashboxId, -amountMinor)

                // Balanced Journal Entry: Debit selected account, Credit Cashbox GL Account
                val lines = listOf(
                    JournalEntryLineEntity(
                        entryId = 0,
                        accountId = debitAccountId,
                        debit = amountMinor,
                        credit = 0,
                        description = "صرف إلى: $paidTo"
                    ),
                    JournalEntryLineEntity(
                        entryId = 0,
                        accountId = cashbox.glAccountId,
                        debit = 0,
                        credit = amountMinor,
                        description = "صرف نقدية: $paymentNumber"
                    )
                )
                val journalRes = postJournalEntry(
                    entryNumber = "JV-PAY-$paymentNumber",
                    description = "سند صرف رقم $paymentNumber إلى $paidTo",
                    referenceType = "PAYMENT",
                    referenceId = paymentNumber,
                    createdBy = operatorName,
                    lines = lines
                )
                if (journalRes is Resource.Error) {
                    throw IllegalStateException(journalRes.message)
                }

                paymentId
            }.let { Resource.Success(it) }
        } catch (e: Exception) {
            Resource.Error("فشل إنشاء سند الصرف: ${e.message}")
        }
    }

    /**
     * Cashbox Transfer: Cashbox A -> Cashbox B atomically
     */
    suspend fun transferBetweenCashboxes(
        fromCashboxId: Long,
        toCashboxId: Long,
        amountMinor: Long,
        notes: String,
        operatorName: String = "SYSTEM"
    ): Resource<Long> = withContext(Dispatchers.IO) {
        if (fromCashboxId == toCashboxId) {
            return@withContext Resource.Error("لا يمكن التحويل إلى نفس الصندوق")
        }
        try {
            database.withTransaction {
                val fromBox = cashboxDao.getCashboxById(fromCashboxId)
                    ?: throw IllegalStateException("صندوق المصدر غير موجود")
                val toBox = cashboxDao.getCashboxById(toCashboxId)
                    ?: throw IllegalStateException("صندوق الوجهة غير موجود")

                if (fromBox.currentBalanceMinor < amountMinor) {
                    throw IllegalStateException("رصيد صندوق المصدر لا يكفي للتحويل")
                }

                val transferNumber = "TRF-${System.currentTimeMillis()}"
                val transfer = CashboxTransferEntity(
                    transferNumber = transferNumber,
                    fromCashboxId = fromCashboxId,
                    toCashboxId = toCashboxId,
                    amountMinor = amountMinor,
                    notes = notes,
                    transferredBy = operatorName
                )
                val transferId = cashboxDao.insertTransfer(transfer)

                cashboxDao.updateBalance(fromCashboxId, -amountMinor)
                cashboxDao.updateBalance(toCashboxId, amountMinor)

                cashboxDao.insertTransaction(
                    CashboxTransactionEntity(
                        transactionNumber = "CBT-OUT-$transferId",
                        cashboxId = fromCashboxId,
                        transactionType = "TRANSFER_OUT",
                        amountMinor = amountMinor,
                        direction = "OUT",
                        balanceAfterMinor = fromBox.currentBalanceMinor - amountMinor,
                        referenceType = "TRANSFER_OUT",
                        referenceId = transferId.toString(),
                        description = "تحويل إلى ${toBox.nameAr}",
                        operatorName = operatorName
                    )
                )

                cashboxDao.insertTransaction(
                    CashboxTransactionEntity(
                        transactionNumber = "CBT-IN-$transferId",
                        cashboxId = toCashboxId,
                        transactionType = "TRANSFER_IN",
                        amountMinor = amountMinor,
                        direction = "IN",
                        balanceAfterMinor = toBox.currentBalanceMinor + amountMinor,
                        referenceType = "TRANSFER_IN",
                        referenceId = transferId.toString(),
                        description = "تحويل وارد من ${fromBox.nameAr}",
                        operatorName = operatorName
                    )
                )

                // Balanced Journal Entry between the two GL accounts
                val lines = listOf(
                    JournalEntryLineEntity(
                        entryId = 0,
                        accountId = toBox.glAccountId,
                        debit = amountMinor,
                        credit = 0,
                        description = "تحويل إلى ${toBox.nameAr}"
                    ),
                    JournalEntryLineEntity(
                        entryId = 0,
                        accountId = fromBox.glAccountId,
                        debit = 0,
                        credit = amountMinor,
                        description = "تحويل من ${fromBox.nameAr}"
                    )
                )
                val journalRes = postJournalEntry(
                    entryNumber = "JV-TRF-$transferId",
                    description = "تحويل نقدية بين الصناديق #$transferId",
                    referenceType = "TRANSFER",
                    referenceId = transferId.toString(),
                    createdBy = operatorName,
                    lines = lines
                )
                if (journalRes is Resource.Error) {
                    throw IllegalStateException(journalRes.message)
                }

                transferId
            }.let { Resource.Success(it) }
        } catch (e: Exception) {
            Resource.Error("فشل تحويل النقدية: ${e.message}")
        }
    }
}
