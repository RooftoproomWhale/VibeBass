package com.woong.vibebass.service

import com.woong.vibebass.domain.Song
import com.woong.vibebass.dto.SongRequest
import com.woong.vibebass.dto.SongResponse
import com.woong.vibebass.repository.SongRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SongService(private val songRepository: SongRepository) {

    companion object {
        private val log = LoggerFactory.getLogger(SongService::class.java)
    }

    fun findAll(): List<SongResponse> {
        log.info("Request to find all songs")
        return songRepository.findAll().map { SongResponse.fromEntity(it) }
    }

    fun findById(id: Long): SongResponse {
        log.info("Request to find song by id: {}", id)
        val song = songRepository.findById(id)
            .orElseThrow {
                log.error("Song not found with id: {}", id)
                IllegalArgumentException("존재하지 않는 곡입니다. ID: $id")
            }
        return SongResponse.fromEntity(song)
    }

    @Transactional
    fun create(request: SongRequest): SongResponse {
        log.info("Request to create song: {}", request.title)
        val song = Song(
            title = request.title,
            artist = request.artist,
            youtubeVideoId = request.youtubeVideoId,
            anchorPoints = request.anchorPoints
        )
        val savedSong = songRepository.save(song)
        log.info("Song created successfully with id: {}", savedSong.id)
        return SongResponse.fromEntity(savedSong)
    }

    @Transactional
    fun update(id: Long, request: SongRequest): SongResponse {
        log.info("Request to update song with id: {}", id)
        val song = songRepository.findById(id)
            .orElseThrow {
                log.error("Song update failed. Song not found with id: {}", id)
                IllegalArgumentException("수정하려는 곡이 존재하지 않습니다. ID: $id")
            }

        song.title = request.title
        song.artist = request.artist
        song.youtubeVideoId = request.youtubeVideoId
        song.anchorPoints = request.anchorPoints

        log.info("Song updated successfully with id: {}", id)
        return SongResponse.fromEntity(song)
    }

    @Transactional
    fun delete(id: Long) {
        log.info("Request to delete song with id: {}", id)
        if (!songRepository.existsById(id)) {
            log.error("Song delete failed. Song not found with id: {}", id)
            throw IllegalArgumentException("삭제하려는 곡이 존재하지 않습니다. ID: $id")
        }
        songRepository.deleteById(id)
        log.info("Song deleted successfully (soft delete) with id: {}", id)
    }
}
