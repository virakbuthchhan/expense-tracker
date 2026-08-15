package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.findMainActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.service.ImportDelimiter
import com.example.data.service.ImportExecutionResult
import com.example.data.service.ImportPreviewItem
import com.example.data.service.ImportTargetField
import com.example.data.service.ParsedRawTable
import com.example.data.service.TsvCsvImportService
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.Emerald500
import com.example.ui.theme.ExpenseRed
import com.example.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences by viewModel.userPreferences.collectAsState()

    // 0: Source Input, 1: Column Mapping, 2: Preview & Import
    var currentStep by remember { mutableIntStateOf(0) }

    // Source Data State
    var rawText by remember { mutableStateOf("") }
    var selectedDelimiter by remember { mutableStateOf(ImportDelimiter.TAB) }
    var hasHeaderRow by remember { mutableStateOf(true) }
    var defaultTransactionType by remember { mutableStateOf("expense") }

    // Parsed Data State
    var parsedTable by remember {
        mutableStateOf(
            ParsedRawTable(
                delimiter = ImportDelimiter.TAB,
                hasHeaderRow = true,
                headerNames = emptyList(),
                rawRows = emptyList(),
                suggestedMappings = emptyList()
            )
        )
    }

    val columnMappings = remember { mutableStateListOf<ImportTargetField>() }
    val previewItems = remember { mutableStateListOf<ImportPreviewItem>() }

    // Import Execution State
    var isImporting by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<ImportExecutionResult?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    fun pickFileSafely() {
        val activity = context.findMainActivity()
        if (activity != null) {
            activity.launchFilePicker { uri: Uri? ->
                if (uri != null) {
                    scope.launch {
                        try {
                            val content = TsvCsvImportService.readFromUri(context, uri)
                            if (content.isNotBlank()) {
                                rawText = content
                                val detected = TsvCsvImportService.detectDelimiter(content)
                                selectedDelimiter = detected
                                Toast.makeText(context, "Loaded file (${content.lines().filter { it.isNotBlank() }.size} lines)", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Selected file is empty", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        } else {
            Toast.makeText(context, "Cannot open file picker", Toast.LENGTH_SHORT).show()
        }
    }

    fun parseAndGoToMapping() {
        if (rawText.isBlank()) {
            Toast.makeText(context, "Please enter or select TSV/CSV content first.", Toast.LENGTH_SHORT).show()
            return
        }

        val table = TsvCsvImportService.parseRawTable(
            content = rawText,
            specifiedDelimiter = selectedDelimiter,
            userSpecifiedHasHeader = hasHeaderRow
        )
        parsedTable = table
        columnMappings.clear()
        columnMappings.addAll(table.suggestedMappings)

        currentStep = 1
    }

    fun generatePreviewAndProceed() {
        val items = TsvCsvImportService.buildPreviewItems(
            parsedTable = parsedTable,
            columnMappings = columnMappings.toList(),
            defaultType = defaultTransactionType
        )
        previewItems.clear()
        previewItems.addAll(items)
        currentStep = 2
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.importTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.importSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) {
                            currentStep--
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stepper Tabs
            ImportStepperHeader(
                currentStep = currentStep,
                onStepClick = { step ->
                    if (step < currentStep) {
                        currentStep = step
                    } else if (step == 1 && rawText.isNotBlank()) {
                        parseAndGoToMapping()
                    } else if (step == 2 && columnMappings.isNotEmpty()) {
                        generatePreviewAndProceed()
                    }
                }
            )

            // Step Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (currentStep) {
                    0 -> StepSourceInput(
                        rawText = rawText,
                        onRawTextChanged = { rawText = it },
                        selectedDelimiter = selectedDelimiter,
                        onDelimiterSelected = { selectedDelimiter = it },
                        hasHeaderRow = hasHeaderRow,
                        onHasHeaderRowChanged = { hasHeaderRow = it },
                        defaultType = defaultTransactionType,
                        onDefaultTypeChanged = { defaultTransactionType = it },
                        onPickFileClick = {
                            pickFileSafely()
                        },
                        onLoadSampleClick = {
                            rawText = TsvCsvImportService.SAMPLE_TSV_DATA
                            selectedDelimiter = ImportDelimiter.TAB
                            hasHeaderRow = true
                            Toast.makeText(context, "Loaded sample TSV data from screenshot", Toast.LENGTH_SHORT).show()
                        },
                        onContinueClick = {
                            parseAndGoToMapping()
                        }
                    )

                    1 -> StepColumnMapping(
                        parsedTable = parsedTable,
                        columnMappings = columnMappings,
                        onMappingChanged = { index, newField ->
                            columnMappings[index] = newField
                        },
                        onBackClick = { currentStep = 0 },
                        onProceedClick = { generatePreviewAndProceed() }
                    )

                    2 -> StepPreviewAndImport(
                        previewItems = previewItems,
                        currencySymbol = preferences.currencySymbol,
                        isImporting = isImporting,
                        onToggleItem = { index ->
                            val current = previewItems[index]
                            previewItems[index] = current.copy(isSelected = !current.isSelected)
                        },
                        onSelectAll = { select ->
                            val updated = previewItems.map { it.copy(isSelected = if (it.isValid) select else false) }
                            previewItems.clear()
                            previewItems.addAll(updated)
                        },
                        onBackClick = { currentStep = 1 },
                        onImportClick = {
                            val itemsToImport = previewItems.filter { it.isSelected && it.isValid }
                            if (itemsToImport.isEmpty()) {
                                Toast.makeText(context, strings.noRecordsToImport, Toast.LENGTH_SHORT).show()
                                return@StepPreviewAndImport
                            }

                            isImporting = true
                            viewModel.importTransactions(itemsToImport) { result ->
                                isImporting = false
                                importResult = result
                                showSuccessDialog = true
                            }
                        }
                    )
                }
            }
        }
    }

    // Success Dialog
    if (showSuccessDialog && importResult != null) {
        val result = importResult!!
        AlertDialog(
            onDismissRequest = { /* require button click */ },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Emerald500.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Emerald500,
                        modifier = Modifier.size(36.dp)
                    )
                }
            },
            title = {
                Text(
                    text = strings.importSuccessTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), strings.importSuccessMessage, result.totalImported),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = strings.recordsFoundCount, style = MaterialTheme.typography.bodySmall)
                                Text(text = "${result.totalImported}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = strings.totalAmountToImport, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = "${preferences.currencySymbol}${String.format(Locale.US, "%.2f", result.totalAmount)}",
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald500,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (result.createdCategoriesCount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Categories Created", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "${result.createdCategoriesCount}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateToTransactions()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(strings.viewTransactions)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateToDashboard()
                    }
                ) {
                    Text(strings.backToDashboard)
                }
            }
        )
    }
}

