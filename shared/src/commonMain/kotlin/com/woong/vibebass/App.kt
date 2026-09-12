package com.woong.vibebass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun App() {
    PracticeTheme {
        var videoId by remember { mutableStateOf("") }
        var videoInput by remember { mutableStateOf("") }
        var videoError by remember { mutableStateOf(false) }
        var editingVideo by remember { mutableStateOf(false) }
        var videoRetry by remember { mutableIntStateOf(0) }
        var pdfPath by remember { mutableStateOf("") }
        var title by remember { mutableStateOf("") }
        var artist by remember { mutableStateOf("") }
        var fileName by remember { mutableStateOf("") }
        var currentTime by remember { mutableFloatStateOf(0f) }
        var scrollPosition by remember { mutableFloatStateOf(0f) }
        var isPlaying by remember { mutableStateOf(false) }
        var isSyncMode by remember { mutableStateOf(false) }
        var autoFollow by remember { mutableStateOf(true) }
        var anchors by remember { mutableStateOf<List<AnchorPoint>>(emptyList()) }
        var undoAnchors by remember { mutableStateOf<List<AnchorPoint>?>(null) }
        var songs by remember { mutableStateOf<List<SongData>>(emptyList()) }
        var selectedSongId by remember { mutableStateOf<Long?>(null) }
        var attachingSavedScore by remember { mutableStateOf(false) }
        var loadingSongs by remember { mutableStateOf(true) }
        var libraryError by remember { mutableStateOf(false) }
        var saving by remember { mutableStateOf(false) }
        var dirty by remember { mutableStateOf(false) }
        var status by remember { mutableStateOf("") }
        var libraryTab by remember { mutableStateOf(false) }
        var compactControls by remember { mutableStateOf(false) }
        var pendingChange by remember { mutableStateOf<(() -> Unit)?>(null) }
        val practiceFocus = remember { FocusRequester() }

        fun refreshSongs() {
            loadingSongs = true
            libraryError = false
            SyncDataManager.loadSongs(
                onSuccess = { songs = it; loadingSongs = false },
                onFailure = { loadingSongs = false; libraryError = true }
            )
        }
        fun replaceSession(action: () -> Unit) {
            if (dirty) pendingChange = action else action()
        }
        fun choosePdf() {
            if (attachingSavedScore) triggerPdfUpload() else replaceSession { triggerPdfUpload() }
        }
        fun recordAnchor() {
            if (pdfPath.isEmpty() || videoId.isEmpty() || !isSyncMode) return
            val time = (currentTime * 10).toInt() / 10f
            undoAnchors = anchors
            anchors = (anchors.filterNot { it.timeSec == time } + AnchorPoint(time, scrollPosition)).sortedBy { it.timeSec }
            dirty = true
            status = "${practiceTime(time)} 위치를 기록했습니다."
        }
        fun saveSession() {
            if (saving || anchors.isEmpty() || videoId.isEmpty() || title.isBlank()) return
            val savedAnchors = anchors
            val savedSource = pdfPath
            val savedVideo = videoId
            saving = true
            status = ""
            SyncDataManager.saveSyncData(title.trim(), artist, videoId, savedAnchors,
                onSuccess = {
                    saving = false
                    if (savedSource == pdfPath && savedVideo == videoId && savedAnchors == anchors) dirty = false
                    status = "싱크를 저장했습니다. PDF는 이 기기에서 다시 선택해 주세요."
                    refreshSongs()
                },
                onFailure = {
                    saving = false
                    status = "저장하지 못했습니다. 서버 연결을 확인하고 다시 시도해 주세요."
                }
            )
        }

        LaunchedEffect(Unit) { refreshSongs() }
        LaunchedEffect(currentTime, isSyncMode, autoFollow, pdfPath, anchors) {
            if (pdfPath.isNotEmpty() && !isSyncMode && autoFollow && anchors.isNotEmpty()) {
                SyncDataManager.scrollToPdfPixel(SyncCalculator.calculateScrollPixel(currentTime, anchors).toDouble())
            }
        }

        BoxWithConstraints(Modifier.fillMaxSize().background(PracticeColors.Desk)) {
            val compact = maxWidth < 900.dp
            val tight = maxWidth < 480.dp
            val inactivePane = Modifier.size(0.dp).focusProperties { canFocus = false }.clearAndSetSemantics { }
            val controls: @Composable (Modifier) -> Unit = { modifier ->
                Column(modifier.background(PracticeColors.Surface).clipToBounds()) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                        PaneHeading("연습 컨트롤")
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().height(200.dp).background(PracticeColors.Desk, RoundedCornerShape(8.dp))) {
                            key(videoRetry) {
                                YoutubePlayer(
                                    videoId = videoId, currentTime = currentTime, isPlaying = isPlaying,
                                    onTimeUpdate = { currentTime = it }, onStateChange = { isPlaying = it },
                                    onVideoIdFound = { videoId = it; videoInput = "https://youtu.be/$it"; currentTime = 0f },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            if (videoId.isEmpty()) {
                                Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("함께 연주할 영상", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text("악보를 선택하면 영상을 찾아드려요.", textAlign = TextAlign.Center, color = PracticeColors.Muted)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (videoId.isEmpty() || editingVideo) {
                            OutlinedTextField(
                                value = videoInput, onValueChange = { videoInput = it; videoError = false },
                                label = { Text("YouTube 링크") }, singleLine = true, isError = videoError,
                                supportingText = if (videoError) ({ Text("올바른 YouTube 링크를 입력해 주세요.") }) else null,
                                modifier = Modifier.fillMaxWidth()
                            )
                            TextButton(onClick = {
                                val parsed = youtubeVideoId(videoInput)
                                videoError = parsed == null
                                if (parsed != null) {
                                    videoId = parsed; currentTime = 0f; videoRetry++; editingVideo = false
                                    if (anchors.isNotEmpty()) dirty = true
                                }
                            }, enabled = videoInput.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp)) { Text("영상 연결") }
                        } else {
                            TextButton(onClick = { editingVideo = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("영상 변경 · 다시 연결") }
                        }
                        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(practiceTime(currentTime), fontSize = 32.sp, fontFamily = FontFamily.Monospace, color = PracticeColors.Ink)
                            Text(if (isPlaying) "재생 중" else if (videoId.isEmpty()) "영상 대기" else "일시 정지",
                                style = MaterialTheme.typography.bodySmall, color = PracticeColors.Muted)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ModeButton("연습", !isSyncMode, Modifier.weight(1f)) { isSyncMode = false }
                            ModeButton("싱크 편집", isSyncMode, Modifier.weight(1f)) {
                                isSyncMode = true; libraryTab = false; practiceFocus.requestFocus()
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth()) {
                            WorkspaceTab("싱크 포인트", !libraryTab, Modifier.weight(1f)) { libraryTab = false }
                            WorkspaceTab("보관함", libraryTab, Modifier.weight(1f)) { libraryTab = true }
                        }
                        Spacer(Modifier.height(16.dp))
                        if (libraryTab) {
                            PaneHeading("보관된 연주 목록")
                            SongLibrary(songs, selectedSongId, loadingSongs, libraryError, ::refreshSongs) { song ->
                                replaceSession {
                                    selectedSongId = song.id
                                    videoId = song.youtubeVideoId
                                    videoInput = "https://youtu.be/${song.youtubeVideoId}"
                                    videoError = false
                                    currentTime = 0f
                                    anchors = song.anchorPoints.sortedBy { it.timeSec }
                                    undoAnchors = null
                                    title = song.title
                                    artist = song.artist.orEmpty()
                                    fileName = ""
                                    pdfPath = ""
                                    scrollPosition = 0f
                                    attachingSavedScore = true
                                    dirty = false
                                    compactControls = false
                                    isSyncMode = false
                                    status = "싱크를 불러왔습니다. '${song.title}'의 PDF를 선택해 주세요."
                                }
                            }
                        } else {
                            Text(if (isSyncMode) "악보를 원하는 위치로 옮기고 기록하세요." else "저장한 위치에 맞춰 악보가 따라갑니다.", color = PracticeColors.Muted)
                            Spacer(Modifier.height(12.dp))
                            if (isSyncMode) {
                                StudioButton("현재 위치 기록", ::recordAnchor, enabled = pdfPath.isNotEmpty() && videoId.isNotEmpty(), modifier = Modifier.fillMaxWidth())
                            } else {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("자동 스크롤", style = MaterialTheme.typography.titleMedium)
                                    Switch(checked = autoFollow, onCheckedChange = { autoFollow = it },
                                        modifier = Modifier.semantics { contentDescription = "자동 스크롤" }, enabled = anchors.isNotEmpty())
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            SyncPointList(anchors, isSyncMode) { anchor ->
                                undoAnchors = anchors
                                anchors = anchors.filterNot { it == anchor }
                                dirty = true
                                status = "싱크 포인트를 삭제했습니다."
                            }
                            if (undoAnchors != null) {
                                TextButton(onClick = {
                                    anchors = undoAnchors.orEmpty(); undoAnchors = null; dirty = true
                                    status = "이전 싱크 포인트를 복원했습니다."
                                }, modifier = Modifier.heightIn(min = 48.dp)) { Text("편집 되돌리기") }
                            }
                        }
                    }
                    HorizontalDivider(color = PracticeColors.Divider)
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        StudioButton(if (saving) "저장 중..." else if (selectedSongId != null) "사본 저장" else "싱크 저장", ::saveSession,
                            enabled = !saving && dirty && title.isNotBlank() && videoId.isNotEmpty() && anchors.isNotEmpty(), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text(if (pdfPath.isNotEmpty() && anchors.isEmpty()) "싱크 포인트를 1개 이상 기록해 주세요."
                            else if (selectedSongId != null) "원본을 유지하고 새 항목으로 저장합니다."
                            else if (dirty) "저장하지 않은 변경사항이 있어요." else "싱크와 영상 정보가 보관함에 저장됩니다.",
                            style = MaterialTheme.typography.bodySmall, color = PracticeColors.Muted)
                    }
                }
            }
            val score: @Composable (Modifier) -> Unit = { modifier ->
                Column(modifier.clipToBounds()) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).background(PracticeColors.Surface).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        PaneHeading("악보")
                        Text(if (pdfPath.isEmpty()) "PDF 대기" else if (isSyncMode) "싱크 편집 중" else "연습 모드",
                            style = MaterialTheme.typography.bodySmall, color = PracticeColors.Muted)
                    }
                    HorizontalDivider(color = PracticeColors.Divider)
                    Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                        PdfSheetViewer(pdfSource = pdfPath, onScrollPositionChanged = { scrollPosition = it },
                            onPdfFileSelected = { name, url ->
                                if (!attachingSavedScore) {
                                    title = name.substringBeforeLast('.', name)
                                    artist = ""
                                    videoId = ""
                                    videoInput = ""
                                    videoError = false
                                    currentTime = 0f
                                    anchors = emptyList()
                                    undoAnchors = null
                                    selectedSongId = null
                                    dirty = false
                                    isSyncMode = true
                                }
                                attachingSavedScore = false
                                fileName = name
                                pdfPath = url
                                scrollPosition = 0f
                                compactControls = false
                                status = ""
                            }, modifier = Modifier.fillMaxSize())
                        if (pdfPath.isEmpty()) ScoreEmptyState(attachingSavedScore, ::choosePdf, Modifier.fillMaxSize())
                    }
                    if (compact && isSyncMode && pdfPath.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().background(PracticeColors.Surface).padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(practiceTime(currentTime), fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                            StudioButton("현재 위치 기록", ::recordAnchor, enabled = videoId.isNotEmpty())
                        }
                    }
                }
            }

            Column(Modifier.fillMaxSize().focusRequester(practiceFocus).onKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Spacebar && isSyncMode && pdfPath.isNotEmpty() && videoId.isNotEmpty()) {
                    recordAnchor(); true
                } else false
            }.focusable()) {
                Row(Modifier.fillMaxWidth().height(64.dp).background(PracticeColors.Surface).padding(horizontal = if (tight) 16.dp else 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("VibeBass Studio", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    if (!tight) Text("나의 연습 공간", style = MaterialTheme.typography.bodySmall, color = PracticeColors.Muted)
                }
                HorizontalDivider(color = PracticeColors.Divider)
                Row(Modifier.fillMaxWidth().padding(horizontal = if (tight) 16.dp else 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title.ifEmpty { "오늘의 연습" }, style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
                        Text(if (fileName.isNotEmpty()) fileName else if (attachingSavedScore) "이 곡의 PDF를 연결해 주세요" else "악보를 펼치고, 한 곡에 집중하세요.",
                            style = MaterialTheme.typography.bodySmall, color = PracticeColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    StudioButton("악보 선택", ::choosePdf, primary = false)
                }
                if (compact) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        WorkspaceTab("악보", !compactControls, Modifier.weight(1f)) { compactControls = false }
                        WorkspaceTab("컨트롤 · 보관함", compactControls, Modifier.weight(1f)) { compactControls = true }
                    }
                }
                Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = if (compact) 0.dp else 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 16.dp)) {
                    // Keep callbacks alive when compact navigation hides a pane.
                    controls(if (!compact) Modifier.width(344.dp).fillMaxHeight()
                        else if (compactControls) Modifier.weight(1f).fillMaxHeight() else inactivePane)
                    score(if (!compact || !compactControls) Modifier.weight(1f).fillMaxHeight() else inactivePane)
                }
                if (pendingChange != null) {
                    Column(Modifier.fillMaxWidth().background(PracticeColors.Surface).padding(16.dp)) {
                        Text("저장하지 않은 싱크가 있어요. 새 악보나 곡으로 바꿀까요?")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { pendingChange = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text("취소") }
                            TextButton(onClick = { val action = pendingChange; pendingChange = null; action?.invoke() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("저장하지 않고 바꾸기") }
                        }
                    }
                } else if (status.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().background(PracticeColors.Surface).padding(horizontal = 16.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }, verticalAlignment = Alignment.CenterVertically) {
                        Text(status, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = { status = "" }, modifier = Modifier.heightIn(min = 48.dp)) { Text("닫기") }
                    }
                } else if (!compact) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("PDF와 YouTube로 만드는 나만의 연습", style = MaterialTheme.typography.bodySmall, color = PracticeColors.Muted)
                        Text("${anchors.size}개 싱크 포인트", style = MaterialTheme.typography.bodySmall, color = PracticeColors.Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaneHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

@Composable
private fun StudioButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, primary: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)
    val buttonModifier = modifier.heightIn(min = 48.dp).then(
        if (focused) Modifier.border(2.dp, PracticeColors.Ink, shape).padding(3.dp) else Modifier
    )
    if (primary) {
        Button(onClick, modifier = buttonModifier, enabled = enabled, shape = shape,
            interactionSource = interaction, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) { Text(text, maxLines = 1) }
    } else {
        OutlinedButton(onClick, modifier = buttonModifier, enabled = enabled, shape = shape,
            interactionSource = interaction, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PracticeColors.Ink)) { Text(text, maxLines = 1) }
    }
}

