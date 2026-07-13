package com.woong.vibebass.sync

import kotlin.test.Test
import kotlin.test.assertEquals

class SyncCalculatorTest {

    @Test
    fun testCalculateScrollPixel_EmptyAnchors() {
        val result = SyncCalculator.calculateScrollPixel(10f, emptyList())
        assertEquals(0f, result)
    }

    @Test
    fun testCalculateScrollPixel_SingleAnchor() {
        val anchors = listOf(AnchorPoint(5f, 100f))
        assertEquals(100f, SyncCalculator.calculateScrollPixel(2f, anchors))
        assertEquals(100f, SyncCalculator.calculateScrollPixel(5f, anchors))
        assertEquals(100f, SyncCalculator.calculateScrollPixel(10f, anchors))
    }

    @Test
    fun testCalculateScrollPixel_Interpolation() {
        val anchors = listOf(
            AnchorPoint(0f, 0f),
            AnchorPoint(10f, 100f),
            AnchorPoint(20f, 300f)
        )

        // 경계값 이전
        assertEquals(0f, SyncCalculator.calculateScrollPixel(-5f, anchors))
        
        // 경계값 0
        assertEquals(0f, SyncCalculator.calculateScrollPixel(0f, anchors))
        
        // 0초 ~ 10초 사이 보간 (5초 -> 50px)
        assertEquals(50f, SyncCalculator.calculateScrollPixel(5f, anchors))
        
        // 10초 ~ 20초 사이 보간 (15초 -> 200px)
        assertEquals(200f, SyncCalculator.calculateScrollPixel(15f, anchors))
        
        // 경계값 20
        assertEquals(300f, SyncCalculator.calculateScrollPixel(20f, anchors))
        
        // 경계값 이후
        assertEquals(300f, SyncCalculator.calculateScrollPixel(25f, anchors))
    }
}
