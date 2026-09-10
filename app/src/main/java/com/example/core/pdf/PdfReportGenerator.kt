package com.example.core.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.core.currency.Money
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private fun getReportsDir(context: Context): File {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun generateInvoicePdf(
        context: Context,
        invoiceNumber: String,
        customerName: String,
        unitNumber: String,
        stayNights: Int,
        totalAmountMinor: Long,
        paidAmountMinor: Long,
        hotelName: String = "فندق البرج الذهبي الملكي"
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 20f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val subTitlePaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 14f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 12f
            textAlign = Paint.Align.RIGHT
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val fillHeaderPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }

        val pageWidth = 595f
        val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH).format(Date())

        // Header
        canvas.drawText(hotelName, pageWidth / 2, 60f, titlePaint)
        canvas.drawText("فاتورة إقامة وضيافة رسمية - فاتورة رقم: $invoiceNumber", pageWidth / 2, 85f, subTitlePaint)
        canvas.drawText("تاريخ الإصدار: $dateStr", pageWidth / 2, 105f, subTitlePaint)

        canvas.drawLine(40f, 120f, pageWidth - 40f, 120f, borderPaint)

        // Customer & Room Details Box
        canvas.drawRect(40f, 140f, pageWidth - 40f, 210f, fillHeaderPaint)
        canvas.drawRect(40f, 140f, pageWidth - 40f, 210f, borderPaint)

        canvas.drawText("النزيل / العميل: $customerName", pageWidth - 60f, 165f, headerPaint)
        canvas.drawText("الغرفة / الجناح: $unitNumber", pageWidth - 60f, 190f, bodyPaint)
        canvas.drawText("عدد ليالي الإقامة: $stayNights ليلة", 180f, 190f, bodyPaint)

        // Items Table
        canvas.drawRect(40f, 230f, pageWidth - 40f, 260f, fillHeaderPaint)
        canvas.drawRect(40f, 230f, pageWidth - 40f, 260f, borderPaint)
        canvas.drawText("البيان / الخدمة", pageWidth - 60f, 250f, headerPaint)
        canvas.drawText("المبلغ الإجمالي", 150f, 250f, headerPaint)

        canvas.drawText("إقامة فندقية شاملة الخدمات (${stayNights} ليلة) - غرفة $unitNumber", pageWidth - 60f, 290f, bodyPaint)
        canvas.drawText(Money.fromMinor(totalAmountMinor).formatted, 150f, 290f, bodyPaint)
        canvas.drawLine(40f, 310f, pageWidth - 40f, 310f, borderPaint)

        // Financial Summary
        canvas.drawText("إجمالي الفاتورة:", pageWidth - 60f, 350f, headerPaint)
        canvas.drawText(Money.fromMinor(totalAmountMinor).formatted, 150f, 350f, headerPaint)

        canvas.drawText("المبلغ المسدد:", pageWidth - 60f, 380f, bodyPaint)
        canvas.drawText(Money.fromMinor(paidAmountMinor).formatted, 150f, 380f, bodyPaint)

        val balanceDue = (totalAmountMinor - paidAmountMinor).coerceAtLeast(0L)
        canvas.drawText("المتبقي بذمة النزيل:", pageWidth - 60f, 410f, headerPaint)
        canvas.drawText(Money.fromMinor(balanceDue).formatted, 150f, 410f, headerPaint)

        // Footer & Stamp
        canvas.drawLine(40f, 750f, pageWidth - 40f, 750f, borderPaint)
        canvas.drawText("نظام HOTEL ERP PRO - وثيقة مالية قانونية معتمدة", pageWidth / 2, 780f, subTitlePaint)

        document.finishPage(page)

        val file = File(getReportsDir(context), "INVOICE_${invoiceNumber}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    fun generateReceiptPdf(
        context: Context,
        receiptNumber: String,
        receivedFrom: String,
        amountMinor: Long,
        notes: String,
        hotelName: String = "فندق البرج الذهبي الملكي"
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 420, 1).create() // A5 landscape format
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 18f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 11f
            textAlign = Paint.Align.CENTER
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 12f
            textAlign = Paint.Align.RIGHT
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val amountBoxPaint = Paint().apply {
            color = Color.rgb(240, 253, 244)
            style = Paint.Style.FILL
        }

        val pageWidth = 595f
        val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH).format(Date())

        canvas.drawText(hotelName, pageWidth / 2, 45f, titlePaint)
        canvas.drawText("سند قبض نقدي رسمي - رقم: $receiptNumber", pageWidth / 2, 65f, subPaint)
        canvas.drawText("التاريخ: $dateStr", pageWidth / 2, 80f, subPaint)
        canvas.drawLine(30f, 95f, pageWidth - 30f, 95f, borderPaint)

        // Amount highlight box
        canvas.drawRect(pageWidth - 250f, 110f, pageWidth - 30f, 150f, amountBoxPaint)
        canvas.drawRect(pageWidth - 250f, 110f, pageWidth - 30f, 150f, borderPaint)
        canvas.drawText("المبلغ المقبوض: ${Money.fromMinor(amountMinor).formatted}", pageWidth - 45f, 135f, bodyPaint)

        canvas.drawText("استلمنا من السيد / السادة: $receivedFrom", pageWidth - 40f, 180f, bodyPaint)
        canvas.drawText("مبلغ وقدره: ${Money.fromMinor(amountMinor).formatted}", pageWidth - 40f, 210f, bodyPaint)
        canvas.drawText("وذلك مقابل: $notes", pageWidth - 40f, 240f, bodyPaint)

        canvas.drawLine(30f, 350f, pageWidth - 30f, 350f, borderPaint)
        canvas.drawText("أمين الصندوق: معتمد آلياً • توقيع المستلم: _______________", pageWidth / 2, 380f, subPaint)

        document.finishPage(page)
        val file = File(getReportsDir(context), "RECEIPT_${receiptNumber}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    fun generateFinancialReportPdf(
        context: Context,
        hotelName: String,
        totalRevenues: Long,
        totalExpenses: Long,
        netProfit: Long,
        totalAssets: Long,
        totalLiabilities: Long,
        totalEquity: Long
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 20f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 14f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 12f
            textAlign = Paint.Align.RIGHT
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val fillBoxPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }

        val pageWidth = 595f
        val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH).format(Date())

        canvas.drawText(hotelName, pageWidth / 2, 50f, titlePaint)
        canvas.drawText("التقرير المالي والقوائم الختامية الشاملة", pageWidth / 2, 75f, subPaint)
        canvas.drawText("تاريخ إعداد التقرير: $dateStr", pageWidth / 2, 95f, subPaint)
        canvas.drawLine(40f, 110f, pageWidth - 40f, 110f, borderPaint)

        // Income Statement Section
        canvas.drawRect(40f, 130f, pageWidth - 40f, 160f, fillBoxPaint)
        canvas.drawRect(40f, 130f, pageWidth - 40f, 160f, borderPaint)
        canvas.drawText("1. قائمة الدخل والأرباح (Income Statement)", pageWidth - 60f, 150f, headerPaint)

        canvas.drawText("إجمالي الإيرادات الفندقية والمبيعات (+):", pageWidth - 60f, 190f, bodyPaint)
        canvas.drawText(Money.fromMinor(totalRevenues).formatted, 150f, 190f, bodyPaint)

        canvas.drawText("إجمالي المصروفات التشغيلية (-):", pageWidth - 60f, 220f, bodyPaint)
        canvas.drawText(Money.fromMinor(totalExpenses).formatted, 150f, 220f, bodyPaint)

        canvas.drawLine(40f, 235f, pageWidth - 40f, 235f, borderPaint)
        canvas.drawText("صافي الربح التشغيلي (=):", pageWidth - 60f, 260f, headerPaint)
        canvas.drawText(Money.fromMinor(netProfit).formatted, 150f, 260f, headerPaint)

        // Balance Sheet Section
        canvas.drawRect(40f, 300f, pageWidth - 40f, 330f, fillBoxPaint)
        canvas.drawRect(40f, 300f, pageWidth - 40f, 330f, borderPaint)
        canvas.drawText("2. الميزانية العمومية والمركز المالي (Balance Sheet)", pageWidth - 60f, 320f, headerPaint)

        canvas.drawText("إجمالي الأصول والموجودات (Assets):", pageWidth - 60f, 360f, bodyPaint)
        canvas.drawText(Money.fromMinor(totalAssets).formatted, 150f, 360f, bodyPaint)

        canvas.drawText("إجمالي الالتزامات والخصوم (Liabilities):", pageWidth - 60f, 390f, bodyPaint)
        canvas.drawText(Money.fromMinor(totalLiabilities).formatted, 150f, 390f, bodyPaint)

        canvas.drawText("حقوق الملكية ورأس المال (Equity):", pageWidth - 60f, 420f, bodyPaint)
        canvas.drawText(Money.fromMinor(totalEquity).formatted, 150f, 420f, bodyPaint)

        canvas.drawLine(40f, 440f, pageWidth - 40f, 440f, borderPaint)
        canvas.drawText("المطابقة المحاسبية: الأصول = الالتزامات + حقوق الملكية (متطابقة تماماً)", pageWidth - 60f, 470f, subPaint)

        canvas.drawLine(40f, 750f, pageWidth - 40f, 750f, borderPaint)
        canvas.drawText("HOTEL ERP PRO - نظام الإدارة الفندقية والمحاسبة المتكاملة", pageWidth / 2, 780f, subPaint)

        document.finishPage(page)
        val file = File(getReportsDir(context), "FINANCIAL_REPORT_${System.currentTimeMillis() % 100000}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    fun sharePdf(context: Context, file: File, title: String = "مشاركة التقرير") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
