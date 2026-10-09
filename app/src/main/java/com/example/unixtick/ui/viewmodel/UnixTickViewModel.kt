package com.example.unixtick.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unixtick.data.converter.UnixTickConverter
import com.example.unixtick.data.model.ConversionMode
import com.example.unixtick.data.model.DefaultTimezoneOption
import com.example.unixtick.data.model.SavedTimestamp
import com.example.unixtick.data.model.TimestampUnit
import com.example.unixtick.data.model.TimestampUnitDetection
import com.example.unixtick.data.model.UnixTickResult
import com.example.unixtick.data.model.UserSettings
import com.example.unixtick.data.storage.UnixTickPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

data class ConverterUiState(
    val mode: ConversionMode = ConversionMode.TIMESTAMP_TO_DATE,
    val timestampText: String = "",
    val timestampUnit: TimestampUnit = TimestampUnit.AUTO,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedTime: LocalTime = LocalTime.now().withNano(0),
    val selectedZoneId: String = "UTC",
    val result: UnixTickResult? = null,
    val errorMessage: String? = null,
    val isAmbiguous: Boolean = false,
    val detectedUnitLabel: String? = null
)

data class CurrentTimeUiState(
    val instant: Instant = Instant.now(),
    val unixSeconds: Long = Instant.now().epochSecond,
    val unixMilliseconds: Long = Instant.now().toEpochMilli(),
    val utcFormatted: String = "",
    val localFormatted: String = ""
)

