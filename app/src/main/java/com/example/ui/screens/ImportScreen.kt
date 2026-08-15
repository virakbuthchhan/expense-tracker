package com.example.ui.screens

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.findMainActivity
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
    val snackbarHostState = remember { SnackbarHostState() }

    // Source Data State
    var rawText by remember { mutableStateOf("") }
    var selectedDelimiter by remember { mutableStateOf(ImportDelimiter.TAB) }
    var hasHeaderRow by remember { mutableStateOf(true) }
    var defaultTransactionType by remember { mutableStateOf("expense") }
    var isParsing by remember { mutableStateOf(false) }

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
                        isParsing = true
                        try {
                            val content = TsvCsvImportService.readFromUri(context, uri)
                            if (content.isNotBlank()) {
                                rawText = content
                                val detected = TsvCsvImportService.detectDelimiter(content)
                                selectedDelimiter = detected
                                val lineCount = content.lines().filter { it.isNotBlank() }.size
                                val msg = String.format(Locale.getDefault(), strings.parsingSuccessSnackbar, lineCount)
                                snackbarHostState.showSnackbar(
                                    message = msg,
                                    withDismissAction = true
                                )
                            } else {
                                snackbarHostState.showSnackbar(
                                    message = "Selected file is empty.",
                                    withDismissAction = true
                                )
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                message = "Error reading file: ${e.message}",
                                withDismissAction = true
                            )
                        } finally {
                            isParsing = false
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
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Please enter or select TSV/CSV content first.",
                    withDismissAction = true
                )
            }
            return
        }

        scope.launch {
            isParsing = true
            try {
                val table = TsvCsvImportService.parseRawTable(
                    content = rawText,
                    specifiedDelimiter = selectedDelimiter,
                    userSpecifiedHasHeader = hasHeaderRow
                )
                parsedTable = table
                columnMappings.clear()
                columnMappings.addAll(table.suggestedMappings)
                currentStep = 1
                val totalRows = table.rawRows.size
                val msg = String.format(Locale.getDefault(), strings.parsingSuccessSnackbar, totalRows)
                snackbarHostState.showSnackbar(
                    message = msg,
                    withDismissAction = true
                )
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Failed to parse content: ${e.message}",
                    withDismissAction = true
                )
            } finally {
                isParsing = false
            }
        }
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
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        shape = RoundedCornerShape(12.dp),
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        dismissActionContentColor = MaterialTheme.colorScheme.primary
                    )
                }
            )
        },
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
            // Animated parsing loading indicator
            AnimatedVisibility(visible = isParsing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.parsingFileProgress,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                }
            }

            // High-Polish Responsive Stepper
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
                        isParsing = isParsing,
                        onPickFileClick = { pickFileSafely() },
                        onLoadSampleClick = {
                            rawText = TsvCsvImportService.SAMPLE_TSV_DATA
                            selectedDelimiter = ImportDelimiter.TAB
                            hasHeaderRow = true
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Loaded sample TSV data. Tap 'Proceed to Mapping'.",
                                    withDismissAction = true
                                )
                            }
                        },
                        onContinueClick = { parseAndGoToMapping() }
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
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Emerald500,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    text = strings.importSuccessTitle,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), strings.importSuccessMessage, result.totalImported),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = strings.validRecordsCount, style = MaterialTheme.typography.bodySmall)
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
    val stepLabels = listOf(
        strings.stepSourceShort,
        strings.stepMappingShort,
        strings.stepPreviewShort
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            stepLabels.forEachIndexed { index, stepLabel ->
                val isActive = currentStep == index
                val isCompleted = currentStep > index

                val containerBg = when {
                    isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    isCompleted -> Emerald500.copy(alpha = 0.12f)
                    else -> Color.Transparent
                }

                val contentColor = when {
                    isActive -> MaterialTheme.colorScheme.primary
                    isCompleted -> Emerald500
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(containerBg)
                        .clickable { onStepClick(index) }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else if (isCompleted) Emerald500
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
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
                        text = stepLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
    isParsing: Boolean = false,
    onPickFileClick: () -> Unit,
    onLoadSampleClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPickFileClick,
                enabled = !isParsing,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = strings.pickFileButton, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onLoadSampleClick,
                enabled = !isParsing,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = strings.loadSampleDataButton, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            }
        }

        // Format Settings Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Delimiter selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.delimiterLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ImportDelimiter.values().forEach { delimiter ->
                            FilterChip(
                                selected = selectedDelimiter == delimiter,
                                onClick = { onDelimiterSelected(delimiter) },
                                label = { Text(delimiter.label, fontSize = 11.5.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // Toggle Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.hasHeaderLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = hasHeaderRow,
                        onCheckedChange = onHasHeaderRowChanged,
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // Default Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.defaultTypeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = defaultType == "expense",
                            onClick = { onDefaultTypeChanged("expense") },
                            label = { Text(strings.filterExpense, fontSize = 11.5.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = defaultType == "income",
                            onClick = { onDefaultTypeChanged("income") },
                            label = { Text(strings.filterIncome, fontSize = 11.5.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // Raw Text Box with Paste & Clear controls
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (rawText.isNotBlank()) {
                        Text(
                            text = "${rawText.lines().filter { it.isNotBlank() }.size} lines",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = { onRawTextChanged("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            if (clipboard != null && clipboard.hasPrimaryClip()) {
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).coerceToText(context).toString()
                                    if (text.isNotBlank()) {
                                        onRawTextChanged(text)
                                        val detected = TsvCsvImportService.detectDelimiter(text)
                                        onDelimiterSelected(detected)
                                    }
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste", fontSize = 12.sp)
                    }
                }
            }

            OutlinedTextField(
                value = rawText,
                onValueChange = onRawTextChanged,
                placeholder = {
                    Text(strings.pastePlaceholder, fontSize = 12.5.sp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(14.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
            )
        }

        // Proceed Button
        Button(
            onClick = onContinueClick,
            enabled = rawText.isNotBlank() && !isParsing,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isParsing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.parsingFileProgress, fontWeight = FontWeight.Bold)
            } else {
                Text(strings.readyToPreview, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
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
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

        Spacer(modifier = Modifier.height(10.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBackClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Text(strings.cancel)
            }

            Button(
                onClick = onProceedClick,
                enabled = hasAmountMapped,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(46.dp)
            ) {
                Text(strings.readyToPreview, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMapped) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isMapped) 1.dp else 0.dp),
        border = if (isMapped) borderStrokeForField(currentTarget) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${('A'.code + columnIndex).toChar()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
                        shape = RoundedCornerShape(8.dp),
                        color = if (isMapped) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDropdown = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = targetFieldIcon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isMapped) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = targetFieldLabel,
                                style = MaterialTheme.typography.labelSmall,
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
    var previewSearchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("all") } // all, expense, income

    val totalRecords = previewItems.size
    val selectedCount = previewItems.count { it.isSelected && it.isValid }
    val selectedSum = previewItems.filter { it.isSelected && it.isValid }.sumOf { it.amount }

    // Filter preview items based on user search and filter
    val displayedItems = previewItems.mapIndexed { index, item -> Pair(index, item) }.filter { (_, item) ->
        val matchesQuery = previewSearchQuery.isBlank() ||
                item.description.contains(previewSearchQuery, ignoreCase = true) ||
                item.categoryName.contains(previewSearchQuery, ignoreCase = true) ||
                item.dateFormatted.contains(previewSearchQuery, ignoreCase = true)

        val matchesType = when (filterType) {
            "expense" -> item.type == "expense"
            "income" -> item.type == "income"
            else -> true
        }

        matchesQuery && matchesType
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Summary Metrics Bar
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = strings.recordsFoundCount, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text(text = "$totalRecords", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = strings.validRecordsCount, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text(text = "$selectedCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Emerald500)
                }

                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = strings.totalAmountToImport, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.2f", selectedSum)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search & Controls in Preview
        OutlinedTextField(
            value = previewSearchQuery,
            onValueChange = { previewSearchQuery = it },
            placeholder = { Text(strings.searchPlaceholder, fontSize = 12.sp) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            trailingIcon = {
                if (previewSearchQuery.isNotBlank()) {
                    IconButton(onClick = { previewSearchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Select All / Deselect All Controls and Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    FilterChip(
                        selected = filterType == "all",
                        onClick = { filterType = "all" },
                        label = { Text("${strings.filterAll} (${previewItems.size})", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = filterType == "expense",
                        onClick = { filterType = "expense" },
                        label = { Text("${strings.filterExpense} (${previewItems.count { it.type == "expense" }})", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = filterType == "income",
                        onClick = { filterType = "income" },
                        label = { Text("${strings.filterIncome} (${previewItems.count { it.type == "income" }})", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(
                    onClick = { onSelectAll(true) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(strings.selectAll, fontSize = 11.sp)
                }
                TextButton(
                    onClick = { onSelectAll(false) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.RemoveDone, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(strings.deselectAll, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Transactions List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 6.dp)
        ) {
            itemsIndexed(displayedItems) { _, (originalIndex, item) ->
                PreviewItemCard(
                    item = item,
                    currencySymbol = currencySymbol,
                    onToggle = { onToggleItem(originalIndex) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBackClick,
                shape = RoundedCornerShape(12.dp),
                enabled = !isImporting,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(strings.stepMappingShort)
            }

            Button(
                onClick = onImportClick,
                enabled = selectedCount > 0 && !isImporting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                modifier = Modifier
                    .weight(2f)
                    .height(48.dp)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected && item.isValid) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isSelected && item.isValid) 1.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = item.isValid) { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isSelected && item.isValid,
                onCheckedChange = { onToggle() },
                enabled = item.isValid,
                colors = CheckboxDefaults.colors(checkedColor = Emerald500),
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = item.dateFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = item.categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            maxLines = 1,
                            fontSize = 10.5.sp
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

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "$amountPrefix$currencySymbol${String.format(Locale.US, "%.2f", item.amount)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (item.isValid) amountColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
