package com.example.unixtick.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unixtick.data.model.ConversionMode
import com.example.unixtick.data.model.TimestampUnit
import com.example.unixtick.data.storage.UnixTickPreferences
import com.example.unixtick.ui.components.CurrentTimeCard
import com.example.unixtick.ui.components.DatePickerModal
import com.example.unixtick.ui.components.FormulaCard
import com.example.unixtick.ui.components.ResultCard
import com.example.unixtick.ui.components.SaveTimestampDialog
import com.example.unixtick.ui.components.TimePickerModal
import com.example.unixtick.ui.components.TimezoneSelectorDialog
import com.example.unixtick.ui.theme.AmberWarning
import com.example.unixtick.ui.viewmodel.UnixTickViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    viewModel: UnixTickViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTimeState by viewModel.currentTimeState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showSaveDialog by remember { mutableStateOf(false) }
    var showTimezoneDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    if (showSaveDialog) {
        SaveTimestampDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name, notes ->
                viewModel.saveCurrentConversion(name, notes) { saveResult ->
                    when (saveResult) {
                        is UnixTickPreferences.SaveResult.Success -> {
                            if (settings.hapticFeedback) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            showMessage("Saved to History")
                        }
                        is UnixTickPreferences.SaveResult.LimitReached -> {
                            showMessage(saveResult.message)
                        }
                    }
                }
            }
        )
    }

    if (showTimezoneDialog) {
        TimezoneSelectorDialog(
            currentTimezone = uiState.selectedZoneId,
            availableZones = viewModel.availableZoneIds,
            onSelectZone = { viewModel.onZoneIdSelected(it) },
            onDismiss = { showTimezoneDialog = false }
        )
    }

    if (showDatePicker) {
        DatePickerModal(
            selectedDate = uiState.selectedDate,
            onDateSelected = { viewModel.onDateSelected(it) },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        TimePickerModal(
            selectedTime = uiState.selectedTime,
            onTimeSelected = { viewModel.onTimeSelected(it) },
            onDismiss = { showTimePicker = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "UnixTick",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "OFFLINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (settings.hapticFeedback) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            viewModel.resetConverter()
                            showMessage("Converter reset")
                        },
                        modifier = Modifier.semantics { contentDescription = "Reset converter" }
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset")
                    }

                    if (uiState.result != null) {
                        IconButton(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier.semantics { contentDescription = "Save conversion" }
                        ) {
                            Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = "Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isTabletOrLandscape = maxWidth >= 600.dp

            if (isTabletOrLandscape) {
                // Adaptive Two-Column Layout for Tablets / Landscape
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ModeSelectorSection(
                            mode = uiState.mode,
                            onModeChanged = { viewModel.setConversionMode(it) }
                        )

                        if (uiState.mode == ConversionMode.TIMESTAMP_TO_DATE) {
                            TimestampInputSection(
                                timestampText = uiState.timestampText,
                                unit = uiState.timestampUnit,
                                detectedLabel = uiState.detectedUnitLabel,
                                isAmbiguous = uiState.isAmbiguous,
                                errorMessage = uiState.errorMessage,
                                onTimestampChanged = { viewModel.onTimestampInputChanged(it) },
                                onUnitChanged = { viewModel.setTimestampUnit(it) }
                            )
                        } else {
                            DateTimeInputSection(
                                uiState = uiState,
                                onOpenDatePicker = { showDatePicker = true },
                                onOpenTimePicker = { showTimePicker = true },
                                onOpenTimezone = { showTimezoneDialog = true }
                            )
                        }

                        CurrentTimeCard(
                            currentTimeState = currentTimeState,
                            settings = settings,
                            haptic = haptic,
                            onRefresh = { viewModel.refreshCurrentTime() },
                            onUseCurrentTime = { viewModel.useCurrentTime() }
                        )

                        FormulaCard()
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (uiState.result != null) {
                            ResultCard(
                                result = uiState.result!!,
                                mode = uiState.mode,
                                settings = settings,
                                haptic = haptic,
                                onShowSnackbar = { showMessage(it) }
                            )
                        } else if (uiState.errorMessage != null) {
                            ErrorDisplayCard(message = uiState.errorMessage!!)
                        }
                    }
                }
            } else {
                // Single Column Layout for Phones
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ModeSelectorSection(
                        mode = uiState.mode,
                        onModeChanged = { viewModel.setConversionMode(it) }
                    )

                    if (uiState.mode == ConversionMode.TIMESTAMP_TO_DATE) {
                        TimestampInputSection(
                            timestampText = uiState.timestampText,
                            unit = uiState.timestampUnit,
                            detectedLabel = uiState.detectedUnitLabel,
                            isAmbiguous = uiState.isAmbiguous,
                            errorMessage = uiState.errorMessage,
                            onTimestampChanged = { viewModel.onTimestampInputChanged(it) },
                            onUnitChanged = { viewModel.setTimestampUnit(it) }
                        )
                    } else {
                        DateTimeInputSection(
                            uiState = uiState,
                            onOpenDatePicker = { showDatePicker = true },
                            onOpenTimePicker = { showTimePicker = true },
                            onOpenTimezone = { showTimezoneDialog = true }
                        )
                    }

                    if (uiState.result != null) {
                        ResultCard(
                            result = uiState.result!!,
                            mode = uiState.mode,
                            settings = settings,
                            haptic = haptic,
                            onShowSnackbar = { showMessage(it) }
                        )
                    } else if (uiState.errorMessage != null) {
                        ErrorDisplayCard(message = uiState.errorMessage!!)
                    }

                    CurrentTimeCard(
                        currentTimeState = currentTimeState,
                        settings = settings,
                        haptic = haptic,
                        onRefresh = { viewModel.refreshCurrentTime() },
                        onUseCurrentTime = { viewModel.useCurrentTime() }
                    )

                    FormulaCard()

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelectorSection(
    mode: ConversionMode,
    onModeChanged: (ConversionMode) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ConversionMode.entries.forEachIndexed { index, item ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ConversionMode.entries.size),
                onClick = { onModeChanged(item) },
                selected = mode == item
            ) {
                Text(text = item.label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimestampInputSection(
    timestampText: String,
    unit: TimestampUnit,
    detectedLabel: String?,
    isAmbiguous: Boolean,
    errorMessage: String?,
    onTimestampChanged: (String) -> Unit,
    onUnitChanged: (TimestampUnit) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TIMESTAMP",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (detectedLabel != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = detectedLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = timestampText,
                onValueChange = onTimestampChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Unix timestamp input" },
                placeholder = { Text("e.g. 1728000000 or -86400") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    if (timestampText.isNotEmpty()) {
                        IconButton(onClick = { onTimestampChanged("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear timestamp")
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                isError = errorMessage != null,
                shape = RoundedCornerShape(10.dp)
            )

            // Ambiguity Notification Banner
            AnimatedVisibility(visible = isAmbiguous) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "Timestamp unit may be ambiguous.",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Please specify whether this value represents seconds or milliseconds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Unit Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimestampUnit.entries.forEach { u ->
                    FilterChip(
                        selected = unit == u,
                        onClick = { onUnitChanged(u) },
                        label = { Text(u.label) },
                        modifier = Modifier.semantics {
                            contentDescription = when (u) {
                                TimestampUnit.AUTO -> "Timestamp unit auto"
                                TimestampUnit.SECONDS -> "Timestamp unit seconds"
                                TimestampUnit.MILLISECONDS -> "Timestamp unit milliseconds"
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DateTimeInputSection(
    uiState: com.example.unixtick.ui.viewmodel.ConverterUiState,
    onOpenDatePicker: () -> Unit,
    onOpenTimePicker: () -> Unit,
    onOpenTimezone: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "DATE & TIME INPUT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Date selector card
            OutlinedCard(
                onClick = onOpenDatePicker,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("DATE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = uiState.selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Time selector card
            OutlinedCard(
                onClick = onOpenTimePicker,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TIME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = uiState.selectedTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = "Pick Time", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Timezone selector card
            OutlinedCard(
                onClick = onOpenTimezone,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TIMEZONE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = uiState.selectedZoneId,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(imageVector = Icons.Default.Public, contentDescription = "Select Timezone", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ErrorDisplayCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
