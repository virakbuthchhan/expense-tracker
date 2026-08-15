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

    private const val PAGE_WIDTH = 595 // Standard A4 width in points (72 DPI)
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points (72 DPI)
    private const val MARGIN_X = 36f
    private const val MARGIN_Y = 36f
    private const val USABLE_WIDTH = PAGE_WIDTH - (MARGIN_X * 2)

    /**
     * Transforms Room database transactions into a PDF document and writes directly to an OutputStream.
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

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val generationDateStr = timeFormat.format(Date())

            val titlePaint = Paint().apply {
                color = Color.rgb(15, 23, 42) // Slate 900
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(100, 116, 139) // Slate 500
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val headerBgPaint = Paint().apply {
                color = Color.rgb(16, 185, 129) // Emerald 500
                isAntiAlias = true
            }

            val cardBgPaint = Paint().apply {
                color = Color.rgb(241, 245, 249) // Slate 100
                isAntiAlias = true
            }

            val tableHeaderBgPaint = Paint().apply {
                color = Color.rgb(30, 41, 59) // Slate 800
                isAntiAlias = true
            }

            val tableHeaderTxtPaint = Paint().apply {
                color = Color.WHITE
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val rowEvenPaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
            }

            val rowOddPaint = Paint().apply {
                color = Color.rgb(248, 250, 252) // Slate 50
                isAntiAlias = true
            }

            val rowBorderPaint = Paint().apply {
                color = Color.rgb(226, 232, 240) // Slate 200
                strokeWidth = 0.5f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }

            val cellTxtPaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val incomeTxtPaint = Paint().apply {
                color = Color.rgb(16, 185, 129) // Emerald Green
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            val expenseTxtPaint = Paint().apply {
                color = Color.rgb(239, 68, 68) // Red
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            val footerTxtPaint = Paint().apply {
                color = Color.rgb(148, 163, 184)
                textSize = 8f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            // Estimate total pages
            val rowsPerPage = 22
            val totalPages = if (transactions.isEmpty()) 1 else {
                val remainingRows = (transactions.size - 14).coerceAtLeast(0)
                1 + Math.ceil(remainingRows.toDouble() / rowsPerPage).toInt()
            }

            var currentPageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            var currentY = MARGIN_Y

            // --- PAGE 1 HEADER ---
            // Brand Accent Header Bar
            val bannerRect = RectF(MARGIN_X, currentY, MARGIN_X + USABLE_WIDTH, currentY + 6f)
            canvas.drawRoundRect(bannerRect, 3f, 3f, headerBgPaint)
            currentY += 22f

            // Title and Metadata
            canvas.drawText("FINANCIAL TRANSACTION STATEMENT", MARGIN_X, currentY, titlePaint)
            currentY += 12f
            canvas.drawText("${appStrings.appName} • 100% Offline & Private Financial Record", MARGIN_X, currentY, subtitlePaint)

            // Right side metadata
            val metaRightPaint = Paint().apply {
                color = Color.rgb(71, 85, 105)
                textSize = 8.5f
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            canvas.drawText("Generated: $generationDateStr", MARGIN_X + USABLE_WIDTH, currentY - 12f, metaRightPaint)
            canvas.drawText("Currency: $currencyCode ($currencySymbol)", MARGIN_X + USABLE_WIDTH, currentY, metaRightPaint)
            currentY += 18f

            // --- SUMMARY METRICS CARDS (PAGE 1) ---
            val cardHeight = 44f
            val cardGap = 8f
            val cardWidth = (USABLE_WIDTH - (cardGap * 3)) / 4f

            // Card 1: Balance
            drawSummaryMetricCard(
                canvas = canvas,
                x = MARGIN_X,
                y = currentY,
                width = cardWidth,
                height = cardHeight,
                title = appStrings.totalBalance,
                value = "$currencySymbol${String.format(Locale.US, "%,.2f", netBalance)}",
                valueColor = if (netBalance >= 0) Color.rgb(16, 185, 129) else Color.rgb(239, 68, 68),
                bgPaint = cardBgPaint
            )

            // Card 2: Total Income
            drawSummaryMetricCard(
                canvas = canvas,
                x = MARGIN_X + cardWidth + cardGap,
                y = currentY,
                width = cardWidth,
                height = cardHeight,
                title = appStrings.monthlyIncome,
                value = "+$currencySymbol${String.format(Locale.US, "%,.2f", totalIncome)}",
                valueColor = Color.rgb(16, 185, 129),
                bgPaint = cardBgPaint
            )

            // Card 3: Total Expenses
            drawSummaryMetricCard(
                canvas = canvas,
                x = MARGIN_X + (cardWidth + cardGap) * 2,
                y = currentY,
                width = cardWidth,
                height = cardHeight,
                title = appStrings.monthlyExpense,
                value = "-$currencySymbol${String.format(Locale.US, "%,.2f", totalExpense)}",
                valueColor = Color.rgb(239, 68, 68),
                bgPaint = cardBgPaint
            )

            // Card 4: Total Records
            drawSummaryMetricCard(
                canvas = canvas,
                x = MARGIN_X + (cardWidth + cardGap) * 3,
                y = currentY,
                width = cardWidth,
                height = cardHeight,
                title = appStrings.totalTransactionsRecorded,
                value = "${transactions.size} records",
                valueColor = Color.rgb(30, 41, 59),
                bgPaint = cardBgPaint
            )

            currentY += cardHeight + 20f

            // --- TABLE COLUMN WIDTHS ---
            val colDateW = 75f
            val colTypeW = 55f
            val colCatW = 105f
            val colNoteW = 190f
            val colAmountW = 98f
            val rowHeight = 22f

            // Function to draw table header
            fun drawTableHeader(c: Canvas, y: Float) {
                val headerRect = RectF(MARGIN_X, y, MARGIN_X + USABLE_WIDTH, y + 20f)
                c.drawRoundRect(headerRect, 4f, 4f, tableHeaderBgPaint)

                var colX = MARGIN_X + 8f
                c.drawText(appStrings.date.uppercase(), colX, y + 13.5f, tableHeaderTxtPaint)
                colX += colDateW
                c.drawText("TYPE", colX, y + 13.5f, tableHeaderTxtPaint)
                colX += colTypeW
                c.drawText(appStrings.category.uppercase(), colX, y + 13.5f, tableHeaderTxtPaint)
                colX += colCatW
                c.drawText("NOTE / MERCHANT", colX, y + 13.5f, tableHeaderTxtPaint)

                val amountHeaderPaint = Paint(tableHeaderTxtPaint).apply {
                    textAlign = Paint.Align.RIGHT
                }
                c.drawText(appStrings.amount.uppercase(), MARGIN_X + USABLE_WIDTH - 8f, y + 13.5f, amountHeaderPaint)
            }

            drawTableHeader(canvas, currentY)
            currentY += 20f

            if (transactions.isEmpty()) {
                currentY += 40f
                val emptyPaint = Paint().apply {
                    color = Color.rgb(148, 163, 184)
                    textSize = 11f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("No transactions recorded in database.", MARGIN_X + (USABLE_WIDTH / 2f), currentY, emptyPaint)
            } else {
                for (i in transactions.indices) {
                    val t = transactions[i]

                    // Check if we need a new page
                    if (currentY + rowHeight > PAGE_HEIGHT - 50f) {
                        // Draw footer on current page
                        canvas.drawText("Page $currentPageNumber of $totalPages • Generated by ${appStrings.appName} Offline", PAGE_WIDTH / 2f, PAGE_HEIGHT - 20f, footerTxtPaint)
                        pdfDocument.finishPage(page)

                        // Start new page
                        currentPageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = MARGIN_Y

                        // Mini header on subsequent pages
                        canvas.drawText("FINANCIAL TRANSACTION STATEMENT (CONTINUED)", MARGIN_X, currentY + 10f, titlePaint)
                        canvas.drawText("Generated: $generationDateStr", MARGIN_X + USABLE_WIDTH, currentY + 10f, metaRightPaint)
                        currentY += 24f

                        drawTableHeader(canvas, currentY)
                        currentY += 20f
                    }

                    // Row background
                    val rowRect = RectF(MARGIN_X, currentY, MARGIN_X + USABLE_WIDTH, currentY + rowHeight)
                    canvas.drawRect(rowRect, if (i % 2 == 0) rowEvenPaint else rowOddPaint)
                    canvas.drawRect(rowRect, rowBorderPaint)

                    val textBaseline = currentY + 14f
                    var colX = MARGIN_X + 8f

                    // 1. Date
                    val dateText = dateFormat.format(Date(t.date))
                    canvas.drawText(dateText, colX, textBaseline, cellTxtPaint)
                    colX += colDateW

                    // 2. Type badge text
                    val isIncome = t.type.equals("income", ignoreCase = true)
                    val typeText = if (isIncome) "INCOME" else "EXPENSE"
                    val typeColorPaint = Paint(cellTxtPaint).apply {
                        color = if (isIncome) Color.rgb(16, 185, 129) else Color.rgb(239, 68, 68)
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textSize = 7.5f
                    }
                    canvas.drawText(typeText, colX, textBaseline, typeColorPaint)
                    colX += colTypeW

                    // 3. Category
                    val catName = truncateText(t.categoryName, 18)
                    canvas.drawText(catName, colX, textBaseline, cellTxtPaint)
                    colX += colCatW

                    // 4. Note
                    val noteText = truncateText(if (t.note.isNotBlank()) t.note else "-", 32)
                    canvas.drawText(noteText, colX, textBaseline, cellTxtPaint)

                    // 5. Amount (aligned right)
                    val formattedAmount = if (isIncome) {
                        "+$currencySymbol${String.format(Locale.US, "%,.2f", t.amount)}"
                    } else {
                        "-$currencySymbol${String.format(Locale.US, "%,.2f", t.amount)}"
                    }
                    canvas.drawText(
                        formattedAmount,
                        MARGIN_X + USABLE_WIDTH - 8f,
                        textBaseline,
                        if (isIncome) incomeTxtPaint else expenseTxtPaint
                    )

                    currentY += rowHeight
                }
            }

            // Draw footer on last page
            canvas.drawText("Page $currentPageNumber of $totalPages • Generated by ${appStrings.appName} Offline", PAGE_WIDTH / 2f, PAGE_HEIGHT - 20f, footerTxtPaint)
            pdfDocument.finishPage(page)

            // Write out to stream
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
        } finally {
            pdfDocument.close()
        }
    }

    private fun drawSummaryMetricCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String,
        value: String,
        valueColor: Int,
        bgPaint: Paint
    ) {
        val rect = RectF(x, y, x + width, y + height)
        canvas.drawRoundRect(rect, 8f, 8f, bgPaint)

        val borderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

        val cardTitlePaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val cardValuePaint = Paint().apply {
            color = valueColor
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawText(truncateText(title.uppercase(), 16), x + 8f, y + 15f, cardTitlePaint)
        canvas.drawText(truncateText(value, 15), x + 8f, y + 33f, cardValuePaint)
    }

    private fun truncateText(text: String, maxChars: Int): String {
        return if (text.length > maxChars) {
            text.substring(0, maxChars - 2) + ".."
        } else {
            text
        }
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
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(exportDir, "expense_statement_$timeStamp.pdf")

        FileOutputStream(file).use { fos ->
            generatePdfToStream(
                transactions = transactions,
                currencyCode = currencyCode,
                currencySymbol = currencySymbol,
                appStrings = appStrings,
                outputStream = fos
            )
        }

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Exports Room transactions directly into an existing Uri (e.g. from Storage Access Framework ACTION_CREATE_DOCUMENT).
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
            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                generatePdfToStream(
                    transactions = transactions,
                    currencyCode = currencyCode,
                    currencySymbol = currencySymbol,
                    appStrings = appStrings,
                    outputStream = outputStream
                )
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Creates an Intent to share or save the generated PDF file.
     */
    fun createSharePdfIntent(context: Context, pdfUri: Uri, title: String = "Expense Statement PDF"): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_TITLE, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Creates an Intent to view/print the generated PDF file in a PDF reader.
     */
    fun createViewPdfIntent(pdfUri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(pdfUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
