package com.example.unixtick.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.unixtick.data.model.ConversionMode
import com.example.unixtick.data.model.DateFormatOption
import com.example.unixtick.data.model.DefaultTimezoneOption
import com.example.unixtick.data.model.SavedTimestamp
import com.example.unixtick.data.model.ThemeMode
import com.example.unixtick.data.model.TimeFormatOption
import com.example.unixtick.data.model.TimestampUnit
import com.example.unixtick.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "unixtick_preferences")

class UnixTickPreferences(private val context: Context) {

    private object Keys {
        val SAVED_HISTORY = stringPreferencesKey("saved_history")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_CONVERSION_MODE = stringPreferencesKey("default_conversion_mode")
        val DEFAULT_TIMESTAMP_UNIT = stringPreferencesKey("default_timestamp_unit")
        val DEFAULT_TIMEZONE = stringPreferencesKey("default_timezone")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val TIME_FORMAT = stringPreferencesKey("time_format")
        val SHOW_UTC = booleanPreferencesKey("show_utc")
        val SHOW_LOCAL_TIME = booleanPreferencesKey("show_local_time")
        val SHOW_ISO8601 = booleanPreferencesKey("show_iso8601")
        val SHOW_MILLISECONDS = booleanPreferencesKey("show_milliseconds")
        val SHOW_HUMAN_DATE = booleanPreferencesKey("show_human_date")
        val CONFIRM_DELETE = booleanPreferencesKey("confirm_delete")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val HISTORY_LIMIT = intPreferencesKey("history_limit")
        val HEX_MONOSPACE = booleanPreferencesKey("hex_monospace")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val themeModeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(themeModeStr) }.getOrDefault(ThemeMode.SYSTEM)

        val conversionModeStr = prefs[Keys.DEFAULT_CONVERSION_MODE] ?: ConversionMode.TIMESTAMP_TO_DATE.name
        val conversionMode = runCatching { ConversionMode.valueOf(conversionModeStr) }.getOrDefault(ConversionMode.TIMESTAMP_TO_DATE)

        val unitStr = prefs[Keys.DEFAULT_TIMESTAMP_UNIT] ?: TimestampUnit.AUTO.name
        val timestampUnit = runCatching { TimestampUnit.valueOf(unitStr) }.getOrDefault(TimestampUnit.AUTO)

        val timezoneStr = prefs[Keys.DEFAULT_TIMEZONE] ?: DefaultTimezoneOption.UTC.name
        val defaultTz = runCatching { DefaultTimezoneOption.valueOf(timezoneStr) }.getOrDefault(DefaultTimezoneOption.UTC)

        val dateFormatStr = prefs[Keys.DATE_FORMAT] ?: DateFormatOption.YYYY_MM_DD.name
        val dateFormat = runCatching { DateFormatOption.valueOf(dateFormatStr) }.getOrDefault(DateFormatOption.YYYY_MM_DD)

        val timeFormatStr = prefs[Keys.TIME_FORMAT] ?: TimeFormatOption.HOUR_24.name
        val timeFormat = runCatching { TimeFormatOption.valueOf(timeFormatStr) }.getOrDefault(TimeFormatOption.HOUR_24)

