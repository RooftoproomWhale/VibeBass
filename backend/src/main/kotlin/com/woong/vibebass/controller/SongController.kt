package com.woong.vibebass.controller

import com.woong.vibebass.dto.SongRequest
import com.woong.vibebass.dto.SongResponse
import com.woong.vibebass.service.SongService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/songs")
@Validated
@CrossOrigin(origins = ["*"])
class SongController(private val songService: SongService) {

    @GetMapping
    fun getSongs(): ResponseEntity<List<SongResponse>> {
        return ResponseEntity.ok(songService.findAll())
    }

    @GetMapping("/{id}")
    fun getSong(@PathVariable id: Long): ResponseEntity<SongResponse> {
        return ResponseEntity.ok(songService.findById(id))
    }

    @PostMapping
    fun createSong(@RequestBody @Valid request: SongRequest): ResponseEntity<SongResponse> {
        val song = songService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(song)
    }

    @PutMapping("/{id}")
    fun updateSong(
        @PathVariable id: Long,
        @RequestBody @Valid request: SongRequest
    ): ResponseEntity<SongResponse> {
        return ResponseEntity.ok(songService.update(id, request))
    }

    @DeleteMapping("/{id}")
    fun deleteSong(@PathVariable id: Long): ResponseEntity<Void> {
        songService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
