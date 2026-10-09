package com.example.unixtick.data.model

import java.time.Instant
import java.time.ZonedDateTime

data class UnixTickResult(
    val instant: Instant,
    val unixSeconds: Long,
    val unixMilliseconds: Long,
    val utcDateTime: ZonedDateTime,
    val localDateTime: ZonedDateTime,
    val isoUtc: String,
    val isoLocal: String,
    val detectedUnit: TimestampUnit?,
    val detectionStatus: TimestampUnitDetection = TimestampUnitDetection.Seconds,
    val dayOfWeek: String = "",
    val humanFormattedDate: String = "",
    val isAmbiguousUnit: Boolean = false,
    val dstWarningMessage: String? = null
)