class UnixTickViewModel(
    private val preferences: UnixTickPreferences
) : ViewModel() {

    val settings: StateFlow<UserSettings> = preferences.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserSettings()
    )

    private val _uiState = MutableStateFlow(ConverterUiState())
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    private val _currentTimeState = MutableStateFlow(CurrentTimeUiState())
    val currentTimeState: StateFlow<CurrentTimeUiState> = _currentTimeState.asStateFlow()

    val rawHistory: StateFlow<List<SavedTimestamp>> = preferences.historyFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    val filteredHistory: StateFlow<List<SavedTimestamp>> = combine(
        rawHistory,
        _historySearchQuery
    ) { historyList, query ->
        val trimmed = query.trim()
        val list = if (trimmed.isEmpty()) {
            historyList
        } else {
            historyList.filter { item ->
                item.name.contains(trimmed, ignoreCase = true) ||
                item.notes.contains(trimmed, ignoreCase = true) ||
                item.timestampText.contains(trimmed, ignoreCase = true) ||
                item.unixSeconds.toString().contains(trimmed, ignoreCase = true) ||
                item.utcIso.contains(trimmed, ignoreCase = true) ||
                item.localIso.contains(trimmed, ignoreCase = true)
            }
        }
        // Favorites appear first, then sorted by createdAt descending
        list.sortedWith(
            compareByDescending<SavedTimestamp> { it.isFavorite }
                .thenByDescending { it.createdAt }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    val availableZoneIds: List<String> by lazy {
        val all = ZoneId.getAvailableZoneIds().sorted()
        // Ensure UTC and systemDefault are at the top
        val defaultId = ZoneId.systemDefault().id
        val priority = mutableListOf("UTC")
        if (defaultId != "UTC") priority.add(defaultId)
        val rest = all.filterNot { it in priority }
        priority + rest
    }

    init {
        refreshCurrentTime()
        // Initialize with current timestamp
        val now = Instant.now(Clock.systemUTC())
        val defaultSec = now.epochSecond.toString()
        _uiState.update { current ->
            current.copy(
                timestampText = defaultSec,
                selectedDate = LocalDate.now(),
                selectedTime = LocalTime.now().withNano(0)
            )
        }
        recalculate()
    }

    fun refreshCurrentTime() {
        val now = Instant.now(Clock.systemUTC())
        val systemZone = ZoneId.systemDefault()
        val s = settings.value
        _currentTimeState.value = CurrentTimeUiState(
            instant = now,
            unixSeconds = now.epochSecond,
            unixMilliseconds = now.toEpochMilli(),
            utcFormatted = UnixTickConverter.formatUtc(now, s.dateFormat, s.timeFormat),
            localFormatted = UnixTickConverter.formatLocal(now, systemZone, s.dateFormat, s.timeFormat)
        )
    }

    fun setConversionMode(mode: ConversionMode) {
        _uiState.update { it.copy(mode = mode) }
        recalculate()
    }

    fun onTimestampInputChanged(input: String) {
        // Clean input: integer only, allow single optional leading '-'
        val trimmed = input.trim()
        val filtered = buildString {
            for (i in trimmed.indices) {
                val c = trimmed[i]
                if (c == '-' && i == 0) {
                    append(c)
                } else if (c.isDigit()) {
                    append(c)
                }
            }
        }
        _uiState.update { it.copy(timestampText = filtered) }
        recalculate()
    }

    fun setTimestampUnit(unit: TimestampUnit) {
        _uiState.update { it.copy(timestampUnit = unit) }
        recalculate()
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        recalculate()
    }

    fun onTimeSelected(time: LocalTime) {
        _uiState.update { it.copy(selectedTime = time) }
        recalculate()
    }

    fun onZoneIdSelected(zoneId: String) {
        _uiState.update { it.copy(selectedZoneId = zoneId) }
        recalculate()
    }

    fun useCurrentTime() {
        refreshCurrentTime()
        val now = Instant.now(Clock.systemUTC())
        val currentState = _uiState.value
        if (currentState.mode == ConversionMode.TIMESTAMP_TO_DATE) {
            val text = when (currentState.timestampUnit) {
                TimestampUnit.MILLISECONDS -> now.toEpochMilli().toString()
                else -> now.epochSecond.toString()
            }
            _uiState.update { it.copy(timestampText = text) }
        } else {
            val zone = runCatching { ZoneId.of(currentState.selectedZoneId) }.getOrDefault(ZoneOffset.UTC)
            val zdt = now.atZone(zone)
            _uiState.update {
                it.copy(
                    selectedDate = zdt.toLocalDate(),
                    selectedTime = zdt.toLocalTime().withNano(0)
                )
            }
        }
        recalculate()
    }

    fun resetConverter() {
        val s = settings.value
        val defaultTz = when (s.defaultTimezone) {
            DefaultTimezoneOption.UTC -> "UTC"
            DefaultTimezoneOption.DEVICE_LOCAL -> ZoneId.systemDefault().id
        }
        val now = Instant.now(Clock.systemUTC())
        val defaultTimestamp = when (s.defaultTimestampUnit) {
            TimestampUnit.MILLISECONDS -> now.toEpochMilli().toString()
            else -> now.epochSecond.toString()
        }
        val zdt = now.atZone(runCatching { ZoneId.of(defaultTz) }.getOrDefault(ZoneOffset.UTC))

        _uiState.value = ConverterUiState(
            mode = s.defaultConversionMode,
            timestampText = defaultTimestamp,
            timestampUnit = s.defaultTimestampUnit,
            selectedDate = zdt.toLocalDate(),
            selectedTime = zdt.toLocalTime().withNano(0),
            selectedZoneId = defaultTz
        )
        refreshCurrentTime()
        recalculate()
    }

    private fun recalculate() {
        val state = _uiState.value
        val s = settings.value
        val zone = runCatching { ZoneId.of(state.selectedZoneId) }.getOrDefault(ZoneOffset.UTC)

        if (state.mode == ConversionMode.TIMESTAMP_TO_DATE) {
            val text = state.timestampText.trim()
            if (text.isEmpty() || text == "-") {
                _uiState.update {
                    it.copy(
                        result = null,
                        errorMessage = "Enter a valid integer timestamp.",
                        isAmbiguous = false,
                        detectedUnitLabel = null
                    )
                }
                return
            }

            val timestampLong = text.toLongOrNull()
            if (timestampLong == null) {
                _uiState.update {
                    it.copy(
                        result = null,
                        errorMessage = "Timestamp is outside the supported range.",
                        isAmbiguous = false,
                        detectedUnitLabel = null
                    )
                }
                return
            }

            val detection = UnixTickConverter.detectTimestampUnit(timestampLong)
            val isAmbiguous = (state.timestampUnit == TimestampUnit.AUTO && detection is TimestampUnitDetection.Ambiguous)

            val unitToUse = when (state.timestampUnit) {
                TimestampUnit.SECONDS -> TimestampUnit.SECONDS
                TimestampUnit.MILLISECONDS -> TimestampUnit.MILLISECONDS
                TimestampUnit.AUTO -> when (detection) {
                    is TimestampUnitDetection.Milliseconds -> TimestampUnit.MILLISECONDS
                    else -> TimestampUnit.SECONDS
                }
            }

            val detectedLabel = when (state.timestampUnit) {
                TimestampUnit.AUTO -> when (detection) {
                    is TimestampUnitDetection.Seconds -> "AUTO · SECONDS"
                    is TimestampUnitDetection.Milliseconds -> "AUTO · MILLISECONDS"
                    is TimestampUnitDetection.Ambiguous -> "AUTO · CHOOSE UNIT"
                    else -> null
                }
                TimestampUnit.SECONDS -> "SECONDS"
                TimestampUnit.MILLISECONDS -> "MILLISECONDS"
            }

            val instant = UnixTickConverter.safeTimestampToInstant(timestampLong, unitToUse)
            if (instant == null) {
                _uiState.update {
                    it.copy(
                        result = null,
                        errorMessage = "Timestamp is outside the supported range.",
                        isAmbiguous = false,
                        detectedUnitLabel = detectedLabel
                    )
                }
                return
            }

            val result = UnixTickConverter.buildResult(
                instant = instant,
                chosenUnit = state.timestampUnit,
                zoneId = zone,
                dateFormat = s.dateFormat,
                timeFormat = s.timeFormat
            )

            _uiState.update {
                it.copy(
                    result = result,
                    errorMessage = null,
                    isAmbiguous = isAmbiguous,
                    detectedUnitLabel = detectedLabel
                )
            }
        } else {
            // DATE_TO_TIMESTAMP mode
            val date = state.selectedDate
            val time = state.selectedTime
            val dstWarning = UnixTickConverter.checkDstStatus(date, time, zone)

            val instant = try {
                UnixTickConverter.dateTimeToInstant(date, time, zone)
            } catch (_: DateTimeException) {
                null
            }

            if (instant == null) {
                _uiState.update {
                    it.copy(
                        result = null,
                        errorMessage = "Selected date/time is outside the supported range.",
                        isAmbiguous = false,
                        detectedUnitLabel = null
                    )
                }
                return
            }

            val result = UnixTickConverter.buildResult(
                instant = instant,
                chosenUnit = TimestampUnit.SECONDS,
                zoneId = zone,
                dateFormat = s.dateFormat,
                timeFormat = s.timeFormat,
                dstWarning = dstWarning
            )

            _uiState.update {
                it.copy(
                    result = result,
                    errorMessage = null,
                    isAmbiguous = false,
                    detectedUnitLabel = null
                )
            }
        }
    }

    fun saveCurrentConversion(
        name: String,
        notes: String,
        onComplete: (UnixTickPreferences.SaveResult) -> Unit
    ) {
        val state = _uiState.value
        val result = state.result ?: return
        val finalName = name.trim().ifEmpty { "Untitled Timestamp" }.take(60)
        val finalNotes = notes.trim().take(300)

        val record = SavedTimestamp(
            id = UUID.randomUUID().toString(),
            name = finalName,
            notes = finalNotes,
            mode = state.mode,
            timestampText = if (state.mode == ConversionMode.TIMESTAMP_TO_DATE) state.timestampText else result.unixSeconds.toString(),
            timestampUnit = state.timestampUnit,
            unixSeconds = result.unixSeconds,
            unixMilliseconds = result.unixMilliseconds,
            zoneId = state.selectedZoneId,
            utcIso = result.isoUtc,
            localIso = result.isoLocal,
            createdAt = System.currentTimeMillis(),
            isFavorite = false
        )

        viewModelScope.launch {
            val saveResult = preferences.saveTimestamp(record, limit = settings.value.historyLimit)
            onComplete(saveResult)
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            preferences.toggleFavorite(id)
        }
    }

    fun duplicateSavedTimestamp(id: String) {
        viewModelScope.launch {
            preferences.duplicateTimestamp(id)
        }
    }

    fun deleteSavedTimestamp(id: String) {
        viewModelScope.launch {
            preferences.deleteTimestamp(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            preferences.clearHistory()
        }
    }

    fun openSavedTimestamp(item: SavedTimestamp) {
        val zone = runCatching { ZoneId.of(item.zoneId) }.getOrDefault(ZoneOffset.UTC)
        val instant = Instant.ofEpochSecond(item.unixSeconds)
        val zdt = instant.atZone(zone)

        _uiState.value = ConverterUiState(
            mode = item.mode,
            timestampText = item.timestampText,
            timestampUnit = item.timestampUnit,
            selectedDate = zdt.toLocalDate(),
            selectedTime = zdt.toLocalTime().withNano(0),
            selectedZoneId = item.zoneId
        )
        recalculate()
    }

    fun onSearchQueryChanged(query: String) {
        _historySearchQuery.value = query
    }

    fun updateSettings(newSettings: UserSettings) {
        viewModelScope.launch {
            preferences.updateSettings(newSettings)
            recalculate()
            refreshCurrentTime()
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            preferences.resetSettings()
            recalculate()
            refreshCurrentTime()
        }
    }
}
