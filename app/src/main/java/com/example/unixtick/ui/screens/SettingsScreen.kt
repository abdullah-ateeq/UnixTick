package com.example.unixtick.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.unixtick.data.model.ConversionMode
import com.example.unixtick.data.model.DateFormatOption
import com.example.unixtick.data.model.DefaultTimezoneOption
import com.example.unixtick.data.model.ThemeMode
import com.example.unixtick.data.model.TimeFormatOption
import com.example.unixtick.data.model.TimestampUnit
import com.example.unixtick.data.model.UserSettings
import com.example.unixtick.ui.viewmodel.UnixTickViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: UnixTickViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showResetSettingsConfirm by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    fun showMessage(msg: String) {
        scope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (showResetSettingsConfirm) {
        AlertDialog(
            onDismissRequest = { showResetSettingsConfirm = false },
            title = { Text("Reset Settings") },
            text = { Text("Reset all preferences to default values?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetSettings()
                        showResetSettingsConfirm = false
                        showMessage("Settings reset to defaults")
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetSettingsConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text("Clear All History") },
            text = { Text("Delete all saved timestamp conversions?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearHistoryConfirm = false
                        showMessage("History cleared")
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance Section
            SettingsSectionHeader("APPEARANCE")
            SettingsCard {
                DropdownSettingItem(
                    title = "Theme",
                    currentValue = settings.themeMode.label,
                    options = ThemeMode.entries.map { it.label },
                    onSelect = { label ->
                        val selected = ThemeMode.entries.first { it.label == label }
                        viewModel.updateSettings(settings.copy(themeMode = selected))
                    }
                )

                HorizontalDivider()

                SwitchSettingItem(
                    title = "Dynamic Color",
                    subtitle = "Use Android 12+ wallpaper colors",
                    checked = settings.dynamicColor,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(dynamicColor = it)) }
                )

                HorizontalDivider()

                SwitchSettingItem(
                    title = "HEX-Style Monospace Formatting",
                    subtitle = "Render timestamps in technical monospace font",
                    checked = settings.hexMonospaceFormatting,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(hexMonospaceFormatting = it)) }
                )
            }

            // Defaults Section
            SettingsSectionHeader("CONVERTER DEFAULTS")
            SettingsCard {
                DropdownSettingItem(
                    title = "Default Conversion Direction",
                    currentValue = settings.defaultConversionMode.label,
                    options = ConversionMode.entries.map { it.label },
                    onSelect = { label ->
                        val mode = ConversionMode.entries.first { it.label == label }
                        viewModel.updateSettings(settings.copy(defaultConversionMode = mode))
                    }
                )

                HorizontalDivider()

                DropdownSettingItem(
                    title = "Default Timestamp Unit",
                    currentValue = settings.defaultTimestampUnit.label,
                    options = TimestampUnit.entries.map { it.label },
                    onSelect = { label ->
                        val unit = TimestampUnit.entries.first { it.label == label }
                        viewModel.updateSettings(settings.copy(defaultTimestampUnit = unit))
                    }
                )

                HorizontalDivider()

                DropdownSettingItem(
                    title = "Default Timezone",
                    currentValue = settings.defaultTimezone.label,
                    options = DefaultTimezoneOption.entries.map { it.label },
                    onSelect = { label ->
                        val tz = DefaultTimezoneOption.entries.first { it.label == label }
                        viewModel.updateSettings(settings.copy(defaultTimezone = tz))
                    }
                )

                HorizontalDivider()

                DropdownSettingItem(
                    title = "Date Format",
                    currentValue = settings.dateFormat.label,
                    options = DateFormatOption.entries.map { it.label },
                    onSelect = { label ->
                        val df = DateFormatOption.entries.first { it.label == label }
                        viewModel.updateSettings(settings.copy(dateFormat = df))
                    }
                )

                HorizontalDivider()

                DropdownSettingItem(
                    title = "Time Format",
                    currentValue = settings.timeFormat.label,
                    options = TimeFormatOption.entries.map { it.label },
                    onSelect = { label ->
                        val tf = TimeFormatOption.entries.first { it.label == label }
                        viewModel.updateSettings(settings.copy(timeFormat = tf))
                    }
                )
            }

            // Display Toggles Section
            SettingsSectionHeader("DISPLAY OPTIONS")
            SettingsCard {
                SwitchSettingItem(
                    title = "Show UTC Output",
                    subtitle = "Always display UTC date and time",
                    checked = settings.showUtc,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(showUtc = it)) }
                )

                HorizontalDivider()

                SwitchSettingItem(
                    title = "Show Device Local Time",
                    subtitle = "Display device timezone equivalent",
                    checked = settings.showLocalTime,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(showLocalTime = it)) }
                )

                HorizontalDivider()

                SwitchSettingItem(
                    title = "Show ISO 8601",
                    subtitle = "Display ISO 8601 string representations",
                    checked = settings.showIso8601,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(showIso8601 = it)) }
                )

                HorizontalDivider()

                SwitchSettingItem(
                    title = "Show Milliseconds",
                    subtitle = "Show millisecond values and copy button",
                    checked = settings.showMilliseconds,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(showMilliseconds = it)) }
                )

                HorizontalDivider()

                SwitchSettingItem(
                    title = "Show Human Date",
                    subtitle = "Display full weekday and month names",
                    checked = settings.showHumanDate,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(showHumanDate = it)) }
                )
            }

            // General & History Section
            SettingsSectionHeader("HISTORY & SYSTEM")
            SettingsCard {
                DropdownSettingItem(
                    title = "History Limit",
                    currentValue = "${settings.historyLimit} records",
                    options = listOf("50 records", "100 records"),
                    onSelect = { label ->
                        val limit = if (label.startsWith("50")) 50 else 100
                        viewModel.updateSettings(settings.copy(historyLimit = limit))
                    }
                )

                HorizontalDivider()

                SwitchSettingItem(
                    title = "Confirm Deletion",
                    subtitle = "Show confirmation dialog when deleting history items",
                    checked = settings.confirmDelete,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(confirmDelete = it)) }
                )

                HorizontalDivider()

                SwitchSettingItem(
                    title = "Haptic Feedback",
                    subtitle = "Vibrate on copy, save, and favorite actions",
                    checked = settings.hapticFeedback,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(hapticFeedback = it)) }
                )
            }

            // Danger & Reset Section
            SettingsSectionHeader("DATA MANAGEMENT")
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearHistoryConfirm = true }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Clear History", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        Text("Delete all saved conversions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    OutlinedButton(onClick = { showClearHistoryConfirm = true }) {
                        Text("Clear")
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showResetSettingsConfirm = true }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Reset Settings", style = MaterialTheme.typography.bodyLarge)
                        Text("Restore default preferences", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    OutlinedButton(onClick = { showResetSettingsConfirm = true }) {
                        Text("Reset")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SwitchSettingItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun DropdownSettingItem(
    title: String,
    currentValue: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = currentValue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
