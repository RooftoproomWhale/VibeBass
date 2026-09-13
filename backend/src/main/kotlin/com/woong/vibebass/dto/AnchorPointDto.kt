package com.woong.vibebass.dto

data class AnchorPointDto(
    val timeSec: Double,
    val scrollPixel: Double,
    // Optional for existing JSONB records: zero-based page index + fraction down the page.
    val pagePosition: Double? = null
)
