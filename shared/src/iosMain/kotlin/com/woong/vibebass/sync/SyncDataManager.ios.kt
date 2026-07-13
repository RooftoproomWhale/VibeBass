package com.woong.vibebass.sync

actual object SyncDataManager {
    actual fun saveSyncData(
        title: String,
        artist: String,
        youtubeVideoId: String,
        anchorPoints: List<AnchorPoint>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // iOS용 저장 구현 뼈대
    }

    actual fun loadSongs(
        onSuccess: (List<SongData>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // iOS용 로드 구현 뼈대
    }
}
