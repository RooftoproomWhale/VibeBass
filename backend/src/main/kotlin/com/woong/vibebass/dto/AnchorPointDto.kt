package com.woong.vibebass.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.constraints.AssertTrue

data class AnchorPointDto(
    val timeSec: Double,
    val scrollPixel: Double,
    // Optional for existing JSONB records: zero-based page index + fraction down the page.
    val pagePosition: Double? = null
) {
    @get:JsonIgnore
    @get:AssertTrue(message = "앵커 좌표는 Float 범위의 0 이상 유한한 숫자여야 합니다.")
    val coordinatesValid: Boolean
        get() = listOf(timeSec, scrollPixel, pagePosition ?: 0.0).all {
            it.isFinite() && it >= 0.0 && it <= Float.MAX_VALUE.toDouble()
        }
}
