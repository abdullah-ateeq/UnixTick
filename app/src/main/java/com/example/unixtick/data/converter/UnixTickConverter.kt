package com.example.unixtick.data.converter

import com.example.unixtick.data.model.DateFormatOption
import com.example.unixtick.data.model.TimeFormatOption
import com.example.unixtick.data.model.TimestampUnit
import com.example.unixtick.data.model.TimestampUnitDetection
import com.example.unixtick.data.model.UnixTickResult
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object UnixTickConverter {

    private const val MIN_PLAUSIBLE_YEAR = 1970
    private const val MAX_PLAUSIBLE_YEAR = 2100
    private const val MODERN_ERA_START_YEAR = 2000

    // Threshold where timestamp in seconds reaches year 2000 (~946684800L)
    private const val MODERN_SECONDS_THRESHOLD = 946_684_800L
    // Threshold where timestamp in seconds reaches year 2100 (~4102444800L)
    private const val MAX_PLAUSIBLE_SECONDS = 4_102_444_800L

    /**
     * Converts a Long timestamp to an Instant according to the provided unit.
     * Throws DateTimeException or ArithmeticException if value overflows supported range.
     */
    fun timestampToInstant(
        timestamp: Long,
        unit: TimestampUnit
    ): Instant {
        return when (unit) {
            TimestampUnit.SECONDS -> Instant.ofEpochSecond(timestamp)
            TimestampUnit.MILLISECONDS -> Instant.ofEpochMilli(timestamp)
            TimestampUnit.AUTO -> {
                when (detectTimestampUnit(timestamp)) {
                    is TimestampUnitDetection.Milliseconds -> Instant.ofEpochMilli(timestamp)
                    else -> Instant.ofEpochSecond(timestamp)
                }
            }
        }
    }

    /**
     * Safely attempts to convert a Long timestamp into an Instant.
     * Returns null if outside supported Instant range.
     */
    fun safeTimestampToInstant(
        timestamp: Long,
        unit: TimestampUnit
    ): Instant? {
        return try {
            timestampToInstant(timestamp, unit)
        } catch (_: DateTimeException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun instantToUnixSeconds(instant: Instant): Long {
        return instant.epochSecond
    }

    fun instantToUnixMilliseconds(instant: Instant): Long {
        return instant.toEpochMilli()
    }

    fun instantToUtcDateTime(instant: Instant): ZonedDateTime {
        return instant.atZone(ZoneOffset.UTC)
    }

    fun instantToLocalDateTime(instant: Instant, zoneId: ZoneId): ZonedDateTime {
        return instant.atZone(zoneId)
    }

    /**
     * Converts a LocalDate and LocalTime in the specified ZoneId into an Instant.
     */
    fun dateTimeToInstant(
        date: LocalDate,
        time: LocalTime,
        zoneId: ZoneId
    ): Instant {
        val localDateTime = LocalDateTime.of(date, time)
        val zonedDateTime = localDateTime.atZone(zoneId)
        return zonedDateTime.toInstant()
    }

    /**
     * Detects whether a given timestamp is best interpreted as Seconds, Milliseconds,
     * or is Ambiguous, OutOfRange, or Invalid.
     */
    fun detectTimestampUnit(timestamp: Long): TimestampUnitDetection {
        val secInstant = try {
            Instant.ofEpochSecond(timestamp)
        } catch (_: Exception) {
            null
        }

        val milliInstant = try {
            Instant.ofEpochMilli(timestamp)
        } catch (_: Exception) {
            null
        }

        if (secInstant == null && milliInstant == null) {
            return TimestampUnitDetection.OutOfRange
        }

        val secYear = secInstant?.atZone(ZoneOffset.UTC)?.year
        val milliYear = milliInstant?.atZone(ZoneOffset.UTC)?.year

        val secPlausible = secYear != null && secYear in MIN_PLAUSIBLE_YEAR..MAX_PLAUSIBLE_YEAR
        val milliPlausible = milliYear != null && milliYear in MIN_PLAUSIBLE_YEAR..MAX_PLAUSIBLE_YEAR

        // If milliseconds interpretation is in modern plausible range (e.g. 2000..2100)
        // and seconds interpretation is far in the future (> 2100), it's Milliseconds.
        if (milliYear != null && milliYear in MODERN_ERA_START_YEAR..MAX_PLAUSIBLE_YEAR &&
            (secYear == null || secYear > MAX_PLAUSIBLE_YEAR)
        ) {
            return TimestampUnitDetection.Milliseconds
        }

        // If seconds interpretation is in modern era (>= 2000..2100)
        // and millis interpretation is within first few months of 1970 (less than 48 days):
        // It is clearly a modern 10-digit seconds timestamp.
        if (secYear != null && secYear in MODERN_ERA_START_YEAR..MAX_PLAUSIBLE_YEAR &&
            timestamp in MODERN_SECONDS_THRESHOLD..MAX_PLAUSIBLE_SECONDS
        ) {
            return TimestampUnitDetection.Seconds
        }

        // If only one is plausible in the overall 1970..2100 range:
        if (secPlausible && !milliPlausible) {
            return TimestampUnitDetection.Seconds
        }
        if (milliPlausible && !secPlausible) {
            return TimestampUnitDetection.Milliseconds
        }

        // If both interpretations fall in 1970..2100:
        if (secPlausible && milliPlausible) {
            return TimestampUnitDetection.Ambiguous
        }

        // Near-epoch timestamps (e.g. 1960..2100) where both are valid near epoch:
        val secNearEpoch = secYear != null && secYear in 1960..MAX_PLAUSIBLE_YEAR
        val milliNearEpoch = milliYear != null && milliYear in 1960..MAX_PLAUSIBLE_YEAR
        if (secNearEpoch && milliNearEpoch) {
            return TimestampUnitDetection.Ambiguous
        }

        // Neither interpretation falls within supported detection range:
        return TimestampUnitDetection.OutOfRange
    }

    /**
     * Checks DST rules for a given LocalDate and LocalTime in the specified ZoneId.
     * Returns a warning message if local time is in a DST gap or overlap, otherwise null.
     */
    fun checkDstStatus(
        date: LocalDate,
        time: LocalTime,
        zoneId: ZoneId
    ): String? {
        val localDateTime = LocalDateTime.of(date, time)
        val rules = zoneId.rules
        val validOffsets = rules.getValidOffsets(localDateTime)
        return when {
            validOffsets.isEmpty() -> "This local time does not exist in the selected timezone (DST gap)."
            validOffsets.size > 1 -> "This local time occurs more than once in the selected timezone (DST overlap)."
            else -> null
        }
    }

    fun formatUtc(
        instant: Instant,
        dateFormat: DateFormatOption = DateFormatOption.YYYY_MM_DD,
        timeFormat: TimeFormatOption = TimeFormatOption.HOUR_24
    ): String {
        val zdt = instant.atZone(ZoneOffset.UTC)
        val dateStr = DateTimeFormatter.ofPattern(dateFormat.pattern, Locale.US).format(zdt)
        val timeStr = DateTimeFormatter.ofPattern(timeFormat.pattern, Locale.US).format(zdt)
        return "$dateStr $timeStr UTC"
    }

    fun formatLocal(
        instant: Instant,
        zoneId: ZoneId,
        dateFormat: DateFormatOption = DateFormatOption.YYYY_MM_DD,
        timeFormat: TimeFormatOption = TimeFormatOption.HOUR_24
    ): String {
        val zdt = instant.atZone(zoneId)
        val dateStr = DateTimeFormatter.ofPattern(dateFormat.pattern, Locale.US).format(zdt)
        val timeStr = DateTimeFormatter.ofPattern(timeFormat.pattern, Locale.US).format(zdt)
        val tzShort = zdt.format(DateTimeFormatter.ofPattern("z", Locale.US))
        val tzDisplay = if (tzShort.isNotBlank() && !tzShort.startsWith("GMT") && !tzShort.startsWith("UTC")) {
            tzShort
        } else {
            val offset = zdt.offset.toString()
            "UTC$offset"
        }
        return "$dateStr $timeStr $tzDisplay"
    }

    fun formatIsoUtc(instant: Instant): String {
        return DateTimeFormatter.ISO_INSTANT.format(instant)
    }

    fun formatIsoLocal(instant: Instant, zoneId: ZoneId): String {
        val zdt = instant.atZone(zoneId)
        val offset = zdt.offset
        val offsetDateTime = OffsetDateTime.of(zdt.toLocalDateTime(), offset)
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime)
    }

    fun formatHumanDate(instant: Instant, zoneId: ZoneId): String {
        val zdt = instant.atZone(zoneId)
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US)
        return zdt.format(formatter)
    }

    fun getDayOfWeek(instant: Instant, zoneId: ZoneId): String {
        val zdt = instant.atZone(zoneId)
        return zdt.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
    }

    fun getTimezoneOffsetString(instant: Instant, zoneId: ZoneId): String {
        val zdt = instant.atZone(zoneId)
        return zdt.offset.id
    }

    /**
     * Builds a comprehensive UnixTickResult from an Instant.
     */
    fun buildResult(
        instant: Instant,
        chosenUnit: TimestampUnit,
        zoneId: ZoneId,
        dateFormat: DateFormatOption = DateFormatOption.YYYY_MM_DD,
        timeFormat: TimeFormatOption = TimeFormatOption.HOUR_24,
        dstWarning: String? = null
    ): UnixTickResult {
        val unixSeconds = instant.epochSecond
        val unixMillis = instant.toEpochMilli()
        val utcZdt = instant.atZone(ZoneOffset.UTC)
        val localZdt = instant.atZone(zoneId)

        val detection = detectTimestampUnit(
            if (chosenUnit == TimestampUnit.MILLISECONDS) unixMillis else unixSeconds
        )

        val detectedUnit = when (chosenUnit) {
            TimestampUnit.SECONDS -> TimestampUnit.SECONDS
            TimestampUnit.MILLISECONDS -> TimestampUnit.MILLISECONDS
            TimestampUnit.AUTO -> when (detection) {
                is TimestampUnitDetection.Milliseconds -> TimestampUnit.MILLISECONDS
                is TimestampUnitDetection.Seconds -> TimestampUnit.SECONDS
                else -> null
            }
        }

        return UnixTickResult(
            instant = instant,
            unixSeconds = unixSeconds,
            unixMilliseconds = unixMillis,
            utcDateTime = utcZdt,
            localDateTime = localZdt,
            isoUtc = formatIsoUtc(instant),
            isoLocal = formatIsoLocal(instant, zoneId),
            detectedUnit = detectedUnit,
            detectionStatus = detection,
            dayOfWeek = getDayOfWeek(instant, zoneId),
            humanFormattedDate = formatHumanDate(instant, zoneId),
            isAmbiguousUnit = detection is TimestampUnitDetection.Ambiguous,
            dstWarningMessage = dstWarning
        )
    }
}
