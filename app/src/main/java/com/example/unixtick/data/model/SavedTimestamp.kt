package com.example.unixtick.data.model

data class SavedTimestamp(
    val id: String,
    val name: String,
    val notes: String,
    val mode: ConversionMode,
    val timestampText: String,
    val timestampUnit: TimestampUnit,
    val unixSeconds: Long,
    val unixMilliseconds: Long,
    val zoneId: String,
    val utcIso: String,
    val localIso: String,
    val createdAt: Long,
    val isFavorite: Boolean
)
