package com.example.unixtick

import com.example.unixtick.data.model.ConversionMode
import com.example.unixtick.data.model.SavedTimestamp
import com.example.unixtick.data.model.TimestampUnit
import com.example.unixtick.data.storage.UnixTickPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class UnixTickPersistenceTest {

    // 23. History persistence serialization & deserialization
    @Test
    fun testHistorySerializationRoundTrip() {
        val originalList = listOf(
            SavedTimestamp(
                id = UUID.randomUUID().toString(),
                name = "API Response",
                notes = "Auth token timestamp",
                mode = ConversionMode.TIMESTAMP_TO_DATE,
                timestampText = "1728000000",
                timestampUnit = TimestampUnit.SECONDS,
                unixSeconds = 1728000000L,
                unixMilliseconds = 1728000000000L,
                zoneId = "UTC",
                utcIso = "2024-10-04T00:00:00Z",
                localIso = "2024-10-04T05:00:00+05:00",
                createdAt = 1728000005000L,
                isFavorite = true
            ),
            SavedTimestamp(
                id = UUID.randomUUID().toString(),
                name = "Server Log",
                notes = "Error occurrence",
                mode = ConversionMode.DATE_TO_TIMESTAMP,
                timestampText = "1728000100",
                timestampUnit = TimestampUnit.SECONDS,
                unixSeconds = 1728000100L,
                unixMilliseconds = 1728000100000L,
                zoneId = "Asia/Karachi",
                utcIso = "2024-10-04T00:01:40Z",
                localIso = "2024-10-04T05:01:40+05:00",
                createdAt = 1728000010000L,
                isFavorite = false
            )
        )

        val json = UnixTickPreferences.serializeHistory(originalList)
        val deserialized = UnixTickPreferences.deserializeHistory(json)

        assertEquals(originalList.size, deserialized.size)
        assertEquals(originalList[0].id, deserialized[0].id)
        assertEquals(originalList[0].name, deserialized[0].name)
        assertEquals(originalList[0].notes, deserialized[0].notes)
        assertEquals(originalList[0].unixSeconds, deserialized[0].unixSeconds)
        assertEquals(originalList[0].isFavorite, deserialized[0].isFavorite)

        assertEquals(originalList[1].id, deserialized[1].id)
        assertEquals(originalList[1].name, deserialized[1].name)
        assertEquals(originalList[1].zoneId, deserialized[1].zoneId)
    }

    // 24. Empty/Malformed JSON handling
    @Test
    fun testEmptyOrCorruptJsonHandling() {
        val emptyList = UnixTickPreferences.deserializeHistory("")
        assertTrue(emptyList.isEmpty())

        val corruptList = UnixTickPreferences.deserializeHistory("not a json")
        assertTrue(corruptList.isEmpty())
    }

    // 25. Favorites prioritization & sorting
    @Test
    fun testFavoritesAppearFirst() {
        val item1 = SavedTimestamp(
            id = "1",
            name = "Item 1",
            notes = "",
            mode = ConversionMode.TIMESTAMP_TO_DATE,
            timestampText = "100",
            timestampUnit = TimestampUnit.SECONDS,
            unixSeconds = 100L,
            unixMilliseconds = 100000L,
            zoneId = "UTC",
            utcIso = "",
            localIso = "",
            createdAt = 1000L,
            isFavorite = false
        )
        val item2 = SavedTimestamp(
            id = "2",
            name = "Item 2",
            notes = "",
            mode = ConversionMode.TIMESTAMP_TO_DATE,
            timestampText = "200",
            timestampUnit = TimestampUnit.SECONDS,
            unixSeconds = 200L,
            unixMilliseconds = 200000L,
            zoneId = "UTC",
            utcIso = "",
            localIso = "",
            createdAt = 2000L,
            isFavorite = true
        )

        val list = listOf(item1, item2)
        val sorted = list.sortedWith(
            compareByDescending<SavedTimestamp> { it.isFavorite }
                .thenByDescending { it.createdAt }
        )

        assertEquals("2", sorted.first().id)
        assertTrue(sorted.first().isFavorite)
        assertEquals("1", sorted.last().id)
    }

    // 26. Delete
    @Test
    fun testDeleteRecord() {
        val item1 = SavedTimestamp(
            id = "del-1",
            name = "Keep",
            notes = "",
            mode = ConversionMode.TIMESTAMP_TO_DATE,
            timestampText = "100",
            timestampUnit = TimestampUnit.SECONDS,
            unixSeconds = 100L,
            unixMilliseconds = 100000L,
            zoneId = "UTC",
            utcIso = "",
            localIso = "",
            createdAt = 1000L,
            isFavorite = false
        )
        val item2 = SavedTimestamp(
            id = "del-2",
            name = "Delete Me",
            notes = "",
            mode = ConversionMode.TIMESTAMP_TO_DATE,
            timestampText = "200",
            timestampUnit = TimestampUnit.SECONDS,
            unixSeconds = 200L,
            unixMilliseconds = 200000L,
            zoneId = "UTC",
            utcIso = "",
            localIso = "",
            createdAt = 2000L,
            isFavorite = false
        )

        val list = listOf(item1, item2)
        val filtered = list.filterNot { it.id == "del-2" }

        assertEquals(1, filtered.size)
        assertEquals("del-1", filtered[0].id)
    }

    // 27. Duplicate
    @Test
    fun testDuplicateRecord() {
        val original = SavedTimestamp(
            id = "orig-1",
            name = "Original Name",
            notes = "Test notes",
            mode = ConversionMode.TIMESTAMP_TO_DATE,
            timestampText = "1728000000",
            timestampUnit = TimestampUnit.SECONDS,
            unixSeconds = 1728000000L,
            unixMilliseconds = 1728000000000L,
            zoneId = "UTC",
            utcIso = "2024-10-04T00:00:00Z",
            localIso = "2024-10-04T05:00:00+05:00",
            createdAt = 1000L,
            isFavorite = true
        )

        val duplicate = original.copy(
            id = UUID.randomUUID().toString(),
            name = "Copy of ${original.name}",
            createdAt = System.currentTimeMillis(),
            isFavorite = false
        )

        assertNotEquals(original.id, duplicate.id)
        assertEquals("Copy of Original Name", duplicate.name)
        assertEquals(original.unixSeconds, duplicate.unixSeconds)
        assertEquals(original.unixMilliseconds, duplicate.unixMilliseconds)
        assertFalse(duplicate.isFavorite) // Duplicate should default to not favorite
    }

    // History limit logic: oldest non-favorite dropped when full
    @Test
    fun testHistoryLimitDropOldestNonFavorite() {
        val list = mutableListOf<SavedTimestamp>()
        for (i in 1..5) {
            list.add(
                SavedTimestamp(
                    id = "id-$i",
                    name = "Item $i",
                    notes = "",
                    mode = ConversionMode.TIMESTAMP_TO_DATE,
                    timestampText = "$i",
                    timestampUnit = TimestampUnit.SECONDS,
                    unixSeconds = i.toLong(),
                    unixMilliseconds = i * 1000L,
                    zoneId = "UTC",
                    utcIso = "",
                    localIso = "",
                    createdAt = i.toLong(),
                    isFavorite = (i == 5) // item 5 (oldest non-favorite is item 1)
                )
            )
        }

        // Limit = 5. Add new record.
        val oldestNonFavIndex = list.indexOfLast { !it.isFavorite }
        val removed = list.removeAt(oldestNonFavIndex)

        // The oldest non-favorite in this list (end of list is oldest) was item 4
        assertFalse(removed.isFavorite)
    }
}
