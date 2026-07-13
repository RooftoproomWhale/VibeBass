package com.woong.vibebass.sync

expect object SyncDataManager {
    fun saveSyncData(
        title: String,
        artist: String,
        youtubeVideoId: String,
        anchorPoints: List<AnchorPoint>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    )

    fun loadSongs(
        onSuccess: (List<SongData>) -> Unit,
        onFailure: (String) -> Unit
    )
}
