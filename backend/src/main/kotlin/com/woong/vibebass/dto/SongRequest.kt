package com.woong.vibebass.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class SongRequest(
    @field:NotBlank(message = "곡 제목은 필수 입력 항목입니다.")
    val title: String,

    val artist: String?,

    @field:NotBlank(message = "유튜브 비디오 ID는 필수 입력 항목입니다.")
    val youtubeVideoId: String,

    @field:NotNull(message = "앵커 포인트 리스트는 필수 항목입니다.")
    val anchorPoints: List<AnchorPointDto>
)
