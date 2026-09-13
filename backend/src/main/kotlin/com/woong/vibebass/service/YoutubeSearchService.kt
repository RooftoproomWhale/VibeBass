package com.woong.vibebass.service

import io.netty.channel.ChannelOption
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder
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
        val key = apiKey.trim()
        if (key.isEmpty()) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "자동 검색을 사용할 수 없습니다. YouTube 링크를 직접 입력해 주세요.")
        }

        try {
            val targetUri = UriComponentsBuilder.fromHttpUrl(searchUrl)
                .queryParam("part", "snippet")
                .queryParam("q", "{query}")
                .queryParam("type", "video")
                .queryParam("maxResults", "1")
                .encode()
                .buildAndExpand(query)
                .toUri()

            return webClient.get()
                .uri(targetUri)
                .header("X-Goog-Api-Key", key)
                .retrieve()
                // 4xx (Client Error) 및 5xx (Server Error) 개별 예외 포착
                .onStatus(HttpStatusCode::is4xxClientError) { clientResponse ->
                    log.error("YouTube API 4xx Client Error. Status code: {}", clientResponse.statusCode())
                    clientResponse.releaseBody()
                        .then(Mono.error(IllegalArgumentException("YouTube API client error")))
                }
                .onStatus(HttpStatusCode::is5xxServerError) { serverResponse ->
                    log.error("YouTube API 5xx Server Error. Status code: {}", serverResponse.statusCode())
                    serverResponse.releaseBody()
                        .then(Mono.error(IllegalStateException("YouTube API server error")))
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
            // Network exceptions can contain request headers/URIs. Never retain their message or cause.
            log.warn("YouTube search failed. Exception type: {}", e.javaClass.simpleName)
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "자동 검색에 실패했습니다. YouTube 링크를 직접 입력해 주세요.")
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
