package com.woong.vibebass.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class YoutubeSearchServiceTest {

    private lateinit var youtubeSearchService: YoutubeSearchService

    @BeforeEach
    fun setUp() {
        youtubeSearchService = YoutubeSearchService()
        ReflectionTestUtils.setField(youtubeSearchService, "apiKey", "TEST_API_KEY")
        ReflectionTestUtils.setField(youtubeSearchService, "searchUrl", "https://www.googleapis.com/youtube/v3/search")
    }

    @Test
    fun `성공 - 유튜브 비디오 검색 및 ID 파싱`() {
        // Given - ExchangeFunction을 모킹해 복잡한 체이닝 stubbing 오류를 차단하고 가상 JSON 응답 반환
        val mockResponseJson = """
            {
                "items": [
                    {
                        "id": {
                            "videoId": "dQw4w9WgXcQ"
                        }
                    }
                ]
            }
        """.trimIndent()

        val exchangeFunction = ExchangeFunction {
            Mono.just(
                ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(mockResponseJson)
                    .build()
            )
        }
        val fakeWebClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
        ReflectionTestUtils.setField(youtubeSearchService, "webClient", fakeWebClient)

        // When
        val videoId = youtubeSearchService.searchVideo("한로로 입춘")

        // Then
        assertEquals("dQw4w9WgXcQ", videoId)
    }

    @Test
    fun `실패 - 검색 결과가 비어있을 때 예외 발생 (Edge Case)`() {
        // Given - 빈 items 배열 응답 반환
        val mockResponseJson = """
            {
                "items": []
            }
        """.trimIndent()

        val exchangeFunction = ExchangeFunction {
            Mono.just(
                ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(mockResponseJson)
                    .build()
            )
        }
        val fakeWebClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
        ReflectionTestUtils.setField(youtubeSearchService, "webClient", fakeWebClient)

        // When & Then (Edge Case 예외 검증)
        val exception = assertThrows<IllegalStateException> {
            youtubeSearchService.searchVideo("존재할수없는곡명")
        }
        assertTrue(exception.message!!.contains("유튜브 검색 중 예외가 발생했습니다"))
    }
}
