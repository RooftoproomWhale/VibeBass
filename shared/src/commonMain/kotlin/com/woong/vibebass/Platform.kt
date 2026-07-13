package com.woong.vibebass

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform