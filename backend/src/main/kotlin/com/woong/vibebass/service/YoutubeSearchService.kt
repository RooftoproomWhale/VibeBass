package com.woong.vibebass.service

import io.netty.channel.ChannelOption
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.util.retry.Retry
import java.time.Duration

@Service
class YoutubeSearchService {

    companion object {
        private val log = LoggerFactory.getLogger(YoutubeSearchService::class.java)
    }

    private val webClient: WebClient

    @Value("\${youtube.api-key}")
    private lateinit var apiKey: String

    @Value("\${youtube.search-url}")
    private lateinit var searchUrl: String

    init {
        // Connect/Read Timeout 이 설정된 WebClient 생성 (회복탄력성 표준 준수)
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(5))

        this.webClient = WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }

    fun searchVideo(query: String): String {
        log.info("Requesting YouTube video search for query: '{}'", query)

        try {
            return webClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .path(searchUrl)
                        .queryParam("part", "snippet")
                        .queryParam("q", query)
                        .queryParam("type", "video")
                        .queryParam("maxResults", "1")
                        .queryParam("key", apiKey)
                        .build()
                }
                .retrieve()
                // 4xx (Client Error) 및 5xx (Server Error) 개별 예외 포착
                .onStatus(HttpStatusCode::is4xxClientError) { clientResponse ->
                    log.error("YouTube API 4xx Client Error. Status code: {}", clientResponse.statusCode())
                    clientResponse.bodyToMono(String::class.java)
                        .flatMap { body -> Mono.error(IllegalArgumentException("유튜브 API 요청 오류 (Client Error): $body")) }
                }
                .onStatus(HttpStatusCode::is5xxServerError) { serverResponse ->
                    log.error("YouTube API 5xx Server Error. Status code: {}", serverResponse.statusCode())
                    serverResponse.bodyToMono(String::class.java)
                        .flatMap { body -> Mono.error(IllegalStateException("유튜브 검색 서버 장애 (Server Error): $body")) }
                }
                .bodyToMono(Map::class.java)
                // 멱등원성 읽기 작업에 대한 지수 백오프 기반 재시도 (최대 3회)
                .retryWhen(
                    Retry.backoff(3, Duration.ofSeconds(1))
                        .filter { it is IllegalStateException }
                        .doBeforeRetry { retrySignal ->
                            log.warn("Retrying YouTube search API due to server error. Retry count: {}", retrySignal.totalRetries() + 1)
                        }
                )
                .map { response -> extractVideoId(response as Map<String, Any>) }
                .block() ?: throw IllegalStateException("유튜브 검색 결과 반환 실패")
        } catch (e: Exception) {
            log.error("YouTube search execution failed for query '{}'. Error: {}", query, e.message, e)
            throw IllegalStateException("유튜브 검색 중 예외가 발생했습니다. 원인: ${e.message}", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractVideoId(response: Map<String, Any>): String {
        val items = response["items"] as? List<Map<String, Any>>
            ?: throw IllegalArgumentException("유튜브 응답 결과가 비어 있습니다.")
        
        if (items.isEmpty()) {
            log.warn("No YouTube videos found matching the query.")
            throw IllegalArgumentException("검색 결과와 일치하는 유튜브 비디오가 없습니다.")
        }

        val firstItem = items[0]
        val idMap = firstItem["id"] as? Map<String, Any>
            ?: throw IllegalArgumentException("올바르지 않은 유튜브 응답 규격입니다.")

        val videoId = idMap["videoId"] as? String
            ?: throw IllegalArgumentException("올바르지 않은 유튜브 비디오 ID 규격입니다.")

        log.info("Successfully matched YouTube Video ID: {}", videoId)
        return videoId
    }
}
