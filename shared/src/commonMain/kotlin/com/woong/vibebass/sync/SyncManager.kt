package com.woong.vibebass.sync

data class AnchorPoint(
    val timeSec: Float,
    val scrollPixel: Float
)

object SyncCalculator {
    /**
     * 현재 재생 시간(currentTime)에 따른 악보 스크롤 픽셀 값을 선형 보간(Linear Interpolation)하여 반환합니다.
     */
    fun calculateScrollPixel(currentTime: Float, anchors: List<AnchorPoint>): Float {
        if (anchors.isEmpty()) return 0f
        
        // 시간 오름차순으로 정렬
        val sorted = anchors.sortedBy { it.timeSec }
        
        // 현재 시간이 첫 번째 앵커의 시간보다 작거나 같을 경우
        if (currentTime <= sorted.first().timeSec) {
            return sorted.first().scrollPixel
        }
        
        // 현재 시간이 마지막 앵커의 시간보다 크거나 같을 경우
        if (currentTime >= sorted.last().timeSec) {
            return sorted.last().scrollPixel
        }
        
        // 현재 시간 바로 직후에 있는 앵커의 인덱스를 탐색
        val nextIndex = sorted.indexOfFirst { it.timeSec > currentTime }
        if (nextIndex == -1 || nextIndex == 0) {
            return sorted.last().scrollPixel
        }
        
        val p1 = sorted[nextIndex - 1]
        val p2 = sorted[nextIndex]
        
        // 선형 보간 공식 적용: y = y1 + (y2 - y1) * (t - t1) / (t2 - t1)
        val timeDiff = p2.timeSec - p1.timeSec
        if (timeDiff == 0f) return p1.scrollPixel
        
        val ratio = (currentTime - p1.timeSec) / timeDiff
        return p1.scrollPixel + (p2.scrollPixel - p1.scrollPixel) * ratio
    }
}
