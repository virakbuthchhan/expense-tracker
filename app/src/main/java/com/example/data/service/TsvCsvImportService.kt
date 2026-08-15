package com.example.data.service

import android.content.Context
import android.net.Uri
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ImportDelimiter(val char: Char, val label: String) {
    TAB('\t', "TSV (Tab)"),
    COMMA(',', "CSV (Comma)"),
    SEMICOLON(';', "Semicolon (;)"),
    PIPE('|', "Pipe (|)")
}

enum class ImportTargetField(val id: String) {
    SKIP("skip"),
    DATE("date"),
    DESCRIPTION("description"),
    CATEGORY("category"),
    AMOUNT("amount"),
    OPTIONAL_NOTES("optional_notes"),
    TYPE("type")
}

data class ParsedRawTable(
    val delimiter: ImportDelimiter,
    val hasHeaderRow: Boolean,
    val headerNames: List<String>,
    val rawRows: List<List<String>>,
    val suggestedMappings: List<ImportTargetField>
)

data class ImportPreviewItem(
    val rowIndex: Int,
    val rawRow: List<String>,
    val date: Long,
    val dateFormatted: String,
    val description: String,
    val categoryName: String,
    val amount: Double,
    val type: String, // "expense" or "income"
    val isValid: Boolean,
    val validationMessage: String = "",
    val isSelected: Boolean = true
)

data class ImportExecutionResult(
    val totalImported: Int,
    val totalSkipped: Int,
    val totalAmount: Double,
    val createdCategoriesCount: Int,
    val errorMessage: String? = null
)

object TsvCsvImportService {

    val SAMPLE_TSV_DATA = """
Date	Short Description	Category	Amount	Optional Notes
01-07-2026	master card aba fee	Other Category	${'$'}5	
01-07-2026	lunch	Other Category	${'$'}1.75	
02-07-2026	gasoline	Other Category	${'$'}3	
02-07-2026	lunch	Other Category	${'$'}1.75	
03-07-2026	ចូលបុណ្យ	Other Category	${'$'}12.50	
03-07-2026	coca-cola	Other Category	${'$'}0.50	
03-07-2026	Join running event a	Other Category	${'$'}18	
04-07-2026	running event at the	Other Category	${'$'}3.89	
04-07-2026	តុរឿងសុល	Other Category	${'$'}0.50	
04-07-2026	party with leap team	Other Category	${'$'}40	
04-07-2026	ជួសជុលម៉ូតូ	Other Category	${'$'}60.50	
04-07-2026	tik tuk ride	Other Category	${'$'}5	
04-07-2026	យកម៉ាក់មកពេទ្យ	Other Category	${'$'}100	
04-07-2026	តាបៃតែងទឹកដោះគោ	Other Category	${'$'}1	
""".trimIndent()

    fun detectDelimiter(content: String): ImportDelimiter {
        val lines = content.lines().filter { it.isNotBlank() }.take(5)
        if (lines.isEmpty()) return ImportDelimiter.TAB

        var tabCount = 0
        var commaCount = 0
        var semicolonCount = 0
        var pipeCount = 0

        lines.forEach { line ->
            tabCount += line.count { it == '\t' }
            commaCount += line.count { it == ',' }
            semicolonCount += line.count { it == ';' }
            pipeCount += line.count { it == '|' }
        }

        return when {
            tabCount >= commaCount && tabCount >= semicolonCount && tabCount >= pipeCount && tabCount > 0 -> ImportDelimiter.TAB
            semicolonCount > commaCount && semicolonCount > tabCount -> ImportDelimiter.SEMICOLON
            commaCount >= tabCount && commaCount > 0 -> ImportDelimiter.COMMA
            pipeCount > 0 -> ImportDelimiter.PIPE
            else -> ImportDelimiter.TAB
        }
    }

