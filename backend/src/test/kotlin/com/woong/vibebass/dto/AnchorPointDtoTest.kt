package com.woong.vibebass.dto

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AnchorPointDtoTest {
    @Test
    fun `기존 픽셀 JSON을 읽고 새 페이지 좌표를 손실 없이 저장한다`() {
        val mapper = jacksonObjectMapper()
        val legacy = mapper.readValue<AnchorPointDto>("""{"timeSec":10,"scrollPixel":1024}""")
        assertNull(legacy.pagePosition)
        assertEquals(1024.0, legacy.scrollPixel)
        val current = legacy.copy(pagePosition = 1.25)
        assertEquals(current, mapper.readValue<AnchorPointDto>(mapper.writeValueAsString(current)))
    }
}
