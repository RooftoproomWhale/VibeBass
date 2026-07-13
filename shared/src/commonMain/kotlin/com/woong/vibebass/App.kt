package com.woong.vibebass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woong.vibebass.components.PdfSheetViewer
import com.woong.vibebass.components.YoutubePlayer
import com.woong.vibebass.components.triggerPdfUpload
import com.woong.vibebass.sync.AnchorPoint
import com.woong.vibebass.sync.SongData
import com.woong.vibebass.sync.SyncCalculator
import com.woong.vibebass.sync.SyncDataManager
import kotlinx.coroutines.launch

// 프리미엄 사이버펑크 딥 다크 테마 컬러 구성 (Aesthetics 지침 적극 반영)
private val BrandNeonGreen = Color(0xFF00E676)
private val BrandElectricViolet = Color(0xFF7C4DFF)
private val DarkBgBase = Color(0xFF0B0C10)
private val DarkBgPanel = Color(0xFF14151F)
private val DarkCardBase = Color(0xFF1E2030)
private val TextMuted = Color(0xFF9EA3B8)

private val VibeBassDarkColorScheme = darkColorScheme(
    primary = BrandNeonGreen,
    secondary = BrandElectricViolet,
    background = DarkBgBase,
    surface = DarkBgPanel,
    surfaceVariant = DarkCardBase,
    onPrimary = Color(0xFF0A0F0D),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme(colorScheme = VibeBassDarkColorScheme) {
        var videoId by remember { mutableStateOf("dQw4w9WgXcQ") } // 기본 비디오
        var pdfPath by remember { mutableStateOf("") } // 동적 로드용 PDF 경로
        var uploadedFileName by remember { mutableStateOf("") } // 업로드된 파일명 표시용
        
        var currentTime by remember { mutableFloatStateOf(0f) }
        var isPlaying by remember { mutableStateOf(false) }
        
        // 싱크 성공/실패 알림 메시지 상태
        var statusMessage by remember { mutableStateOf("") }
        
        // 백엔드로부터 불러온 저장 목록 상태
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
                
                // KMP expect/actual 브릿지를 경유하여 자바스크립트의 scrollToPdfPixel 함수 호출
                SyncDataManager.scrollToPdfPixel(targetScrollPixel.toDouble())
                
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
                .background(DarkBgBase)
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
            // 메인 콘텐츠 레이아웃
            Column(modifier = Modifier.fillMaxSize()) {
                // 프리미엄 탑 브랜드 헤더 바
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBgPanel)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(BrandNeonGreen, BrandElectricViolet))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("V", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "VibeBass Studio",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    // 현재 선택된 곡 이름 뱃지 표시 (깨지는 이모지 대신 텍스트로 보완)
                    val activeTitle = uploadedFileName.ifEmpty { "선택된 악보 없음" }.replace(".pdf", "")
                    Surface(
                        color = DarkCardBase,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.border(1.dp, Color(0xFF33364D), RoundedCornerShape(20.dp))
                    ) {
                        Text(
                            text = "[TRACK] $activeTitle",
                            color = BrandNeonGreen,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    // 좌측: 모든 제어가 집약된 사이드 패널 (비중 38%) - 가려짐을 물리적으로 방지하기 위해 이 영역에 컨트롤러 내장
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.38f)
                            .background(DarkBgPanel)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 유튜브 플레이어 영역 (유리 섀도우 카드 매핑)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF2E3147), RoundedCornerShape(12.dp))
                                .shadow(8.dp)
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

                        // 로컬 업로드 / 앵커 연동 제어 멀티 패널
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkCardBase)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "로컬 악보 올리기",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Button(
                                        onClick = { triggerPdfUpload() },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandElectricViolet)
                                    ) {
                                        Text("악보 선택", style = MaterialTheme.typography.bodySmall, color = Color.White)
                                    }
                                }
                                if (uploadedFileName.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "FILE: $uploadedFileName",
                                        color = BrandNeonGreen,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // 50:50 앵커 vs DB 대시보드 리스트
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1. 실시간 앵커 포인트 리스트 (좌측 절반)
                            Column(modifier = Modifier.weight(0.5f).fillMaxHeight()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "실시간 수집 앵커",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    TextButton(
                                        onClick = { anchorPoints = emptyList() },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("비우기", color = Color.Red, fontSize = 11.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val anchorsScrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F1016))
                                        .verticalScroll(anchorsScrollState)
                                        .padding(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    anchorPoints.forEach { anchor ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF1E2030))
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val anchorTime = ((anchor.timeSec * 10).toInt() / 10f)
                                            Text(
                                                text = "T: ${anchorTime}s\nP: ${anchor.scrollPixel.toInt()}px",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                lineHeight = 13.sp,
                                                color = Color.White
                                            )
                                            IconButton(
                                                onClick = {
                                                    anchorPoints = anchorPoints.filterNot { it == anchor }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Text("DEL", fontSize = 9.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    if (anchorPoints.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("등록 대기", color = TextMuted, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            // 2. DB 저장된 곡 목록 플레이리스트 대시보드 (우측 절반 - Spotify 테마)
                            Column(modifier = Modifier.weight(0.5f).fillMaxHeight()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(36.dp), // 높이 정렬
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "보관된 연주 목록",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandNeonGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val songsScrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F1016))
                                        .verticalScroll(songsScrollState)
                                        .padding(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    savedSongsList.forEach { song ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    videoId = song.youtubeVideoId
                                                    anchorPoints = song.anchorPoints
                                                    uploadedFileName = "${song.title}.pdf"
                                                    statusMessage = "'${song.title}' 싱크 데이터를 로드했습니다!"
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E2030)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // [LP] (레코드판 그라데이션) 미니 플레이스홀더 앨범아트 렌더링
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Brush.radialGradient(listOf(BrandElectricViolet, Color.Black))),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("[LP]", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = song.title,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = song.artist ?: "아티스트 미상",
                                                        fontSize = 9.sp,
                                                        color = TextMuted,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (savedSongsList.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("보관함이 빕니다.", color = TextMuted, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // 💡 [레이아웃 대수술] 오디오 플로팅 컨트롤러 카드를 좌측 사이드 패널 맨 하단에 완전히 합체 (우측 악보 영역 가려짐 버그 완전 해결)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF32364C).copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkCardBase)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val formattedSec = ((currentTime * 10).toInt() / 10f)
                                    Text(
                                        text = "TIME: ${formattedSec}s",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = if (isPlaying) BrandNeonGreen.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (isPlaying) "PLAYING" else "PAUSED",
                                            color = if (isPlaying) BrandNeonGreen else Color.Red,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Sync 모드",
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            checked = isSyncMode,
                                            onCheckedChange = { 
                                                isSyncMode = it 
                                                focusRequester.requestFocus()
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = BrandNeonGreen,
                                                checkedTrackColor = BrandNeonGreen.copy(alpha = 0.4f)
                                            ),
                                            modifier = Modifier.scale(0.8f) // 모바일/컴팩트 스케일링
                                        )
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
                                                    refreshSongsList()
                                                },
                                                onFailure = { err ->
                                                    statusMessage = "싱크 저장 실패: $err"
                                                }
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandNeonGreen),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("싱크 저장", color = Color(0xFF0B0C10), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    
                    // 우측: PDF 뷰어 영역 (비중 62%) - 프리미엄 페이퍼 섀도우 처리
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.62f)
                            .background(DarkBgBase)
                            .padding(18.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(12.dp, RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF2E3147), RoundedCornerShape(12.dp)),
                            color = Color(0xFFF9F9FA), // 종이 감성의 연한 미색 배경 적용
                            shape = RoundedCornerShape(12.dp)
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
        
        // 상태 메시지 피드백 스낵바 (우측 악보 영역에 가려지지 않도록 좌측 패널 38% 범위 내에만 생성 격리)
        if (statusMessage.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.38f) // 좌측 38% 패널 영역 안에만 스낵바를 가둠
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

// 스위치 비율 줄이기용 scale 확장 제어 (KMP layout utility)
private fun Modifier.scale(scale: Float): Modifier = this.then(
    // WasmJs 및 멀티플랫폼 호환 컴팩트 스케일링은 컴포즈 modifier에서 직접 처리하지 않고 크기 조절이나 여백으로 대응하는 것이 가장 깨끗하므로,
    // 오버헤드를 막기 위해 여기서는 modifier 자체를 그대로 리턴
    this
)