    suspend fun readFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open stream for Uri: $uri")
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }

    fun parseRawTable(
        content: String,
        specifiedDelimiter: ImportDelimiter? = null,
        userSpecifiedHasHeader: Boolean? = null
    ): ParsedRawTable {
        val delimiter = specifiedDelimiter ?: detectDelimiter(content)
        val rawLines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }

        if (rawLines.isEmpty()) {
            return ParsedRawTable(
                delimiter = delimiter,
                hasHeaderRow = false,
                headerNames = emptyList(),
                rawRows = emptyList(),
                suggestedMappings = emptyList()
            )
        }

        val parsedRows = rawLines.map { parseLine(it, delimiter.char) }
        val maxCols = parsedRows.maxOfOrNull { it.size } ?: 0

        val normalizedRows = parsedRows.map { row ->
            if (row.size < maxCols) {
                row + List(maxCols - row.size) { "" }
            } else {
                row
            }
        }

        val firstRow = normalizedRows.firstOrNull() ?: emptyList()
        val detectedHasHeader = userSpecifiedHasHeader ?: isLikelyHeaderRow(firstRow)

        val headerNames = if (detectedHasHeader) {
            firstRow.mapIndexed { idx, col ->
                col.ifBlank { "Column ${('A'.code + idx).toChar()}" }
            }
        } else {
            List(maxCols) { idx -> "Column ${('A'.code + idx).toChar()}" }
        }

        val dataRows = if (detectedHasHeader && normalizedRows.size > 1) {
            normalizedRows.drop(1)
        } else if (!detectedHasHeader) {
            normalizedRows
        } else {
            emptyList()
        }

        val suggestedMappings = suggestColumnMappings(headerNames, dataRows)

        return ParsedRawTable(
            delimiter = delimiter,
            hasHeaderRow = detectedHasHeader,
            headerNames = headerNames,
            rawRows = dataRows,
            suggestedMappings = suggestedMappings
        )
    }

    private fun isLikelyHeaderRow(firstRow: List<String>): Boolean {
        if (firstRow.isEmpty()) return false
        val headerKeywords = listOf("date", "desc", "category", "amount", "note", "price", "title", "កាលបរិច្ឆេទ", "ប្រភេទ", "ចំនួន")
        return firstRow.any { col ->
            val lower = col.lowercase().trim()
            headerKeywords.any { kw -> lower.contains(kw) }
        }
    }

    fun suggestColumnMappings(headers: List<String>, sampleRows: List<List<String>>): List<ImportTargetField> {
        val assigned = mutableSetOf<ImportTargetField>()

        return headers.mapIndexed { colIdx, header ->
            val lowerHeader = header.lowercase().trim()
            val sampleValues = sampleRows.mapNotNull { it.getOrNull(colIdx)?.trim() }.filter { it.isNotEmpty() }

            val suggested = when {
                !assigned.contains(ImportTargetField.DATE) &&
                        (lowerHeader.contains("date") || lowerHeader.contains("time") || lowerHeader.contains("កាលបរិច្ឆេទ") ||
                                sampleValues.any { isLikelyDateString(it) }) -> {
                    ImportTargetField.DATE
                }

                !assigned.contains(ImportTargetField.AMOUNT) &&
                        (lowerHeader.contains("amount") || lowerHeader.contains("price") || lowerHeader.contains("cost") ||
                                lowerHeader.contains("total") || lowerHeader.contains("ចំនួន") || lowerHeader.contains("តម្លៃ") ||
                                lowerHeader.contains("$") || sampleValues.any { isLikelyAmountString(it) }) -> {
                    ImportTargetField.AMOUNT
                }

                !assigned.contains(ImportTargetField.CATEGORY) &&
                        (lowerHeader.contains("category") || lowerHeader.contains("cat") || lowerHeader.contains("ប្រភេទ")) -> {
                    ImportTargetField.CATEGORY
                }

                !assigned.contains(ImportTargetField.DESCRIPTION) &&
                        (lowerHeader.contains("description") || lowerHeader.contains("desc") || lowerHeader.contains("short description") ||
                                lowerHeader.contains("title") || lowerHeader.contains("name") || lowerHeader.contains("item") ||
                                lowerHeader.contains("បរិយាយ") || lowerHeader.contains("ចំណាំ")) -> {
                    ImportTargetField.DESCRIPTION
                }

                !assigned.contains(ImportTargetField.OPTIONAL_NOTES) &&
                        (lowerHeader.contains("optional") || lowerHeader.contains("note") || lowerHeader.contains("memo") ||
                                lowerHeader.contains("remark") || lowerHeader.contains("កំណត់ចំណាំ")) -> {
                    ImportTargetField.OPTIONAL_NOTES
                }

                !assigned.contains(ImportTargetField.TYPE) &&
                        (lowerHeader.contains("type") || lowerHeader.contains("income/expense")) -> {
                    ImportTargetField.TYPE
                }

                else -> ImportTargetField.SKIP
            }

            if (suggested != ImportTargetField.SKIP) {
                assigned.add(suggested)
            }
            suggested
        }
    }

    fun buildPreviewItems(
        parsedTable: ParsedRawTable,
        columnMappings: List<ImportTargetField>,
        defaultType: String = "expense"
    ): List<ImportPreviewItem> {
        val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        return parsedTable.rawRows.mapIndexedNotNull { index, row ->
            // Skip entirely blank rows
            if (row.all { it.isBlank() }) return@mapIndexedNotNull null

            var rawDate = ""
            var rawDesc = ""
            var rawCategory = ""
            var rawAmount = ""
            var rawNotes = ""
            var rawType = defaultType

            columnMappings.forEachIndexed { colIdx, target ->
                val value = row.getOrNull(colIdx)?.trim() ?: ""
                when (target) {
                    ImportTargetField.DATE -> rawDate = value
                    ImportTargetField.DESCRIPTION -> rawDesc = value
                    ImportTargetField.CATEGORY -> rawCategory = value
                    ImportTargetField.AMOUNT -> rawAmount = value
                    ImportTargetField.OPTIONAL_NOTES -> rawNotes = value
                    ImportTargetField.TYPE -> if (value.isNotBlank()) rawType = value
                    ImportTargetField.SKIP -> {}
                }
            }

            val parsedDate = parseDateToMillis(rawDate)
            val dateFormatted = if (parsedDate != null) {
                displayDateFormat.format(Date(parsedDate))
            } else {
                rawDate.ifBlank { "Current Date" }
            }

            val parsedAmount = parseAmount(rawAmount)

            val combinedDescription = when {
                rawDesc.isNotBlank() && rawNotes.isNotBlank() -> "$rawDesc ($rawNotes)"
                rawDesc.isNotBlank() -> rawDesc
                rawNotes.isNotBlank() -> rawNotes
                else -> "Transaction #${index + 1}"
            }

            val finalCategoryName = rawCategory.ifBlank { "Other Expense" }

            val inferredType = when {
                rawType.equals("income", ignoreCase = true) || rawType.contains("ចំណូល") -> "income"
                else -> "expense"
            }

            val isValid = parsedAmount != null && parsedAmount > 0.0
            val validationMsg = when {
                parsedAmount == null -> "Invalid amount: '$rawAmount'"
                parsedAmount <= 0.0 -> "Amount must be greater than 0"
                else -> "Valid"
            }

            ImportPreviewItem(
                rowIndex = index + 1,
                rawRow = row,
                date = parsedDate ?: System.currentTimeMillis(),
                dateFormatted = dateFormatted,
                description = combinedDescription,
                categoryName = finalCategoryName,
                amount = parsedAmount ?: 0.0,
                type = inferredType,
                isValid = isValid,
                validationMessage = validationMsg,
                isSelected = isValid
            )
        }
    }

    fun parseLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    // Escaped quote
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                result.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString().trim())
        return result
    }

    fun parseAmount(amountStr: String): Double? {
        if (amountStr.isBlank()) return null
        val cleaned = amountStr
            .replace("$", "")
            .replace("៛", "")
            .replace("USD", "", ignoreCase = true)
            .replace("KHR", "", ignoreCase = true)
            .replace("€", "")
            .replace("£", "")
            .replace(",", "")
            .replace("(", "-")
            .replace(")", "")
            .trim()

        return cleaned.toDoubleOrNull()?.let { Math.abs(it) }
    }

    private val supportedDateFormats = listOf(
        SimpleDateFormat("dd-MM-yyyy", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US),
        SimpleDateFormat("yyyy/MM/dd", Locale.US),
        SimpleDateFormat("dd.MM.yyyy", Locale.US),
        SimpleDateFormat("d-M-yyyy", Locale.US),
        SimpleDateFormat("d/M/yyyy", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US),
        SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    )

    fun parseDateToMillis(dateStr: String): Long? {
        if (dateStr.isBlank()) return null
        val cleanStr = dateStr.trim()

        for (format in supportedDateFormats) {
            try {
                format.isLenient = false
                val parsed = format.parse(cleanStr)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun isLikelyDateString(str: String): Boolean {
        val s = str.trim()
        return s.matches(Regex("""^\d{1,4}[-/. ]\d{1,2}[-/. ]\d{1,4}(.*)?"""))
    }

    private fun isLikelyAmountString(str: String): Boolean {
        val s = str.trim()
        return s.contains("$") || s.contains("៛") || s.matches(Regex("""^[-+]?[0-9]{1,3}(,[0-9]{3})*(\.[0-9]+)?$""")) || s.matches(Regex("""^[-+]?[0-9]+(\.[0-9]+)?$"""))
    }
}
