package com.woong.vibebass.sync

data class SongData(
    val id: Long,
    val title: String,
    val artist: String?,
    val youtubeVideoId: String,
    val anchorPoints: List<AnchorPoint>
)
