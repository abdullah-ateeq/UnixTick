package com.example.unixtick.data.model

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class DateFormatOption(val pattern: String, val label: String) {
    YYYY_MM_DD("yyyy-MM-dd", "YYYY-MM-DD"),
    MM_DD_YYYY("MM/dd/yyyy", "MM/DD/YYYY"),
    DD_MM_YYYY("dd/MM/yyyy", "DD/MM/YYYY")
}

enum class TimeFormatOption(val pattern: String, val label: String) {
    HOUR_24("HH:mm:ss", "24-Hour"),
    HOUR_12("hh:mm:ss a", "12-Hour")
}

enum class DefaultTimezoneOption(val label: String) {
    UTC("UTC"),
    DEVICE_LOCAL("Device Local")
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val defaultConversionMode: ConversionMode = ConversionMode.TIMESTAMP_TO_DATE,
    val defaultTimestampUnit: TimestampUnit = TimestampUnit.AUTO,
    val defaultTimezone: DefaultTimezoneOption = DefaultTimezoneOption.UTC,
    val dateFormat: DateFormatOption = DateFormatOption.YYYY_MM_DD,
    val timeFormat: TimeFormatOption = TimeFormatOption.HOUR_24,
    val showUtc: Boolean = true,
    val showLocalTime: Boolean = true,
    val showIso8601: Boolean = true,
    val showMilliseconds: Boolean = true,
    val showHumanDate: Boolean = true,
    val confirmDelete: Boolean = true,
    val hapticFeedback: Boolean = true,
    val historyLimit: Int = 100, // 50 or 100
    val hexMonospaceFormatting: Boolean = true
)
