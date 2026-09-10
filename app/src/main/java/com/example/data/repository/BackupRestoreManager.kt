package com.example.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.example.core.result.Resource
import com.example.data.local.HotelDatabase
import com.example.data.local.entity.backup.BackupMetadataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRestoreManager(
    private val context: Context,
    private val database: HotelDatabase
) {
    private val backupDao = database.backupDao()
    private val backupsDir = File(context.filesDir, "backups").apply {
        if (!exists()) mkdirs()
    }

    fun getAllBackups(): Flow<List<BackupMetadataEntity>> = backupDao.getAllBackups()

    suspend fun createBackup(
        notes: String = "نسخة احتياطية يدوية كاملة",
        operator: String = "SYSTEM",
        isPreRestoreSafety: Boolean = false
    ): Resource<BackupMetadataEntity> = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath("hotel_erp_pro.db")
            if (!dbFile.exists()) {
                return@withContext Resource.Error("ملف قاعدة البيانات غير موجود")
            }

            // 1. Force WAL checkpoint to consolidate uncommitted transactions into main file
            try {
                database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            } catch (e: Exception) {
                // Fallback: continue even if checkpoint throws
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            val prefix = if (isPreRestoreSafety) "SAFETY_BEFORE_RESTORE" else "BACKUP"
            val targetFileName = "HOTEL_ERP_${prefix}_${timestamp}.db"
            val targetFile = File(backupsDir, targetFileName)

            // Copy main db file
            FileInputStream(dbFile).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Compute SHA-256 Checksum
            val checksum = calculateSha256(targetFile)

            // Validate integrity of the created backup file
            val integrityValidation = validateDatabaseFile(targetFile)
            if (integrityValidation is Resource.Error) {
                targetFile.delete()
                return@withContext Resource.Error("فشل التحقق من سلامة النسخة الاحتياطية: ${integrityValidation.message}")
            }

            // Compute total records archived
            val totalRecords = countTotalArchivedRecords()

            val metadata = BackupMetadataEntity(
                backupFileName = targetFileName,
                backupSizeBytes = targetFile.length(),
                totalRecordsArchived = totalRecords,
                checksumSha256 = checksum,
                backupType = if (isPreRestoreSafety) "SAFETY_AUTO" else "FULL_OFFLINE",
                status = "COMPLETED",
                notes = notes,
                createdBy = operator,
                createdAt = System.currentTimeMillis()
            )

            backupDao.insertBackupRecord(metadata)
            Resource.Success(metadata)
        } catch (e: Exception) {
            Resource.Error("خطأ أثناء إنشاء النسخة الاحتياطية: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun restoreBackup(
        backupFileName: String,
        operator: String = "ADMIN",
        createAutoPreBackup: Boolean = true
    ): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(backupsDir, backupFileName)
            if (!sourceFile.exists()) {
                return@withContext Resource.Error("ملف النسخة الاحتياطية المحدد غير موجود على الجهاز")
            }

            // 1. Validate the backup file thoroughly before doing anything
            val validation = validateDatabaseFile(sourceFile)
            if (validation is Resource.Error) {
                return@withContext Resource.Error("فشل التحقق من سلامة وصلاحية ملف النسخة: ${validation.message}")
            }

            // 2. Pre-restore automatic safety backup of current live database
            if (createAutoPreBackup) {
                val preBackupResult = createBackup(
                    notes = "نسخة احتياطية أمان تلقائية قبل استعادة $backupFileName",
                    operator = operator,
                    isPreRestoreSafety = true
                )
                if (preBackupResult is Resource.Error) {
                    return@withContext Resource.Error("تعذر إنشاء نسخة الأمان التلقائية قبل الاستعادة: ${preBackupResult.message}")
                }
            }

            val targetDbFile = context.getDatabasePath("hotel_erp_pro.db")
            val walFile = File(targetDbFile.parentFile, "hotel_erp_pro.db-wal")
            val shmFile = File(targetDbFile.parentFile, "hotel_erp_pro.db-shm")

            // Flush live DB
            try {
                database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            } catch (_: Exception) {}

            // Replace main db file
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(targetDbFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Clean stale WAL and SHM files to ensure restored state loads cleanly
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            // Verify integrity of target restored db
            val postRestoreCheck = validateDatabaseFile(targetDbFile)
            if (postRestoreCheck is Resource.Error) {
                return@withContext Resource.Error("تحذير: فشل فحص سلامة البيانات بعد الاستعادة: ${postRestoreCheck.message}")
            }

            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error("فشل استعادة النسخة الاحتياطية: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun importBackupFromUri(sourceUri: Uri): Resource<File> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            val importedFileName = "HOTEL_ERP_IMPORTED_${timestamp}.db"
            val targetFile = File(backupsDir, importedFileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Resource.Error("تعذر فتح وقراءة الملف المحدد")

            // Validate imported file
            val validation = validateDatabaseFile(targetFile)
            if (validation is Resource.Error) {
                targetFile.delete()
                return@withContext Resource.Error("الملف المستورد غير متوافق: ${validation.message}")
            }

            val checksum = calculateSha256(targetFile)
            val metadata = BackupMetadataEntity(
                backupFileName = importedFileName,
                backupSizeBytes = targetFile.length(),
                totalRecordsArchived = 0L,
                checksumSha256 = checksum,
                backupType = "IMPORTED_EXTERNAL",
                status = "COMPLETED",
                notes = "نسخة مستوردة من ملف خارجي",
                createdBy = "USER",
                createdAt = System.currentTimeMillis()
            )
            backupDao.insertBackupRecord(metadata)

            Resource.Success(targetFile)
        } catch (e: Exception) {
            Resource.Error("خطأ أثناء استيراد النسخة الاحتياطية: ${e.localizedMessage ?: e.message}")
        }
    }

    fun getBackupFile(fileName: String): File? {
        val file = File(backupsDir, fileName)
        return if (file.exists()) file else null
    }

    fun validateDatabaseFile(file: File): Resource<Boolean> {
        if (!file.exists() || file.length() < 100) {
            return Resource.Error("الملف غير موجود أو فارغ")
        }

        // Verify SQLite Header
        val header = ByteArray(16)
        FileInputStream(file).use { it.read(header) }
        val headerStr = String(header)
        if (!headerStr.startsWith("SQLite format 3")) {
            return Resource.Error("الملف ليس قاعدة بيانات SQLite صالحة")
        }

        // Open read-only and verify integrity and essential tables
        var testDb: SQLiteDatabase? = null
        return try {
            testDb = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = testDb.rawQuery("PRAGMA integrity_check", null)
            var integrityOk = false
            if (cursor.moveToFirst()) {
                val result = cursor.getString(0)
                integrityOk = result.equals("ok", ignoreCase = true)
            }
            cursor.close()

            if (!integrityOk) {
                return Resource.Error("قاعدة البيانات تحتوي على تلف هيكلي (Integrity check failed)")
            }

            // Verify essential hotel ERP tables exist
            val requiredTables = listOf("units", "customers", "reservations", "accounts", "journal_entries", "users")
            for (table in requiredTables) {
                val checkCursor = testDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table))
                val exists = checkCursor.moveToFirst()
                checkCursor.close()
                if (!exists) {
                    return Resource.Error("النسخة الاحتياطية غير متوافقة مع هيكل النظام (الجدول $table مفقود)")
                }
            }

            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error("خطأ في قراءة ملف النسخة: ${e.message}")
        } finally {
            testDb?.close()
        }
    }

    private fun countTotalArchivedRecords(): Long {
        return try {
            val cursor = database.openHelper.readableDatabase.query(
                "SELECT " +
                        "(SELECT COUNT(*) FROM units) + " +
                        "(SELECT COUNT(*) FROM customers) + " +
                        "(SELECT COUNT(*) FROM reservations) + " +
                        "(SELECT COUNT(*) FROM accounts) + " +
                        "(SELECT COUNT(*) FROM journal_entries) + " +
                        "(SELECT COUNT(*) FROM products) + " +
                        "(SELECT COUNT(*) FROM users)"
            )
            var count = 0L
            if (cursor.moveToFirst()) {
                count = cursor.getLong(0)
            }
            cursor.close()
            count
        } catch (_: Exception) {
            0L
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
