package com.woong.vibebass

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PracticeControlsTest {
    @Test
    fun acceptsVideoLinksWithoutAcceptingLookalikeHosts() {
        val id = "dQw4w9WgXcQ"
        listOf(id, "https://youtu.be/$id?t=12", "https://www.youtube.com/watch?v=$id&list=abc",
            "https://m.youtube.com/watch?list=abc&v=$id", "https://youtube.com/shorts/$id")
            .forEach { assertEquals(id, youtubeVideoId(it)) }
        listOf("https://youtube.com.evil.test/watch?v=$id", "https://example.com/$id", "<script>", "$id-more")
            .forEach { assertNull(youtubeVideoId(it)) }
    }

    @Test
    fun formatsPlaybackTimeWithoutInvalidValues() {
        assertEquals("01:05", practiceTime(65.9f))
        assertEquals("00:00", practiceTime(-1f))
        assertEquals("00:00", practiceTime(Float.NaN))
    }
}
