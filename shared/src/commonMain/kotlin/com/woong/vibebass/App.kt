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
import androidx.compose.ui.unit.dp
import com.woong.vibebass.components.PdfSheetViewer
import com.woong.vibebass.components.YoutubePlayer
import com.woong.vibebass.components.triggerPdfUpload
import com.woong.vibebass.sync.AnchorPoint
import com.woong.vibebass.sync.SyncCalculator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme {
        var videoId by remember { mutableStateOf("dQw4w9WgXcQ") } // Rick Astley - Never Gonna Give You Up (테스트용)
        var pdfPath by remember { mutableStateOf("") } // 동적 로드용 PDF 경로 (Object URL)
        var uploadedFileName by remember { mutableStateOf("") } // 업로드된 파일명 표시용
        
        var currentTime by remember { mutableFloatStateOf(0f) }
        var isPlaying by remember { mutableStateOf(false) }
        
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "VibeBass 싱크 매니저",
                        modifier = Modifier.statusBarsPadding(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // [순서 변경 1] 유튜브 플레이어 영역을 맨 위로 올려 absolute 포지션(top: 100px)과 오버랩시킴
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(Color.Black)
                    ) {
                        YoutubePlayer(
                            videoId = videoId,
                            currentTime = currentTime,
                            isPlaying = isPlaying,
                            onTimeUpdate = { currentTime = it },
                            onStateChange = { isPlaying = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    val formattedTime = ((currentTime * 10).toInt() / 10f)
                    Text(
                        text = "현재 재생 시간: $formattedTime 초",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    // [순서 변경 2] 로컬 업로드 카드를 유튜브 플레이어 아래로 내려 가려짐 방지
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "로컬 악보 등록",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { triggerPdfUpload() }
                                ) {
                                    Text("PDF 악보 선택")
                                }
                                
                                Text(
                                    text = if (uploadedFileName.isNotEmpty()) uploadedFileName else "선택된 파일 없음",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                    
                    // 싱크 매니저 카드 설정
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSyncMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Spacebar 싱크 모드 활성화",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Switch(
                                    checked = isSyncMode,
                                    onCheckedChange = { 
                                        isSyncMode = it 
                                        focusRequester.requestFocus()
                                    }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isSyncMode) {
                                    "작동 중: 악보를 내리다가 연주 타이밍에 맞추어 [Spacebar]를 누르면 현재 화면의 스크롤 위치와 유튜브 재생 시간이 연동되어 앵커 포인트로 저장됩니다."
                                } else {
                                    "대기 중: 재생 시 등록된 앵커 데이터에 맞춰 악보가 실시간으로 자동 부드럽게 스크롤됩니다."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Text(
                        text = "앵커 포인트 리스트 (JSONB 데이터)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    val anchorsScrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .verticalScroll(anchorsScrollState)
                            .padding(8.dp)
                    ) {
                        anchorPoints.forEach { anchor ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val anchorTime = ((anchor.timeSec * 10).toInt() / 10f)
                                Text(
                                    text = "⏱️ ${anchorTime}초 -> 📜 ${anchor.scrollPixel.toInt()}px",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(
                                    onClick = {
                                        anchorPoints = anchorPoints.filterNot { it == anchor }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("삭제", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        if (anchorPoints.isEmpty()) {
                            Text(
                                text = "등록된 앵커 포인트가 없습니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { anchorPoints = emptyList() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("모두 초기화")
                        }
                        
                        Button(
                            onClick = {
                                println("Saved JSONB: ${anchorPoints.map { "{\"time_sec\": ${it.timeSec}, \"scroll_pixel\": ${it.scrollPixel}}" }}")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("싱크 저장")
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
    }
}