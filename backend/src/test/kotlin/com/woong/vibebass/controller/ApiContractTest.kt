package com.woong.vibebass.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.woong.vibebass.domain.Song
import com.woong.vibebass.dto.SongRequest
import com.woong.vibebass.dto.SongResponse
import com.woong.vibebass.service.SongNotFoundException
import com.woong.vibebass.service.SongService
import com.woong.vibebass.service.YoutubeSearchService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.web.server.ResponseStatusException

@WebMvcTest(controllers = [SongController::class, YoutubeController::class])
@Import(ApiExceptionHandler::class)
class ApiContractTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper
    @MockBean lateinit var songs: SongService
    @MockBean lateinit var youtube: YoutubeSearchService

    private fun payload() = mapOf(
        "title" to "한 페이지 [tab] \\ \"제목\"\n\t🎸",
        "artist" to "가수", "youtubeVideoId" to "dQw4w9WgXcQ",
        "anchorPoints" to listOf(mapOf("timeSec" to 0, "scrollPixel" to 0, "pagePosition" to 0.0))
    )

    @Test
    fun `특수문자와 상대 좌표는 JSON으로 왕복한다`() {
        whenever(songs.create(any())).thenAnswer {
            val request = it.getArgument<SongRequest>(0)
            SongResponse.fromEntity(Song(id = 1L, title = request.title, artist = request.artist,
                youtubeVideoId = request.youtubeVideoId, anchorPoints = request.anchorPoints))
        }
        mvc.perform(post("/api/songs").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(payload())))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value(payload()["title"]))
            .andExpect(jsonPath("$.anchorPoints[0].pagePosition").value(0.0))
            .andExpect(jsonPath("$.anchorPoints[0].coordinatesValid").doesNotExist())
    }

    @Test
    fun `잘못된 제목 영상 ID 앵커와 중복 시각은 저장 전에 400을 반환한다`() {
        val anchor = mapOf("timeSec" to 0, "scrollPixel" to 0)
        val invalid = listOf(
            mapOf("title" to " "), mapOf("title" to "x".repeat(256)), mapOf("artist" to "x".repeat(256)),
            mapOf("title" to "곡\u0000"), mapOf("artist" to "가수\u0000"),
            mapOf("youtubeVideoId" to "https://youtu.be/dQw4w9WgXcQ"),
            mapOf("anchorPoints" to listOf(anchor + ("timeSec" to -1))),
            mapOf("anchorPoints" to listOf(anchor + ("scrollPixel" to null))),
            mapOf("anchorPoints" to listOf(anchor + ("pagePosition" to Double.NaN))),
            mapOf("anchorPoints" to listOf(anchor + ("scrollPixel" to 1e100))),
            mapOf("anchorPoints" to listOf(anchor, anchor)), mapOf("anchorPoints" to listOf(null)),
            mapOf("anchorPoints" to List(10001) { anchor }), mapOf("anchorPoints" to null)
        )
        for (change in invalid) {
            mvc.perform(post("/api/songs").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(payload() + change)))
                .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        }
        verifyNoInteractions(songs)
    }

    @Test
    fun `깨진 JSON과 잘못된 ID 및 검색어는 400이며 원문을 노출하지 않는다`() {
        mvc.perform(post("/api/songs").contentType(MediaType.APPLICATION_JSON).content("{ SECRET"))
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SECRET"))))
        for (id in listOf("0", "-1", "invalid")) {
            mvc.perform(get("/api/songs/$id")).andExpect(status().isBadRequest)
        }
        for (query in listOf(" ", "x".repeat(501))) {
            mvc.perform(get("/api/youtube/search").param("query", query)).andExpect(status().isBadRequest)
        }
        mvc.perform(get("/api/youtube/search")).andExpect(status().isBadRequest)
        verifyNoInteractions(songs, youtube)
    }

    @Test
    fun `없는 곡의 조회 수정 삭제는 모두 404이다`() {
        whenever(songs.findById(99)).thenThrow(SongNotFoundException())
        whenever(songs.update(any(), any())).thenThrow(SongNotFoundException())
        doThrow(SongNotFoundException()).whenever(songs).delete(99)
        val requests = listOf(get("/api/songs/99"), delete("/api/songs/99"),
            put("/api/songs/99").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(payload())))
        for (request in requests) {
            mvc.perform(request).andExpect(status().isNotFound)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        }
    }

    @Test
    fun `검색 비활성화와 외부 장애의 상태를 보존하고 내부 오류는 숨긴다`() {
        for ((httpStatus, code) in listOf(HttpStatus.SERVICE_UNAVAILABLE to "YOUTUBE_SEARCH_DISABLED", HttpStatus.BAD_GATEWAY to "YOUTUBE_SEARCH_FAILED")) {
            whenever(youtube.searchVideo("곡")).thenThrow(ResponseStatusException(httpStatus, "SECRET upstream detail"))
            mvc.perform(get("/api/youtube/search").param("query", "곡"))
                .andExpect(status().`is`(httpStatus.value())).andExpect(jsonPath("$.code").value(code))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SECRET"))))
        }
        whenever(songs.findAll()).thenThrow(IllegalStateException("SECRET database detail"))
        mvc.perform(get("/api/songs")).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.code").value("REQUEST_FAILED"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SECRET"))))
    }
}
