package com.woong.vibebass.sync

import kotlin.js.ExperimentalWasmJsInterop

actual object SyncDataManager {
    
    @OptIn(ExperimentalWasmJsInterop::class)
    actual fun saveSyncData(
        title: String,
        artist: String,
        youtubeVideoId: String,
        anchorPoints: List<AnchorPoint>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // 수작업으로 슬림하고 오차 없는 JSON 문자열 포맷팅 수행
        val anchorPointsJson = anchorPoints.joinToString(separator = ",", prefix = "[", postfix = "]") {
            "{\"timeSec\": ${it.timeSec}, \"scrollPixel\": ${it.scrollPixel}}"
        }
        
        // JSON 문자열 이스케이프 및 구조 생성
        val escapedTitle = title.replace("\"", "\\\"")
        val escapedArtist = artist.replace("\"", "\\\"")
        val escapedVideoId = youtubeVideoId.replace("\"", "\\\"")
        
        val requestJson = """
            {
                "title": "$escapedTitle",
                "artist": "$escapedArtist",
                "youtubeVideoId": "$escapedVideoId",
                "anchorPoints": $anchorPointsJson
            }
        """.trimIndent()
        
        saveSyncDataJs(
            jsonPayload = requestJson,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }
}

// Kotlin/WasmJs 제약: js() 블록을 사용하는 함수는 반드시 클래스/Object 내부가 아닌 파일 최상단(Top-level) 함수로 선언해야 함
@OptIn(ExperimentalWasmJsInterop::class)
private fun saveSyncDataJs(
    jsonPayload: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    js("""
        fetch('http://localhost:8082/api/songs', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonPayload
        })
        .then(function(response) {
            if (response.ok) {
                onSuccess();
            } else {
                response.text().then(function(errText) {
                    onFailure(errText || '서버 응답 오류');
                });
            }
        })
        .catch(function(err) {
            onFailure(err.message || '네트워크 연결 실패');
        });
    """)
}
