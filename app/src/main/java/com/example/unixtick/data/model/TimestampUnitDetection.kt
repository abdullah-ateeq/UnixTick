package com.example.unixtick.data.model

sealed interface TimestampUnitDetection {
    data object Seconds : TimestampUnitDetection
    data object Milliseconds : TimestampUnitDetection
    data object Ambiguous : TimestampUnitDetection
    data object OutOfRange : TimestampUnitDetection
    data object Invalid : TimestampUnitDetection
}
