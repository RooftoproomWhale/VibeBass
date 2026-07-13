package com.woong.vibebass.domain

import com.woong.vibebass.dto.AnchorPointDto
import jakarta.persistence.*
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.Where
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "songs")
@SQLDelete(sql = "UPDATE songs SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
class Song(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var title: String,

    var artist: String? = null,

    @Column(name = "youtube_video_id", nullable = false)
    var youtubeVideoId: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "anchor_points", columnDefinition = "jsonb", nullable = false)
    var anchorPoints: List<AnchorPointDto> = emptyList(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
) {
    @PreUpdate
    fun preUpdate() {
        this.updatedAt = LocalDateTime.now()
    }
}
