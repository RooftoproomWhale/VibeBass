package com.woong.vibebass.controller

import com.woong.vibebass.service.SongNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class ApiExceptionHandler : ResponseEntityExceptionHandler() {
    // Keep Spring's HTTP statuses and headers, but never expose rejected input or exception details.
    override fun handleExceptionInternal(
        ex: Exception, body: Any?, headers: HttpHeaders, statusCode: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? = super.handleExceptionInternal(ex, problem(statusCode.value()), headers, statusCode, request)

    @ExceptionHandler(SongNotFoundException::class)
    fun songNotFound(): ResponseEntity<ProblemDetail> = ResponseEntity.status(404).body(problem(404))

    @ExceptionHandler(Exception::class)
    fun unexpected(ex: Exception): ResponseEntity<ProblemDetail> {
        LoggerFactory.getLogger(javaClass).error("API request failed. Exception type: {}", ex.javaClass.simpleName)
        return ResponseEntity.internalServerError().body(problem(500))
    }

    private fun problem(status: Int): ProblemDetail {
        val (code, detail) = when (status) {
            400 -> "INVALID_REQUEST" to "입력 형식을 확인해 주세요. 제목, 영상 ID와 싱크 위치가 올바르지 않습니다."
            404 -> "NOT_FOUND" to "요청한 곡 또는 경로를 찾을 수 없습니다."
            405 -> "METHOD_NOT_ALLOWED" to "지원하지 않는 요청 방식입니다."
            415 -> "UNSUPPORTED_MEDIA_TYPE" to "JSON 형식으로 요청해 주세요."
            502 -> "YOUTUBE_SEARCH_FAILED" to "자동 검색에 실패했습니다. YouTube 링크를 직접 입력해 주세요."
            503 -> "YOUTUBE_SEARCH_DISABLED" to "자동 검색이 비활성화되어 있습니다. YouTube 링크를 직접 입력해 주세요."
            else -> "REQUEST_FAILED" to "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."
        }
        return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail).apply { setProperty("code", code) }
    }
}
