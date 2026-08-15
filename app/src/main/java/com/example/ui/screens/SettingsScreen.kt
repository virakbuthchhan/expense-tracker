package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.Emerald500
import com.example.ui.theme.ExpenseRed
import com.example.ui.viewmodel.ExpenseViewModel

data class CurrencyOption(
    val code: String,
    val name: String,
    val symbol: String
)

val availableCurrencies = listOf(
    CurrencyOption("USD", "US Dollar", "$"),
    CurrencyOption("KHR", "Cambodian Riel", "៛"),
    CurrencyOption("EUR", "Euro", "€"),
    CurrencyOption("GBP", "British Pound", "£"),
    CurrencyOption("JPY", "Japanese Yen", "¥"),
    CurrencyOption("CAD", "Canadian Dollar", "$"),
    CurrencyOption("AUD", "Australian Dollar", "$"),
    CurrencyOption("INR", "Indian Rupee", "₹"),
    CurrencyOption("CNY", "Chinese Yuan", "¥"),
    CurrencyOption("SGD", "Singapore Dollar", "$"),
    CurrencyOption("CHF", "Swiss Franc", "Fr")
)

@Composable
fun SettingsScreen(
    viewModel: ExpenseViewModel,
    onNavigateToCategories: () -> Unit,
    onNavigateToExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val strings = LocalAppStrings.current
    val currentLanguage = AppLanguage.fromCode(preferences.language)

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = strings.settingsTitle,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = strings.settingsSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // General Preferences Group
        item {
            Text(
                text = strings.preferencesGroup,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Language Setting Card
        item {
            SettingsActionCard(
                icon = Icons.Default.Language,
                iconColor = Color(0xFF0D9488),
                title = strings.language,
                subtitle = "${currentLanguage.flag} ${currentLanguage.nativeName} (${currentLanguage.displayName})",
                onClick = { showLanguageDialog = true }
            )
        }

        // Currency Setting Card
        item {
            SettingsActionCard(
                icon = Icons.Default.AttachMoney,
                iconColor = Emerald500,
                title = strings.baseCurrency,
                subtitle = "${preferences.currencyCode} (${preferences.currencySymbol})",
                onClick = { showCurrencyDialog = true }
            )
        }

        // Manage Categories Card
        item {
            SettingsActionCard(
                icon = Icons.Default.Category,
                iconColor = Color(0xFF3B82F6),
                title = strings.manageCategories,
                subtitle = strings.manageCategoriesSubtitle,
                onClick = onNavigateToCategories
            )
        }

        // Data Export Card
        item {
            SettingsActionCard(
                icon = Icons.Default.FileDownload,
                iconColor = Color(0xFF8B5CF6),
                title = strings.exportDataAction,
                subtitle = strings.exportDataSubtitle,
                onClick = onNavigateToExport
            )
        }

        // Security & App Lock Group
        item {
            Text(
                text = strings.securityGroup,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // PIN App Lock Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (preferences.isAppLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "App Lock",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = strings.appLock,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (preferences.isAppLockEnabled) strings.pinActive else strings.pinInactive,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = preferences.isAppLockEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showPinSetupDialog = true
                            } else {
                                viewModel.setAppLock(false, "")
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Emerald500
                        )
                    )
                }
            }
        }

        // Data Management Group
        item {
            Text(
                text = strings.dataManagementGroup,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // Reset Data Card
        item {
            SettingsActionCard(
                icon = Icons.Default.Refresh,
                iconColor = ExpenseRed,
                title = strings.resetAllData,
                subtitle = strings.resetAllSubtitle,
                onClick = { showResetConfirmDialog = true }
            )
        }
    }

    // Language Picker Dialog
    if (showLanguageDialog) {
        Dialog(onDismissRequest = { showLanguageDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = strings.selectLanguage,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            val isSelected = preferences.language.equals(lang.code, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .clickable {
                                        viewModel.setLanguage(lang.code)
                                        showLanguageDialog = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = lang.flag,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = lang.nativeName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = lang.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setLanguage(lang.code)
                                        showLanguageDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showLanguageDialog = false }) {
                            Text(strings.cancel)
                        }
                    }
                }
            }
        }
    }

    // Currency Picker Dialog
    if (showCurrencyDialog) {
        Dialog(onDismissRequest = { showCurrencyDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Select Base Currency",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableCurrencies) { curr ->
                            val isSelected = preferences.currencyCode == curr.code
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Emerald500.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable {
                                        viewModel.setCurrency(curr.code, curr.symbol)
                                        showCurrencyDialog = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${curr.name} (${curr.code})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Emerald500 else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = curr.symbol,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCurrencyDialog = false }) {
                            Text(strings.cancel)
                        }
                    }
                }
            }
        }
    }

    // PIN Setup Dialog
    if (showPinSetupDialog) {
        var pin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showPinSetupDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = strings.setPinTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it
                        },
                        label = { Text(strings.enterPin) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it
                        },
                        label = { Text(strings.confirmPin) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinError != null) {
                        Text(
                            text = pinError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = ExpenseRed,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showPinSetupDialog = false }) {
                            Text(strings.cancel)
                        }
                        Button(
                            onClick = {
                                if (pin.length != 4) {
                                    pinError = "PIN must be 4 digits"
                                } else if (pin != confirmPin) {
                                    pinError = strings.pinMismatch
                                } else {
                                    viewModel.setAppLock(true, pin)
                                    showPinSetupDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(strings.savePin, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Reset Confirm Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text(strings.resetConfirmTitle) },
            text = { Text(strings.resetConfirmMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData {
                            showResetConfirmDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text(strings.confirmReset, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
fun SettingsActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