@Composable
fun ImportStepperHeader(
    currentStep: Int,
    onStepClick: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    val steps = listOf(
        strings.importSourceStep,
        strings.importMappingStep,
        strings.importPreviewStep
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, stepName ->
                val isActive = currentStep == index
                val isCompleted = currentStep > index

                val indicatorColor = when {
                    isActive -> MaterialTheme.colorScheme.primary
                    isCompleted -> Emerald500
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onStepClick(index) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(indicatorColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stepName.substringAfter(". "),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (index < steps.size - 1) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    )
                }
            }
        }
    }
}

@Composable
fun StepSourceInput(
    rawText: String,
    onRawTextChanged: (String) -> Unit,
    selectedDelimiter: ImportDelimiter,
    onDelimiterSelected: (ImportDelimiter) -> Unit,
    hasHeaderRow: Boolean,
    onHasHeaderRowChanged: (Boolean) -> Unit,
    defaultType: String,
    onDefaultTypeChanged: (String) -> Unit,
    onPickFileClick: () -> Unit,
    onLoadSampleClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPickFileClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = strings.pickFileButton, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            OutlinedButton(
                onClick = onLoadSampleClick,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = strings.loadSampleDataButton, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // Format Settings Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = strings.delimiterLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImportDelimiter.values().forEach { delimiter ->
                        FilterChip(
                            selected = selectedDelimiter == delimiter,
                            onClick = { onDelimiterSelected(delimiter) },
                            label = { Text(delimiter.label, fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Toggle Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.hasHeaderLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "First line is used as column names",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = hasHeaderRow,
                        onCheckedChange = onHasHeaderRowChanged,
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Default Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.defaultTypeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = defaultType == "expense",
                            onClick = { onDefaultTypeChanged("expense") },
                            label = { Text(strings.filterExpense) },
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = defaultType == "income",
                            onClick = { onDefaultTypeChanged("income") },
                            label = { Text(strings.filterIncome) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // Raw Text Box
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.pasteTextButton,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                if (rawText.isNotBlank()) {
                    Text(
                        text = "${rawText.lines().filter { it.isNotBlank() }.size} lines",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            OutlinedTextField(
                value = rawText,
                onValueChange = onRawTextChanged,
                placeholder = {
                    Text(strings.pastePlaceholder, fontSize = 13.sp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
            )
        }

        // Proceed Button
        Button(
            onClick = onContinueClick,
            enabled = rawText.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(strings.readyToPreview, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun StepColumnMapping(
    parsedTable: ParsedRawTable,
    columnMappings: List<ImportTargetField>,
    onMappingChanged: (Int, ImportTargetField) -> Unit,
    onBackClick: () -> Unit,
    onProceedClick: () -> Unit
) {
    val strings = LocalAppStrings.current

    val hasAmountMapped = columnMappings.contains(ImportTargetField.AMOUNT)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Explanatory Banner
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.mapColumnSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Columns Mapping List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(parsedTable.headerNames) { colIndex, headerName ->
                val currentTarget = columnMappings.getOrNull(colIndex) ?: ImportTargetField.SKIP
                val sampleValues = parsedTable.rawRows.take(3).mapNotNull { it.getOrNull(colIndex) }.filter { it.isNotBlank() }

                ColumnMappingCard(
                    columnIndex = colIndex,
                    headerName = headerName,
                    sampleValues = sampleValues,
                    currentTarget = currentTarget,
                    onTargetSelected = { onMappingChanged(colIndex, it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBackClick,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(strings.cancel)
            }

            Button(
                onClick = onProceedClick,
                enabled = hasAmountMapped,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
            ) {
                Text(strings.readyToPreview, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ColumnMappingCard(
    columnIndex: Int,
    headerName: String,
    sampleValues: List<String>,
    currentTarget: ImportTargetField,
    onTargetSelected: (ImportTargetField) -> Unit
) {
    val strings = LocalAppStrings.current
    var showDropdown by remember { mutableStateOf(false) }

    val targetFieldLabel = when (currentTarget) {
        ImportTargetField.DATE -> strings.targetFieldDate
        ImportTargetField.DESCRIPTION -> strings.targetFieldDesc
        ImportTargetField.CATEGORY -> strings.targetFieldCategory
        ImportTargetField.AMOUNT -> strings.targetFieldAmount
        ImportTargetField.OPTIONAL_NOTES -> strings.targetFieldOptionalNotes
        ImportTargetField.TYPE -> strings.targetFieldType
        ImportTargetField.SKIP -> strings.targetFieldSkip
    }

    val targetFieldIcon = when (currentTarget) {
        ImportTargetField.DATE -> Icons.Default.DateRange
        ImportTargetField.DESCRIPTION -> Icons.Default.Description
        ImportTargetField.CATEGORY -> Icons.Default.Category
        ImportTargetField.AMOUNT -> Icons.Default.Paid
        ImportTargetField.OPTIONAL_NOTES -> Icons.Default.Notes
        ImportTargetField.TYPE -> Icons.Default.TableChart
        ImportTargetField.SKIP -> Icons.Default.Close
    }

    val isMapped = currentTarget != ImportTargetField.SKIP

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMapped) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isMapped) 1.dp else 0.dp),
        border = if (isMapped) borderStrokeForField(currentTarget) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${('A'.code + columnIndex).toChar()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = headerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Field Selector Dropdown Anchor
                Box {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isMapped) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showDropdown = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = targetFieldIcon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isMapped) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = targetFieldLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isMapped) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        ImportTargetField.values().forEach { target ->
                            val label = when (target) {
                                ImportTargetField.DATE -> strings.targetFieldDate
                                ImportTargetField.DESCRIPTION -> strings.targetFieldDesc
                                ImportTargetField.CATEGORY -> strings.targetFieldCategory
                                ImportTargetField.AMOUNT -> strings.targetFieldAmount
                                ImportTargetField.OPTIONAL_NOTES -> strings.targetFieldOptionalNotes
                                ImportTargetField.TYPE -> strings.targetFieldType
                                ImportTargetField.SKIP -> strings.targetFieldSkip
                            }
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onTargetSelected(target)
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Sample values preview
            if (sampleValues.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strings.sampleValuesLabel}: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = sampleValues.joinToString(separator = ", ") { "\"$it\"" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun borderStrokeForField(target: ImportTargetField): androidx.compose.foundation.BorderStroke {
    val color = when (target) {
        ImportTargetField.AMOUNT -> Emerald500.copy(alpha = 0.7f)
        ImportTargetField.DATE -> Color(0xFF3B82F6).copy(alpha = 0.7f)
        ImportTargetField.DESCRIPTION -> Color(0xFF8B5CF6).copy(alpha = 0.7f)
        ImportTargetField.CATEGORY -> Color(0xFFF59E0B).copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    return androidx.compose.foundation.BorderStroke(1.dp, color)
}

@Composable
fun StepPreviewAndImport(
    previewItems: List<ImportPreviewItem>,
    currencySymbol: String,
    isImporting: Boolean,
    onToggleItem: (Int) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onImportClick: () -> Unit
) {
    val strings = LocalAppStrings.current

    val totalRecords = previewItems.size
    val selectedCount = previewItems.count { it.isSelected && it.isValid }
    val selectedSum = previewItems.filter { it.isSelected && it.isValid }.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Summary Metrics Bar
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = strings.recordsFoundCount, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$totalRecords", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = strings.validRecordsCount, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$selectedCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Emerald500)
                }

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = strings.totalAmountToImport, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%.2f", selectedSum)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Select All / Deselect All Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Preview Transactions ($selectedCount selected)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onSelectAll(true) }) {
                    Icon(imageVector = Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.selectAll, fontSize = 12.sp)
                }
                TextButton(onClick = { onSelectAll(false) }) {
                    Icon(imageVector = Icons.Default.RemoveDone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.deselectAll, fontSize = 12.sp)
                }
            }
        }

        // Transactions List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(previewItems) { index, item ->
                PreviewItemCard(
                    item = item,
                    currencySymbol = currencySymbol,
                    onToggle = { onToggleItem(index) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBackClick,
                shape = RoundedCornerShape(14.dp),
                enabled = !isImporting,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Mapping")
            }

            Button(
                onClick = onImportClick,
                enabled = selectedCount > 0 && !isImporting,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                modifier = Modifier
                    .weight(2f)
                    .height(50.dp)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importing...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${strings.importButtonAction} ($selectedCount)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PreviewItemCard(
    item: ImportPreviewItem,
    currencySymbol: String,
    onToggle: () -> Unit
) {
    val isExpense = item.type == "expense"
    val amountPrefix = if (isExpense) "-" else "+"
    val amountColor = if (isExpense) ExpenseRed else Emerald500

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected && item.isValid) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isSelected && item.isValid) 1.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = item.isValid) { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isSelected && item.isValid,
                onCheckedChange = { onToggle() },
                enabled = item.isValid,
                colors = CheckboxDefaults.colors(checkedColor = Emerald500)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = item.dateFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = item.categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                }

                if (!item.isValid) {
                    Text(
                        text = item.validationMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = ExpenseRed,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "$amountPrefix$currencySymbol${String.format(Locale.US, "%.2f", item.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (item.isValid) amountColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
