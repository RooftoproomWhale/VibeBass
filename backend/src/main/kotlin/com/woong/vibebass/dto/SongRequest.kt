package com.woong.vibebass.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class SongRequest(
    @field:NotBlank(message = "곡 제목은 필수 입력 항목입니다.")
    @field:Size(max = 255, message = "곡 제목은 255자 이하여야 합니다.")
    @field:Pattern(regexp = "[^\\x00]*", message = "곡 제목에 NUL 문자를 넣을 수 없습니다.")
    val title: String,

    @field:Size(max = 255, message = "가수명은 255자 이하여야 합니다.")
    @field:Pattern(regexp = "[^\\x00]*", message = "가수명에 NUL 문자를 넣을 수 없습니다.")
    val artist: String? = null,

    @field:NotBlank(message = "유튜브 비디오 ID는 필수 입력 항목입니다.")
    @field:Pattern(regexp = "[A-Za-z0-9_-]{11}", message = "유튜브 영상 ID는 11자리여야 합니다.")
    val youtubeVideoId: String,

    @field:Valid
    @field:Size(max = 10000, message = "앵커 포인트는 10000개 이하여야 합니다.")
    @field:JsonSetter(contentNulls = Nulls.FAIL)
    val anchorPoints: List<AnchorPointDto>
) {
    @get:JsonIgnore
    @get:AssertTrue(message = "같은 시각의 앵커를 중복 저장할 수 없습니다.")
    val uniqueAnchorTimes: Boolean
        get() = anchorPoints.map { if (it.timeSec == 0.0) 0.0 else it.timeSec }.toSet().size == anchorPoints.size
}
