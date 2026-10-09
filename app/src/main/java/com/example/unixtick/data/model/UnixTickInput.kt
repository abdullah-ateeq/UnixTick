package com.example.unixtick.data.model

import java.time.LocalDate
import java.time.LocalTime

data class UnixTickInput(
    val timestampText: String = "",
    val timestampUnit: TimestampUnit = TimestampUnit.AUTO,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val zoneId: String = "UTC",
    val mode: ConversionMode = ConversionMode.TIMESTAMP_TO_DATE,
    val name: String = "",
    val notes: String = ""
)