        UserSettings(
            themeMode = themeMode,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            defaultConversionMode = conversionMode,
            defaultTimestampUnit = timestampUnit,
            defaultTimezone = defaultTz,
            dateFormat = dateFormat,
            timeFormat = timeFormat,
            showUtc = prefs[Keys.SHOW_UTC] ?: true,
            showLocalTime = prefs[Keys.SHOW_LOCAL_TIME] ?: true,
            showIso8601 = prefs[Keys.SHOW_ISO8601] ?: true,
            showMilliseconds = prefs[Keys.SHOW_MILLISECONDS] ?: true,
            showHumanDate = prefs[Keys.SHOW_HUMAN_DATE] ?: true,
            confirmDelete = prefs[Keys.CONFIRM_DELETE] ?: true,
            hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            historyLimit = prefs[Keys.HISTORY_LIMIT] ?: 100,
            hexMonospaceFormatting = prefs[Keys.HEX_MONOSPACE] ?: true
        )
    }

    val historyFlow: Flow<List<SavedTimestamp>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[Keys.SAVED_HISTORY] ?: "[]"
        deserializeHistory(jsonStr)
    }

    suspend fun updateSettings(settings: UserSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = settings.themeMode.name
            prefs[Keys.DYNAMIC_COLOR] = settings.dynamicColor
            prefs[Keys.DEFAULT_CONVERSION_MODE] = settings.defaultConversionMode.name
            prefs[Keys.DEFAULT_TIMESTAMP_UNIT] = settings.defaultTimestampUnit.name
            prefs[Keys.DEFAULT_TIMEZONE] = settings.defaultTimezone.name
            prefs[Keys.DATE_FORMAT] = settings.dateFormat.name
            prefs[Keys.TIME_FORMAT] = settings.timeFormat.name
            prefs[Keys.SHOW_UTC] = settings.showUtc
            prefs[Keys.SHOW_LOCAL_TIME] = settings.showLocalTime
            prefs[Keys.SHOW_ISO8601] = settings.showIso8601
            prefs[Keys.SHOW_MILLISECONDS] = settings.showMilliseconds
            prefs[Keys.SHOW_HUMAN_DATE] = settings.showHumanDate
            prefs[Keys.CONFIRM_DELETE] = settings.confirmDelete
            prefs[Keys.HAPTIC_FEEDBACK] = settings.hapticFeedback
            prefs[Keys.HISTORY_LIMIT] = settings.historyLimit
            prefs[Keys.HEX_MONOSPACE] = settings.hexMonospaceFormatting
        }
    }

    suspend fun resetSettings() {
        updateSettings(UserSettings())
    }

    sealed interface SaveResult {
        data object Success : SaveResult
        data class LimitReached(val message: String) : SaveResult
    }

    /**
     * Saves a timestamp record respecting history limit and favorite preservation rules.
     */
    suspend fun saveTimestamp(record: SavedTimestamp, limit: Int = 100): SaveResult {
        var result: SaveResult = SaveResult.Success
        context.dataStore.edit { prefs ->
            val jsonStr = prefs[Keys.SAVED_HISTORY] ?: "[]"
            val currentList = deserializeHistory(jsonStr).toMutableList()

            if (currentList.size >= limit) {
                // Find the oldest non-favorite item
                val oldestNonFavIndex = currentList.indexOfLast { !it.isFavorite }
                if (oldestNonFavIndex != -1) {
                    currentList.removeAt(oldestNonFavIndex)
                } else {
                    result = SaveResult.LimitReached("History limit reached. Remove an item before saving.")
                    return@edit
                }
            }

            // Insert new item at beginning
            currentList.add(0, record)
            prefs[Keys.SAVED_HISTORY] = serializeHistory(currentList)
        }
        return result
    }

    suspend fun toggleFavorite(id: String) {
        context.dataStore.edit { prefs ->
            val jsonStr = prefs[Keys.SAVED_HISTORY] ?: "[]"
            val currentList = deserializeHistory(jsonStr).map {
                if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
            }
            prefs[Keys.SAVED_HISTORY] = serializeHistory(currentList)
        }
    }

    suspend fun deleteTimestamp(id: String) {
        context.dataStore.edit { prefs ->
            val jsonStr = prefs[Keys.SAVED_HISTORY] ?: "[]"
            val currentList = deserializeHistory(jsonStr).filterNot { it.id == id }
            prefs[Keys.SAVED_HISTORY] = serializeHistory(currentList)
        }
    }

    suspend fun duplicateTimestamp(id: String) {
        context.dataStore.edit { prefs ->
            val jsonStr = prefs[Keys.SAVED_HISTORY] ?: "[]"
            val currentList = deserializeHistory(jsonStr).toMutableList()
            val target = currentList.find { it.id == id } ?: return@edit
            val duplicate = target.copy(
                id = java.util.UUID.randomUUID().toString(),
                name = "Copy of ${target.name}",
                createdAt = System.currentTimeMillis(),
                isFavorite = false
            )
            currentList.add(0, duplicate)
            prefs[Keys.SAVED_HISTORY] = serializeHistory(currentList)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { prefs ->
            prefs[Keys.SAVED_HISTORY] = "[]"
        }
    }

    companion object {
        fun serializeHistory(list: List<SavedTimestamp>): String {
            val array = JSONArray()
            list.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("notes", item.notes)
                    put("mode", item.mode.name)
                    put("timestampText", item.timestampText)
                    put("timestampUnit", item.timestampUnit.name)
                    put("unixSeconds", item.unixSeconds)
                    put("unixMilliseconds", item.unixMilliseconds)
                    put("zoneId", item.zoneId)
                    put("utcIso", item.utcIso)
                    put("localIso", item.localIso)
                    put("createdAt", item.createdAt)
                    put("isFavorite", item.isFavorite)
                }
                array.put(obj)
            }
            return array.toString()
        }

        fun deserializeHistory(jsonStr: String): List<SavedTimestamp> {
            val list = mutableListOf<SavedTimestamp>()
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val mode = runCatching {
                        ConversionMode.valueOf(obj.getString("mode"))
                    }.getOrDefault(ConversionMode.TIMESTAMP_TO_DATE)

                    val unit = runCatching {
                        TimestampUnit.valueOf(obj.getString("timestampUnit"))
                    }.getOrDefault(TimestampUnit.AUTO)

                    list.add(
                        SavedTimestamp(
                            id = obj.getString("id"),
                            name = obj.optString("name", "Untitled Timestamp"),
                            notes = obj.optString("notes", ""),
                            mode = mode,
                            timestampText = obj.optString("timestampText", "0"),
                            timestampUnit = unit,
                            unixSeconds = obj.optLong("unixSeconds", 0L),
                            unixMilliseconds = obj.optLong("unixMilliseconds", 0L),
                            zoneId = obj.optString("zoneId", "UTC"),
                            utcIso = obj.optString("utcIso", ""),
                            localIso = obj.optString("localIso", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            isFavorite = obj.optBoolean("isFavorite", false)
                        )
                    )
                }
            } catch (_: Exception) {
                // Return empty list on parse error
            }
            return list
        }
    }
}
