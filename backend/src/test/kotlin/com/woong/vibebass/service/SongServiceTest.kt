package com.woong.vibebass.service

import com.woong.vibebass.domain.Song
import com.woong.vibebass.dto.AnchorPointDto
import com.woong.vibebass.dto.SongRequest
import com.woong.vibebass.repository.SongRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.*

@ExtendWith(MockitoExtension::class)
class SongServiceTest {

    @Mock
    lateinit var songRepository: SongRepository

    @InjectMocks
    lateinit var songService: SongService

    @Test
    fun `성공 - 노래 정보 저장`() {
        // Given (실제 DTO 인스턴스 사용 - RULE 준수)
        val request = SongRequest(
            title = "입춘",
            artist = "한로로",
            youtubeVideoId = "dQw4w9WgXcQ",
            anchorPoints = listOf(AnchorPointDto(0.0, 0.0), AnchorPointDto(10.0, 300.0))
        )
        val savedSong = Song(
            id = 1L,
            title = request.title,
            artist = request.artist,
            youtubeVideoId = request.youtubeVideoId,
            anchorPoints = request.anchorPoints
        )
        whenever(songRepository.save(any<Song>())).thenReturn(savedSong)

        // When
        val response = songService.create(request)

        // Then
        assertNotNull(response.id)
        assertEquals("입춘", response.title)
        assertEquals("한로로", response.artist)
        assertEquals(2, response.anchorPoints.size)
        verify(songRepository).save(any<Song>())
    }

    @Test
    fun `성공 - 단건 조회`() {
        // Given
        val song = Song(
            id = 1L,
            title = "입춘",
            artist = "한로로",
            youtubeVideoId = "dQw4w9WgXcQ",
            anchorPoints = listOf(AnchorPointDto(0.0, 0.0))
        )
        whenever(songRepository.findById(1L)).thenReturn(Optional.of(song))

        // When
        val response = songService.findById(1L)

        // Then
        assertEquals(1L, response.id)
        assertEquals("입춘", response.title)
        verify(songRepository).findById(1L)
    }

    @Test
    fun `실패 - 존재하지 않는 ID 조회 시 예외 발생 (Edge Case)`() {
        // Given
        whenever(songRepository.findById(99L)).thenReturn(Optional.empty())

        // When & Then (Edge case validation)
        val exception = assertThrows<IllegalArgumentException> {
            songService.findById(99L)
        }
        assertTrue(exception.message!!.contains("존재하지 않는 곡입니다"))
        verify(songRepository).findById(99L)
    }

    @Test
    fun `실패 - 존재하지 않는 ID 삭제 시 예외 발생 (Edge Case)`() {
        // Given
        whenever(songRepository.existsById(99L)).thenReturn(false)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            songService.delete(99L)
        }
        assertTrue(exception.message!!.contains("삭제하려는 곡이 존재하지 않습니다"))
        verify(songRepository).existsById(99L)
    }
}
