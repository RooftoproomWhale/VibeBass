package com.woong.vibebass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woong.vibebass.components.PdfSheetViewer
import com.woong.vibebass.components.YoutubePlayer
import com.woong.vibebass.components.triggerPdfUpload
import com.woong.vibebass.sync.AnchorPoint
import com.woong.vibebass.sync.SongData
import com.woong.vibebass.sync.SyncCalculator
import com.woong.vibebass.sync.SyncDataManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme {
        var videoId by remember { mutableStateOf("dQw4w9WgXcQ") } // 기본 비디오
        var pdfPath by remember { mutableStateOf("") } // 동적 로드용 PDF 경로
        var uploadedFileName by remember { mutableStateOf("") } // 업로드된 파일명 표시용
        
        var currentTime by remember { mutableFloatStateOf(0f) }
        var isPlaying by remember { mutableStateOf(false) }
        
        // 싱크 성공/실패 알림 메시지 상태
        var statusMessage by remember { mutableStateOf("") }
        
        // 백엔드로부터 불러온 저장 목록 상태 추가
        var savedSongsList by remember { mutableStateOf<List<SongData>>(emptyList()) }
        
        // 앵커 포인트 리스트 (초기 데이터)
        var anchorPoints by remember {
            mutableStateOf(
                listOf(
                    AnchorPoint(0f, 0f),
                    AnchorPoint(10f, 300f),
                    AnchorPoint(20f, 800f),
                    AnchorPoint(40f, 1500f)
                )
            )
        }
        
        var isSyncMode by remember { mutableStateOf(false) }
        
        val scrollState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        // DB 노래 목록 동적 갱신 헬퍼 함수
        fun refreshSongsList() {
            SyncDataManager.loadSongs(
                onSuccess = { list -> savedSongsList = list },
                onFailure = { err -> statusMessage = "목록 갱신 실패: $err" }
            )
        }
        
        // 앱 구동 시 노래 목록 최초 로딩
        LaunchedEffect(Unit) {
            refreshSongsList()
        }
        
        // 선형 보간 자동 스크롤 로직 연동
        LaunchedEffect(currentTime, isSyncMode) {
            if (!isSyncMode && anchorPoints.isNotEmpty()) {
                val targetScrollPixel = SyncCalculator.calculateScrollPixel(currentTime, anchorPoints)
                
                // 스무딩 처리 (애니메이션 스펙을 사용해 목적지까지 감쇠하며 부드럽게 이동)
                val itemHeight = 300f
                val targetIndex = (targetScrollPixel / itemHeight).toInt()
                val targetOffset = (targetScrollPixel % itemHeight).toInt()
                
                if (targetIndex >= 0) {
                    launch {
                        scrollState.animateScrollToItem(targetIndex, targetOffset)
                    }
                }
            }
        }
        
        val focusRequester = remember { FocusRequester() }
        
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { keyEvent ->
                    // 싱크 모드에서 스페이스바 입력 감지 시 앵커 포인트 추가
                    if (isSyncMode && keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Spacebar) {
                        val itemHeight = 300f
                        val currentScrollPixel = (scrollState.firstVisibleItemIndex * itemHeight) + scrollState.firstVisibleItemScrollOffset
                        
                        val roundedTime = ((currentTime * 10).toInt() / 10f)
                        val newAnchor = AnchorPoint(roundedTime, currentScrollPixel)
                        
                        anchorPoints = (anchorPoints.filterNot { it.timeSec == roundedTime } + newAnchor).sortedBy { it.timeSec }
                        true
                    } else {
                        false
                    }
                }
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // 좌측: 유튜브 플레이어 & 싱크 관리 패널 (비중 40%)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.4f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "VibeBass 싱크 매니저",
                        modifier = Modifier.statusBarsPadding(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 유튜브 플레이어 영역
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.Black)
                    ) {
                        YoutubePlayer(
                            videoId = videoId,
                            currentTime = currentTime,
                            isPlaying = isPlaying,
                            onTimeUpdate = { currentTime = it },
                            onStateChange = { isPlaying = it },
                            onVideoIdFound = { videoId = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    val formattedTime = ((currentTime * 10).toInt() / 10f)
                    Text(
                        text = "현재 재생 시간: $formattedTime 초",
                        modifier = Modifier.statusBarsPadding(),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // 로컬 업로드 카드
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "로컬 악보 등록",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { triggerPdfUpload() },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("PDF 악보 선택", style = MaterialTheme.typography.bodySmall)
                                }
                                
                                Text(
                                    text = if (uploadedFileName.isNotEmpty()) uploadedFileName else "선택된 파일 없음",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                    
                    // 스페이스바 싱크 모드 활성화 카드
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSyncMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Spacebar 싱크 모드 활성화",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Switch(
                                    checked = isSyncMode,
                                    onCheckedChange = { 
                                        isSyncMode = it 
                                        focusRequester.requestFocus()
                                    }
                                )
                            }
                        }
                    }
                    
                    // 패널 하단 영역: 실시간 앵커 포인트 리스트와 DB 저장 목록을 50:50 비율로 안분
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. 실시간 수집 앵커 포인트 패널 (가로 절반)
                        Column(
                            modifier = Modifier
                                .weight(0.5f)
                                .fillMaxHeight()
                        ) {
                            Text(
                                text = "실시간 수집 앵커",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val anchorsScrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .verticalScroll(anchorsScrollState)
                                    .padding(6.dp)
                            ) {
                                anchorPoints.forEach { anchor ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val anchorTime = ((anchor.timeSec * 10).toInt() / 10f)
                                        Text(
                                            text = "⏱️${anchorTime}s -> 📜${anchor.scrollPixel.toInt()}px",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Button(
                                            onClick = {
                                                anchorPoints = anchorPoints.filterNot { it == anchor }
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("삭제", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                if (anchorPoints.isEmpty()) {
                                    Text(
                                        text = "등록된 앵커가 없습니다.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        // 2. DB 저장된 노래 목록 대시보드 패널 (가로 절반)
                        Column(
                            modifier = Modifier
                                .weight(0.5f)
                                .fillMaxHeight()
                        ) {
                            Text(
                                text = "DB 저장 목록",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val songsScrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .verticalScroll(songsScrollState)
                                    .padding(6.dp)
                            ) {
                                savedSongsList.forEach { song ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        onClick = {
                                            // 노래 목록 항목 클릭 시 저장된 싱크 데이터 즉시 연동 반영!
                                            videoId = song.youtubeVideoId
                                            anchorPoints = song.anchorPoints
                                            uploadedFileName = "${song.title}.pdf"
                                            statusMessage = "'${song.title}' 싱크 데이터를 불러왔습니다!"
                                        }
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp)) {
                                            Text(
                                                text = song.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = song.artist ?: "아티스트 미상",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "🔗 앵커: ${song.anchorPoints.size}개",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                                if (savedSongsList.isEmpty()) {
                                    Text(
                                        text = "저장된 노래가 없습니다.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                    
                    // 하단 제어 버튼
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { anchorPoints = emptyList() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text("모두 초기화", style = MaterialTheme.typography.bodyMedium)
                        }
                        
                        Button(
                            onClick = {
                                val cleanTitle = uploadedFileName.ifEmpty { "입춘" }.replace(".pdf", "")
                                SyncDataManager.saveSyncData(
                                    title = cleanTitle,
                                    artist = if (cleanTitle == "입춘") "한로로" else "아티스트 미상",
                                    youtubeVideoId = videoId,
                                    anchorPoints = anchorPoints,
                                    onSuccess = {
                                        statusMessage = "싱크 데이터가 백엔드 DB에 성공적으로 저장되었습니다!"
                                        // 저장 성공 시 목록 목록 실시간으로 다시 로딩하여 대시보드 자동 갱신!
                                        refreshSongsList()
                                    },
                                    onFailure = { err ->
                                        statusMessage = "싱크 저장 실패: $err"
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text("싱크 저장", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                
                // 우측: PDF 뷰어 영역 (비중 60%)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.6f)
                        .padding(16.dp)
                ) {
                    PdfSheetViewer(
                        pdfSource = pdfPath,
                        scrollState = scrollState,
                        onPdfFileSelected = { fileName, objectUrl -> 
                            uploadedFileName = fileName
                            pdfPath = objectUrl
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        
        // 상태 메시지 피드백 스낵바
        if (statusMessage.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Snackbar(
                    action = {
                        TextButton(onClick = { statusMessage = "" }) {
                            Text("확인", color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    }
                ) {
                    Text(statusMessage)
                }
            }
        }
    }
}