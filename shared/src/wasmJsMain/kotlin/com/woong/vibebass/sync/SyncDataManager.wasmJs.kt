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
        // Only numeric fields cross this fragment; JavaScript serializes all text with JSON.stringify.
        val anchorPointsJson = anchorPoints.joinToString(separator = ",", prefix = "[", postfix = "]") {
            "{\"timeSec\": ${it.timeSec}, \"scrollPixel\": ${it.scrollPixel}, \"pagePosition\": ${it.pagePosition}}"
        }
        
        saveSyncDataJs(
            title = title,
            artist = artist,
            youtubeVideoId = youtubeVideoId,
            anchorPointsJson = anchorPointsJson,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    actual fun loadSongs(
        onSuccess: (List<SongData>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val songList = mutableListOf<SongData>()
        
        loadSongsJs(
            onSongItem = { id, title, artist, videoId, anchorsStr ->
                val anchors = if (anchorsStr.isEmpty()) {
                    emptyList()
                } else {
                    anchorsStr.split(",").map { anchorStr ->
                        val parts = anchorStr.split(":")
                        AnchorPoint(parts[0].toFloat(), parts[1].toFloat(), parts.getOrNull(2)?.toFloatOrNull())
                    }
                }
                
                songList.add(
                    SongData(
                        id = id.toLong(),
                        title = title,
                        artist = artist.ifEmpty { null },
                        youtubeVideoId = videoId,
                        anchorPoints = anchors
                    )
                )
            },
            onComplete = {
                onSuccess(songList)
            },
            onFailure = onFailure
        )
    }

}

// Kotlin/WasmJs 제약: js() 블록을 사용하는 함수는 반드시 클래스/Object 내부가 아닌 파일 최상단(Top-level) 함수로 선언해야 함
@OptIn(ExperimentalWasmJsInterop::class)
private fun saveSyncDataJs(
    title: String,
    artist: String,
    youtubeVideoId: String,
    anchorPointsJson: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    js("""
        window.vibeBassApi.saveSong(title, artist, youtubeVideoId, anchorPointsJson)
            .then(function() { onSuccess(); }, function(error) { onFailure(error.userMessage || '저장에 실패했습니다. 다시 시도해 주세요.'); });
    """)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun loadSongsJs(
    onSongItem: (Double, String, String, String, String) -> Unit, // WasmJs 브릿지 호환을 위해 id값은 Double로 바인딩
    onComplete: () -> Unit,
    onFailure: (String) -> Unit
) {
    js("""
        window.vibeBassApi.loadSongs()
        .then(function(songs) {
            songs.forEach(function(song) {
                var anchorsStr = "";
                if (song.anchorPoints && song.anchorPoints.length > 0) {
                    anchorsStr = song.anchorPoints.map(function(a) {
                        return a.timeSec + ":" + a.scrollPixel + ":" + (a.pagePosition == null ? "" : a.pagePosition);
                      }).join(",");
                }
                onSongItem(
                    song.id,
                    song.title,
                    song.artist || "",
                    song.youtubeVideoId,
                    anchorsStr
                );
            });
            onComplete();
        })
        .catch(function(err) {
            onFailure(err.userMessage || '보관함을 읽지 못했습니다. 다시 시도해 주세요.');
        });
    """)
}
