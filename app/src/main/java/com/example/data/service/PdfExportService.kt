package com.example.data.service

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.TransactionWithCategory
import com.example.ui.i18n.AppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportService {

    private const val PAGE_WIDTH = 595 // A4 width
    private const val PAGE_HEIGHT = 842 // A4 height
    private const val MARGIN_X = 40f
    private const val MARGIN_Y = 40f
    private const val USABLE_WIDTH = PAGE_WIDTH - (MARGIN_X * 2)

    /**
     * Transforms transactions into a PDF document.
     * Uses more robust font handling and pagination to prevent crashes.
     */
    suspend fun generatePdfToStream(
        transactions: List<TransactionWithCategory>,
        currencyCode: String,
        currencySymbol: String,
        appStrings: AppStrings,
        outputStream: OutputStream
    ) = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        try {
            val totalIncome = transactions.filter { it.type == "income" }.sumOf { it.amount }
            val totalExpense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
            val netBalance = totalIncome - totalExpense

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val generationDateStr = timeFormat.format(Date())

            // Paints with standard SANS_SERIF typeface for better device compatibility
            val titlePaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 16f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 9f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }

            val headerBgPaint = Paint().apply {
                color = Color.rgb(16, 185, 129)
                isAntiAlias = true
            }

            val cardBgPaint = Paint().apply {
                color = Color.rgb(248, 250, 252)
                isAntiAlias = true
            }

            val tableHeaderBgPaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                isAntiAlias = true
            }

            val tableHeaderTxtPaint = Paint().apply {
                color = Color.WHITE
                textSize = 9f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            val rowEvenPaint = Paint().apply { color = Color.WHITE }
            val rowOddPaint = Paint().apply { color = Color.rgb(248, 250, 252) }

            val cellTxtPaint = Paint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }

            val incomeTxtPaint = Paint().apply {
                color = Color.rgb(22, 163, 74)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            val expenseTxtPaint = Paint().apply {
                color = Color.rgb(220, 38, 38)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            val footerTxtPaint = Paint().apply {
                color = Color.rgb(148, 163, 184)
                textSize = 8f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            // Simple Pagination: First page has summary, fewer items.
            val firstPageLimit = 18
            val nextPageLimit = 28
            val totalRecords = transactions.size
            val totalPages = if (totalRecords <= firstPageLimit) 1 
                            else 1 + Math.ceil((totalRecords - firstPageLimit).toDouble() / nextPageLimit).toInt()

            var currentPageNum = 1
            var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNum).create())
            var canvas = page.canvas
            var currentY = MARGIN_Y

            // --- PAGE 1 HEADER ---
            canvas.drawRect(MARGIN_X, currentY, MARGIN_X + USABLE_WIDTH, currentY + 4f, headerBgPaint)
            currentY += 24f

            canvas.drawText("FINANCIAL STATEMENT", MARGIN_X, currentY, titlePaint)
            currentY += 14f
            canvas.drawText("${appStrings.appName} • ${appStrings.offlineSecure}", MARGIN_X, currentY, subtitlePaint)

            val metaPaint = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("Generated: $generationDateStr", MARGIN_X + USABLE_WIDTH, currentY - 14f, metaPaint)
            canvas.drawText("Currency: $currencyCode ($currencySymbol)", MARGIN_X + USABLE_WIDTH, currentY, metaPaint)
            currentY += 24f

            // Summary row
            val cardW = (USABLE_WIDTH - 15f) / 4f
            val cardH = 38f
            
            fun drawSummary(x: Float, label: String, valStr: String, color: Int) {
                val rect = RectF(x, currentY, x + cardW, currentY + cardH)
                canvas.drawRoundRect(rect, 6f, 6f, cardBgPaint)
                val lp = Paint(subtitlePaint).apply { textSize = 6.5f; textAlign = Paint.Align.CENTER }
                val vp = Paint(cellTxtPaint).apply { 
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    this.color = color
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(label.uppercase(), x + cardW/2, currentY + 14f, lp)
                canvas.drawText(valStr, x + cardW/2, currentY + 30f, vp)
            }

            drawSummary(MARGIN_X, appStrings.totalBalance, "$currencySymbol${String.format(Locale.US, "%,.2f", netBalance)}", if(netBalance >= 0) Color.rgb(22, 163, 74) else Color.RED)
            drawSummary(MARGIN_X + cardW + 5f, appStrings.monthlyIncome, "+$currencySymbol${String.format(Locale.US, "%,.0f", totalIncome)}", Color.rgb(22, 163, 74))
            drawSummary(MARGIN_X + (cardW + 5f) * 2, appStrings.monthlyExpense, "-$currencySymbol${String.format(Locale.US, "%,.0f", totalExpense)}", Color.RED)
            drawSummary(MARGIN_X + (cardW + 5f) * 3, "TOTAL RECORDS", "$totalRecords", Color.BLACK)

            currentY += cardH + 28f

            // Table Header
            fun drawTableHead(c: Canvas, y: Float) {
                c.drawRect(MARGIN_X, y, MARGIN_X + USABLE_WIDTH, y + 18f, tableHeaderBgPaint)
                val ty = y + 12f
                c.drawText("DATE", MARGIN_X + 6f, ty, tableHeaderTxtPaint)
                c.drawText("CATEGORY", MARGIN_X + 75f, ty, tableHeaderTxtPaint)
                c.drawText("NOTE", MARGIN_X + 165f, ty, tableHeaderTxtPaint)
                val amPaint = Paint(tableHeaderTxtPaint).apply { textAlign = Paint.Align.RIGHT }
                c.drawText("AMOUNT", MARGIN_X + USABLE_WIDTH - 6f, ty, amPaint)
            }

            drawTableHead(canvas, currentY)
            currentY += 18f

            // Data Rows
            transactions.forEachIndexed { index, t ->
                // Check if current page is full
                val limit = if (currentPageNum == 1) PAGE_HEIGHT - 60f else PAGE_HEIGHT - 60f
                if (currentY > limit) {
                    // Footer before switching
                    canvas.drawText("Page $currentPageNum of $totalPages", PAGE_WIDTH/2f, PAGE_HEIGHT - 20f, footerTxtPaint)
                    pdfDocument.finishPage(page)
                    
                    currentPageNum++
                    page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNum).create())
                    canvas = page.canvas
                    currentY = MARGIN_Y
                    drawTableHead(canvas, currentY)
                    currentY += 18f
                }

                // Row background
                val rowRect = RectF(MARGIN_X, currentY, MARGIN_X + USABLE_WIDTH, currentY + 20f)
                canvas.drawRect(rowRect, if (index % 2 == 0) rowEvenPaint else rowOddPaint)
                
                val ry = currentY + 13.5f
                canvas.drawText(dateFormat.format(Date(t.date)), MARGIN_X + 6f, ry, cellTxtPaint)
                canvas.drawText(safeTruncate(t.categoryName, 18), MARGIN_X + 75f, ry, cellTxtPaint)
                canvas.drawText(safeTruncate(if(t.note.isNotBlank()) t.note else "-", 35), MARGIN_X + 165f, ry, cellTxtPaint)
                
                val isInc = t.type.equals("income", ignoreCase = true)
                val amtStr = if(isInc) "+$currencySymbol${String.format(Locale.US, "%,.2f", t.amount)}" 
                             else "-$currencySymbol${String.format(Locale.US, "%,.2f", t.amount)}"
                canvas.drawText(amtStr, MARGIN_X + USABLE_WIDTH - 6f, ry, if(isInc) incomeTxtPaint else expenseTxtPaint)
                
                currentY += 20f
            }

            // Final Footer
            canvas.drawText("Page $currentPageNum of $totalPages • Generated by ${appStrings.appName}", PAGE_WIDTH/2f, PAGE_HEIGHT - 20f, footerTxtPaint)
            pdfDocument.finishPage(page)

            pdfDocument.writeTo(outputStream)
        } catch (t: Throwable) {
            t.printStackTrace()
            // Rethrow so the caller's try-catch can handle it and return false
            throw Exception("PDF Generation failed: ${t.message}")
        } finally {
            try { pdfDocument.close() } catch (e: Exception) {}
        }
    }

    /**
     * Safer string truncation that avoids splitting surrogate pairs or causing NPEs.
     */
    private fun safeTruncate(text: String?, maxChars: Int): String {
        val s = text ?: return "-"
        return if (s.length > maxChars) s.take(maxChars - 2) + ".." else s
    }

    /**
     * Saves PDF document to a shared cache file and returns its content Uri.
     */
    suspend fun savePdfToCacheFile(
        context: Context,
        transactions: List<TransactionWithCategory>,
        currencyCode: String,
        currencySymbol: String,
        appStrings: AppStrings
    ): Uri = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(exportDir, "kotluy_statement_$timeStamp.pdf")

        FileOutputStream(file).use { fos ->
            generatePdfToStream(transactions, currencyCode, currencySymbol, appStrings, fos)
        }

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Exports Room transactions directly into an existing Uri.
     */
    suspend fun exportToStorageUri(
        context: Context,
        targetUri: Uri,
        transactions: List<TransactionWithCategory>,
        currencyCode: String,
        currencySymbol: String,
        appStrings: AppStrings
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val os = context.contentResolver.openOutputStream(targetUri) ?: return@withContext false
            os.use { outputStream ->
                generatePdfToStream(transactions, currencyCode, currencySymbol, appStrings, outputStream)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun createSharePdfIntent(context: Context, pdfUri: Uri, title: String = "Expense Statement PDF"): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_TITLE, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createViewPdfIntent(pdfUri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(pdfUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