@Composable
private fun ModeButton(text: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    StudioButton(text, onClick, modifier.semantics { selected = active }, primary = active)
}

@Composable
private fun WorkspaceTab(text: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(modifier.semantics { selected = active }
        .then(if (focused) Modifier.border(2.dp, PracticeColors.Ink) else Modifier)
        .clickable(interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge, color = if (active) PracticeColors.Rust else PracticeColors.Muted,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        HorizontalDivider(thickness = if (active) 2.dp else 1.dp, color = if (active) PracticeColors.Rust else PracticeColors.Divider)
    }
}

@Composable
private fun ScoreEmptyState(attach: Boolean, choosePdf: () -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(32.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("PDF", style = MaterialTheme.typography.labelLarge, color = PracticeColors.Rust,
            modifier = Modifier.border(1.dp, PracticeColors.Divider, RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 12.dp))
        Spacer(Modifier.height(24.dp))
        Text(if (attach) "악보를 연결해 주세요" else "악보를 펼쳐볼까요?", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(if (attach) "저장한 싱크는 준비됐어요.\n이 곡의 PDF를 선택하면 이어서 연습할 수 있어요."
            else "PDF 악보를 선택하고\n영상에 맞춰 나만의 싱크를 기록하세요.", color = PracticeColors.Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        StudioButton("악보 선택", choosePdf)
        Spacer(Modifier.height(16.dp))
        Text("PDF 파일은 서버에 업로드되지 않습니다.", style = MaterialTheme.typography.bodySmall, color = PracticeColors.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SyncPointList(anchors: List<AnchorPoint>, editable: Boolean, remove: (AnchorPoint) -> Unit) {
    if (anchors.isEmpty()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
            Text("아직 기록한 위치가 없어요.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("싱크 편집에서 첫 위치를 기록해 보세요.", color = PracticeColors.Muted)
        }
        return
    }
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
        items(anchors) { anchor ->
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(practiceTime(anchor.timeSec), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${anchor.scrollPixel.toInt()} px", style = MaterialTheme.typography.bodySmall, color = PracticeColors.Muted)
                if (editable) TextButton(onClick = { remove(anchor) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("삭제") }
            }
            HorizontalDivider(color = PracticeColors.Divider)
        }
    }
}

@Composable
private fun SongLibrary(songs: List<SongData>, selectedId: Long?, loading: Boolean, error: Boolean, retry: () -> Unit, selectSong: (SongData) -> Unit) {
    when {
        loading -> Text("보관함을 불러오는 중...", modifier = Modifier.padding(vertical = 24.dp), color = PracticeColors.Muted)
        error -> Column(Modifier.padding(vertical = 16.dp)) {
            Text("보관함에 연결하지 못했어요.", color = PracticeColors.Muted)
            TextButton(onClick = retry, modifier = Modifier.heightIn(min = 48.dp)) { Text("다시 시도") }
        }
        songs.isEmpty() -> Column(Modifier.padding(vertical = 24.dp)) {
            Text("첫 연습을 저장해 보세요.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("저장한 곡과 싱크가 여기에 모입니다.", color = PracticeColors.Muted)
        }
        else -> LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp).padding(top = 8.dp)) {
            items(songs, key = { it.id }) { song ->
                val interaction = remember { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                Column(Modifier.fillMaxWidth()
                    .background(if (song.id == selectedId) PracticeColors.Desk else PracticeColors.Surface)
                    .then(if (focused) Modifier.border(2.dp, PracticeColors.Ink) else Modifier)
                    .semantics { selected = song.id == selectedId }
                    .clickable(interactionSource = interaction, indication = null, role = Role.Button) { selectSong(song) }
                    .padding(horizontal = 12.dp, vertical = 16.dp)) {
                    Text(song.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (song.id == selectedId) PracticeColors.Rust else PracticeColors.Ink)
                    Text("${song.artist ?: "아티스트 미상"} · 싱크 ${song.anchorPoints.size}개",
                        style = MaterialTheme.typography.bodySmall, color = PracticeColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

internal fun practiceTime(seconds: Float): String {
    val total = if (seconds.isFinite()) seconds.coerceAtLeast(0f).toInt() else 0
    return "${(total / 60).toString().padStart(2, '0')}:${(total % 60).toString().padStart(2, '0')}"
}

internal fun youtubeVideoId(input: String): String? {
    val value = input.trim()
    if (Regex("[A-Za-z0-9_-]{11}").matches(value)) return value
    return Regex("(?:https?://)?(?:www\\.|m\\.)?(?:youtube\\.com/(?:watch\\?(?:[^#\\s]*&)?v=|shorts/|embed/)|youtu\\.be/)([A-Za-z0-9_-]{11})(?:[?&#/][^\\s]*)?")
        .matchEntire(value)?.groupValues?.get(1)
}
