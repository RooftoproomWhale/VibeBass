package com.woong.vibebass.dto

import com.woong.vibebass.domain.Song
import java.time.LocalDateTime

data class SongResponse(
    val id: Long?,
    val title: String,
    val artist: String?,
    val youtubeVideoId: String,
    val anchorPoints: List<AnchorPointDto>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromEntity(song: Song): SongResponse {
            return SongResponse(
                id = song.id,
                title = song.title,
                artist = song.artist,
                youtubeVideoId = song.youtubeVideoId,
                anchorPoints = song.anchorPoints,
                createdAt = song.createdAt,
                updatedAt = song.updatedAt
            )
        }
    }
}
