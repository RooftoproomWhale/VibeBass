package com.woong.vibebass.controller

import com.woong.vibebass.service.YoutubeSearchService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/youtube")
@CrossOrigin(origins = ["*"])
class YoutubeController(private val youtubeSearchService: YoutubeSearchService) {

    @GetMapping("/search")
    fun searchYoutubeVideo(@RequestParam query: String): ResponseEntity<Map<String, String>> {
        val videoId = youtubeSearchService.searchVideo(query)
        return ResponseEntity.ok(mapOf("videoId" to videoId))
    }
}
