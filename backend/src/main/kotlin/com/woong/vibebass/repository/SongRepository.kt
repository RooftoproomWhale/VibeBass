package com.woong.vibebass.repository

import com.woong.vibebass.domain.Song
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SongRepository : JpaRepository<Song, Long>
