package com.example.unixtick

import com.example.unixtick.data.converter.UnixTickConverter
import com.example.unixtick.data.model.DateFormatOption
import com.example.unixtick.data.model.TimeFormatOption
import com.example.unixtick.data.model.TimestampUnit
import com.example.unixtick.data.model.TimestampUnitDetection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class UnixTickConverterTest {

    // 1. Unix seconds → Instant
    @Test
    fun testUnixSecondsToInstant() {
        val instant = UnixTickConverter.timestampToInstant(1728000000L, TimestampUnit.SECONDS)
        assertEquals(1728000000L, instant.epochSecond)
    }

    // 2. Unix milliseconds → Instant
    @Test
    fun testUnixMillisecondsToInstant() {
        val instant = UnixTickConverter.timestampToInstant(1728000000000L, TimestampUnit.MILLISECONDS)
        assertEquals(1728000000L, instant.epochSecond)
        assertEquals(1728000000000L, instant.toEpochMilli())
    }

    // 3. Instant → seconds
    @Test
    fun testInstantToSeconds() {
        val instant = Instant.ofEpochSecond(1728000000L)
        val seconds = UnixTickConverter.instantToUnixSeconds(instant)
        assertEquals(1728000000L, seconds)
    }

    // 4. Instant → milliseconds
    @Test
    fun testInstantToMilliseconds() {
        val instant = Instant.ofEpochMilli(1728000000500L)
        val millis = UnixTickConverter.instantToUnixMilliseconds(instant)
        assertEquals(1728000000500L, millis)
    }

    // 5. Date/time → seconds
    @Test
    fun testDateTimeToSeconds() {
        val date = LocalDate.of(1970, 1, 1)
        val time = LocalTime.of(0, 0, 0)
        val zone = ZoneOffset.UTC
        val instant = UnixTickConverter.dateTimeToInstant(date, time, zone)
        assertEquals(0L, instant.epochSecond)
    }

    // 6. Date/time → milliseconds
    @Test
    fun testDateTimeToMilliseconds() {
        val date = LocalDate.of(1970, 1, 1)
        val time = LocalTime.of(0, 0, 1, 500_000_000) // 1.5 seconds
        val zone = ZoneOffset.UTC
        val instant = UnixTickConverter.dateTimeToInstant(date, time, zone)
        assertEquals(1L, instant.epochSecond)
        assertEquals(1500L, instant.toEpochMilli())
    }

    // 7. Epoch zero
    @Test
    fun testEpochZero() {
        val instant = UnixTickConverter.timestampToInstant(0L, TimestampUnit.SECONDS)
        assertEquals(0L, instant.epochSecond)
        val formatted = UnixTickConverter.formatUtc(instant)
        assertEquals("1970-01-01 00:00:00 UTC", formatted)
    }

    // 8. Positive timestamp
    @Test
    fun testPositiveTimestamp() {
        val instant = UnixTickConverter.timestampToInstant(1728000000L, TimestampUnit.SECONDS)
        assertEquals(1728000000L, instant.epochSecond)
        val utcZdt = instant.atZone(ZoneOffset.UTC)
        assertEquals(2024, utcZdt.year)
        assertEquals(10, utcZdt.monthValue)
        assertEquals(4, utcZdt.dayOfMonth)
        assertEquals(0, utcZdt.hour)
        assertEquals(0, utcZdt.minute)
        assertEquals(0, utcZdt.second)
        assertEquals("2024-10-04 00:00:00 UTC", UnixTickConverter.formatUtc(instant))
    }

    // 9. Negative timestamp
    @Test
    fun testNegativeTimestampOneDayBeforeEpoch() {
        val instant = UnixTickConverter.timestampToInstant(-86400L, TimestampUnit.SECONDS)
        assertEquals(-86400L, instant.epochSecond)
        val formatted = UnixTickConverter.formatUtc(instant)
        assertEquals("1969-12-31 00:00:00 UTC", formatted)
    }

    @Test
    fun testNegativeTimestampMinusOneSecond() {
        val instant = UnixTickConverter.timestampToInstant(-1L, TimestampUnit.SECONDS)
        assertEquals(-1L, instant.epochSecond)
        val formatted = UnixTickConverter.formatUtc(instant)
        assertEquals("1969-12-31 23:59:59 UTC", formatted)
    }

    @Test
    fun testNegativeTimestampMinusThousandMillis() {
        val instant = UnixTickConverter.timestampToInstant(-1000L, TimestampUnit.MILLISECONDS)
        assertEquals(-1L, instant.epochSecond)
        assertEquals(-1000L, instant.toEpochMilli())
        val formatted = UnixTickConverter.formatUtc(instant)
        assertEquals("1969-12-31 23:59:59 UTC", formatted)
    }

    // 10. Millisecond precision
    @Test
    fun testMillisecondPrecision() {
        val instant = Instant.ofEpochMilli(1500L)
        assertEquals(1L, instant.epochSecond)
        assertEquals(1500L, instant.toEpochMilli())
        assertEquals(500_000_000, instant.nano)
    }

    // 11. UTC formatting
    @Test
    fun testUtcFormattingPatterns() {
        val instant = UnixTickConverter.timestampToInstant(1728000000L, TimestampUnit.SECONDS)
        assertEquals("2024-10-04 00:00:00 UTC", UnixTickConverter.formatUtc(instant, DateFormatOption.YYYY_MM_DD, TimeFormatOption.HOUR_24))
        assertEquals("10/04/2024 00:00:00 UTC", UnixTickConverter.formatUtc(instant, DateFormatOption.MM_DD_YYYY, TimeFormatOption.HOUR_24))
        assertEquals("04/10/2024 00:00:00 UTC", UnixTickConverter.formatUtc(instant, DateFormatOption.DD_MM_YYYY, TimeFormatOption.HOUR_24))
    }

    // 12. Local timezone formatting
    @Test
    fun testTimezoneFormatting() {
        val instant = UnixTickConverter.timestampToInstant(1728000000L, TimestampUnit.SECONDS)

        // Karachi is UTC+5 (no DST in 2024)
        val karachiZone = ZoneId.of("Asia/Karachi")
        val karachiFormatted = UnixTickConverter.formatLocal(instant, karachiZone)
        assertTrue(karachiFormatted.startsWith("2024-10-04 05:00:00"))

        // London in October is BST (UTC+1)
        val londonZone = ZoneId.of("Europe/London")
        val londonFormatted = UnixTickConverter.formatLocal(instant, londonZone)
        assertTrue(londonFormatted.startsWith("2024-10-04 01:00:00"))

        // New York in October is EDT (UTC-4)
        val nyZone = ZoneId.of("America/New_York")
        val nyFormatted = UnixTickConverter.formatLocal(instant, nyZone)
        assertTrue(nyFormatted.startsWith("2024-10-03 20:00:00"))
    }

    // 13. ISO 8601 formatting
    @Test
    fun testIso8601Formatting() {
        val instant = UnixTickConverter.timestampToInstant(1728000000L, TimestampUnit.SECONDS)
        assertEquals("2024-10-04T00:00:00Z", UnixTickConverter.formatIsoUtc(instant))

        val karachiZone = ZoneId.of("Asia/Karachi")
        val isoLocal = UnixTickConverter.formatIsoLocal(instant, karachiZone)
        assertEquals("2024-10-04T05:00:00+05:00", isoLocal)
    }

    // 14. Seconds detection
    @Test
    fun testSecondsDetection() {
        val detection = UnixTickConverter.detectTimestampUnit(1728000000L) // 10-digit modern
        assertEquals(TimestampUnitDetection.Seconds, detection)
    }

    // 15. Milliseconds detection
    @Test
    fun testMillisecondsDetection() {
        val detection = UnixTickConverter.detectTimestampUnit(1728000000000L) // 13-digit modern
        assertEquals(TimestampUnitDetection.Milliseconds, detection)
    }

    // 16. Ambiguous detection
    @Test
    fun testAmbiguousDetection() {
        // Small values near epoch where both seconds and millis are early 1970
        val detection1 = UnixTickConverter.detectTimestampUnit(1000L)
        assertEquals(TimestampUnitDetection.Ambiguous, detection1)

        val detection2 = UnixTickConverter.detectTimestampUnit(-86400L)
        assertEquals(TimestampUnitDetection.Ambiguous, detection2)
    }

    // 17. Invalid / Safe timestamp parsing
    @Test
    fun testSafeTimestampConversion() {
        val safeInstant = UnixTickConverter.safeTimestampToInstant(1728000000L, TimestampUnit.SECONDS)
        assertNotNull(safeInstant)

        val outOfRange = UnixTickConverter.safeTimestampToInstant(Long.MAX_VALUE, TimestampUnit.SECONDS)
        assertNull(outOfRange)
    }

    // 18. Out of range detection
    @Test
    fun testOutOfRangeDetection() {
        val detection = UnixTickConverter.detectTimestampUnit(Long.MAX_VALUE)
        assertEquals(TimestampUnitDetection.OutOfRange, detection)
    }

    // 19. DST timezone handling
    @Test
    fun testDstTimezoneHandling() {
        val nyZone = ZoneId.of("America/New_York")
        // On 2024-03-10, US clocks jumped from 01:59:59 to 03:00:00 (02:00 to 02:59 was a GAP)
        val gapDate = LocalDate.of(2024, 3, 10)
        val gapTime = LocalTime.of(2, 30, 0)
        val warningGap = UnixTickConverter.checkDstStatus(gapDate, gapTime, nyZone)
        assertNotNull(warningGap)
        assertTrue(warningGap!!.contains("does not exist"))

        // Standard unambiguous time
        val normalTime = LocalTime.of(12, 0, 0)
        val warningNormal = UnixTickConverter.checkDstStatus(gapDate, normalTime, nyZone)
        assertNull(warningNormal)

        // Fall-back overlap: on 2024-11-03, 01:30 occurs twice
        val overlapDate = LocalDate.of(2024, 11, 3)
        val overlapTime = LocalTime.of(1, 30, 0)
        val warningOverlap = UnixTickConverter.checkDstStatus(overlapDate, overlapTime, nyZone)
        assertNotNull(warningOverlap)
        assertTrue(warningOverlap!!.contains("more than once"))
    }

    // 20. Date picker conversion
    @Test
    fun testDatePickerConversion() {
        val date = LocalDate.of(2024, 10, 4)
        val time = LocalTime.of(0, 0, 0)
        val instant = UnixTickConverter.dateTimeToInstant(date, time, ZoneOffset.UTC)
        assertEquals(1728000000L, instant.epochSecond)
    }

    // 21. Time picker conversion
    @Test
    fun testTimePickerConversion() {
        val date = LocalDate.of(2024, 10, 4)
        val time = LocalTime.of(5, 30, 15)
        val instant = UnixTickConverter.dateTimeToInstant(date, time, ZoneOffset.UTC)
        val zdt = instant.atZone(ZoneOffset.UTC)
        assertEquals(5, zdt.hour)
        assertEquals(30, zdt.minute)
        assertEquals(15, zdt.second)
    }

    // 22. Current timestamp round-trip
    @Test
    fun testCurrentTimestampRoundTrip() {
        val now = Instant.now()
        val sec = UnixTickConverter.instantToUnixSeconds(now)
        val reconstructed = Instant.ofEpochSecond(sec)
        assertEquals(sec, reconstructed.epochSecond)

        val milli = UnixTickConverter.instantToUnixMilliseconds(now)
        val reconstructedMilli = Instant.ofEpochMilli(milli)
        assertEquals(milli, reconstructedMilli.toEpochMilli())
    }

    // Round-trip requirement: Timestamp → Instant → Timestamp
    @Test
    fun testTimestampRoundTripPreservation() {
        val originalSec = 1728000000L
        val instantSec = UnixTickConverter.timestampToInstant(originalSec, TimestampUnit.SECONDS)
        val recoveredSec = UnixTickConverter.instantToUnixSeconds(instantSec)
        assertEquals(originalSec, recoveredSec)

        val originalMilli = 1728000000123L
        val instantMilli = UnixTickConverter.timestampToInstant(originalMilli, TimestampUnit.MILLISECONDS)
        val recoveredMilli = UnixTickConverter.instantToUnixMilliseconds(instantMilli)
        assertEquals(originalMilli, recoveredMilli)
    }

    // Round-trip requirement: Date/Time → Instant → Date/Time
    @Test
    fun testDateTimeRoundTripPreservation() {
        val originalDate = LocalDate.of(2025, 6, 15)
        val originalTime = LocalTime.of(14, 45, 30)
        val zone = ZoneId.of("Europe/London")

        val instant = UnixTickConverter.dateTimeToInstant(originalDate, originalTime, zone)
        val zdt = instant.atZone(zone)

        assertEquals(originalDate, zdt.toLocalDate())
        assertEquals(originalTime, zdt.toLocalTime())
    }
}
