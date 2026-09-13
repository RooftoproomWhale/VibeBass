package com.woong.vibebass.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.http.HttpStatus
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
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

        val exchangeFunction = ExchangeFunction { request ->
            assertEquals("TEST_API_KEY", request.headers().getFirst("X-Goog-Api-Key"))
            assertFalse(request.url().toString().contains("TEST_API_KEY"))
            assertFalse(request.url().rawQuery.contains("&key="))
            assertTrue(request.url().rawQuery.contains("%26key%3Dinjected"))
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
        val videoId = youtubeSearchService.searchVideo("한로로 입춘 &key=injected")

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
        val exception = assertThrows<ResponseStatusException> {
            youtubeSearchService.searchVideo("존재할수없는곡명")
        }
        assertEquals(HttpStatus.BAD_GATEWAY, exception.statusCode)
        assertNull(exception.cause)
    }

    @Test
    fun `키가 없거나 공백이면 외부 호출 없이 503을 반환한다`() {
        var requests = 0
        val client = WebClient.builder().exchangeFunction {
            requests++
            Mono.error(AssertionError("API key is missing"))
        }.build()
        ReflectionTestUtils.setField(youtubeSearchService, "webClient", client)
        for (key in listOf("", "   ")) {
            ReflectionTestUtils.setField(youtubeSearchService, "apiKey", key)
            val exception = assertThrows<ResponseStatusException> { youtubeSearchService.searchVideo("곡명") }
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.statusCode)
            assertNull(exception.cause)
        }
        assertEquals(0, requests)
    }

    @Test
    @ExtendWith(OutputCaptureExtension::class)
    fun `외부 오류 본문과 네트워크 예외에 포함된 키는 응답과 로그에 남기지 않는다`(output: CapturedOutput) {
        val responses = listOf(
            Mono.just(ClientResponse.create(HttpStatus.FORBIDDEN).body("TEST_API_KEY upstream detail").build()),
            Mono.just(ClientResponse.create(HttpStatus.FORBIDDEN).build()),
            Mono.error<ClientResponse>(RuntimeException("Request failed: TEST_API_KEY upstream detail"))
        )
        for (response in responses) {
            val client = WebClient.builder().exchangeFunction { response }.build()
            ReflectionTestUtils.setField(youtubeSearchService, "webClient", client)
            val exception = assertThrows<ResponseStatusException> { youtubeSearchService.searchVideo("곡명") }
            assertEquals(HttpStatus.BAD_GATEWAY, exception.statusCode)
            assertNull(exception.cause)
            assertFalse(exception.toString().contains("TEST_API_KEY"))
            assertFalse(exception.toString().contains("upstream detail"))
        }
        assertFalse(output.all.contains("TEST_API_KEY"))
        assertFalse(output.all.contains("upstream detail"))
    }
}
