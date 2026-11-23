package com.example.photomanager.ui

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.photomanager.PhotoViewModel
import com.example.photomanager.SlideEffect
import com.example.photomanager.SlideshowConfig
import com.example.photomanager.data.BuiltInTrack
import com.example.photomanager.data.Photo
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class SlideSource(val label: String) {
    SELECTED("Ảnh tự chọn"),
    ALBUM("Theo Album"),
    DATE_RANGE("Theo thời gian"),
    ALL("Tất cả ảnh"),
    FAVORITES("Yêu thích ❤️")
}

private enum class SlideOrder(val label: String) {
    OLDEST_FIRST("Cũ → Mới"),
    NEWEST_FIRST("Mới → Cũ"),
    SHUFFLE("Ngẫu nhiên 🔀")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlideshowSetupScreen(vm: PhotoViewModel, onStart: () -> Unit) {
    val context = LocalContext.current
    val photos by vm.photos.collectAsState()
    val albums by vm.albums.collectAsState()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val preselected = vm.slideshowPreselect
    var source by rememberSaveable {
        mutableStateOf(if (preselected.isNotEmpty()) SlideSource.SELECTED else SlideSource.ALL)
    }
    var customSelectedIds by rememberSaveable {
        mutableStateOf(preselected)
    }
    var showPhotoSelectorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(preselected) {
        if (preselected.isNotEmpty()) {
            customSelectedIds = preselected
            source = SlideSource.SELECTED
        }
    }

    var albumId by rememberSaveable { mutableStateOf<Long?>(null) }
    var albumMenu by remember { mutableStateOf(false) }
    var fromDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var toDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var pickingFrom by remember { mutableStateOf(false) }
    var pickingTo by remember { mutableStateOf(false) }

    var slideOrder by rememberSaveable { mutableStateOf(SlideOrder.OLDEST_FIRST) }
    var seconds by rememberSaveable { mutableFloatStateOf(3f) }
    var effect by rememberSaveable { mutableStateOf(SlideEffect.KEN_BURNS) }
    var loopSlideshow by rememberSaveable { mutableStateOf(true) }
    var showCaptions by rememberSaveable { mutableStateOf(true) }

    // Lựa chọn nhạc nền được lưu bền vững trong PhotoViewModel & bộ nhớ ứng dụng
    val customTracks = vm.customTracks
    val selectedBuiltIn = vm.selectedBuiltInTrack
    val selectedCustomId = vm.selectedCustomTrackId
    val musicMuted = vm.musicMuted
    val selectedCustomTrack = remember(customTracks, selectedCustomId) {
        customTracks.firstOrNull { it.id == selectedCustomId }
    }

    // Trình phát nghe thử nhạc nền
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    fun stopPreview() {
        previewPlayer?.runCatching { stop() }
        previewPlayer?.release()
        previewPlayer = null
        isPreviewPlaying = false
    }

    DisposableEffect(Unit) {
        onDispose { stopPreview() }
    }

    val activeMusicUri: Uri? = remember(musicMuted, selectedCustomTrack, selectedBuiltIn) {
        when {
            musicMuted -> null
            selectedCustomTrack != null -> Uri.fromFile(File(selectedCustomTrack.path))
            selectedBuiltIn != null -> vm.getBuiltInTrackUri(selectedBuiltIn)
            else -> null
        }
    }

    val activeMusicTitle: String? = remember(musicMuted, selectedCustomTrack, selectedBuiltIn) {
        when {
            musicMuted -> null
            selectedCustomTrack != null -> selectedCustomTrack.title
            selectedBuiltIn != null -> selectedBuiltIn.title
            else -> null
        }
    }

    fun togglePreview() {
        if (isPreviewPlaying) {
            stopPreview()
            return
        }
        val uri = activeMusicUri ?: return
        runCatching {
            val mp = MediaPlayer().apply {
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
            previewPlayer = mp
            isPreviewPlaying = true
        }
    }

    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            stopPreview()
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            vm.importCustomMusic(uri)
        }
    }

    // Tự động gán album đầu tiên nếu người dùng chuyển sang chế độ Theo Album
    LaunchedEffect(source, albums) {
        if (source == SlideSource.ALBUM && albumId == null && albums.isNotEmpty()) {
            albumId = albums.first().id
        }
    }

    val chosen = remember(photos, source, albumId, fromDate, toDate, customSelectedIds, slideOrder) {
        val base = when (source) {
            SlideSource.ALL -> photos
            SlideSource.FAVORITES -> photos.filter { it.isFavorite }
            SlideSource.ALBUM -> photos.filter { it.albumId == albumId }
            SlideSource.DATE_RANGE -> photos.filter { p ->
                val day = localDateOf(p.dateTaken)
                val from = fromDate?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                val to = toDate?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                (from == null || !day.isBefore(from)) && (to == null || !day.isAfter(to))
            }
            SlideSource.SELECTED -> {
                val idSet = customSelectedIds.toSet()
                photos.filter { it.id in idSet }
            }
        }
        when (slideOrder) {
            SlideOrder.OLDEST_FIRST -> base.sortedBy { it.dateTaken }
            SlideOrder.NEWEST_FIRST -> base.sortedByDescending { it.dateTaken }
            SlideOrder.SHUFFLE -> base.shuffled()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Phòng dựng Trình chiếu")
                        Text(
                            text = "${chosen.size} ảnh sẵn sàng • ${activeMusicTitle ?: "Không nhạc nền"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                scrollBehavior = scroll
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // BƯỚC 1: CHỌN NGUỒN ẢNH
            Section("1. Chọn nguồn ảnh trình chiếu") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SlideSource.entries.forEach { s ->
                        FilterChip(
                            selected = source == s,
                            onClick = { source = s },
                            label = { Text(s.label) }
                        )
                    }
                }

                when (source) {
                    SlideSource.SELECTED -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(onClick = { showPhotoSelectorDialog = true }) {
                                Icon(Icons.Filled.Checklist, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Chọn từng ảnh (${customSelectedIds.size} đã chọn)")
                            }
                            if (customSelectedIds.isNotEmpty()) {
                                TextButton(onClick = {
                                    customSelectedIds = emptyList()
                                    vm.slideshowPreselect = emptyList()
                                }) {
                                    Text("Bỏ chọn hết")
                                }
                            }
                        }
                    }

                    SlideSource.ALBUM -> {
                        if (albums.isEmpty()) {
                            Text(
                                "Chưa có album nào. Hãy tạo album ở tab Album trước.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val selectedAlbum = albums.firstOrNull { it.id == albumId }
                            Box {
                                OutlinedButton(onClick = { albumMenu = true }) {
                                    Text(
                                        selectedAlbum?.let { "${it.name} (${it.photoCount} ảnh)" }
                                            ?: "Chọn album trình chiếu"
                                    )
                                }
                                DropdownMenu(expanded = albumMenu, onDismissRequest = { albumMenu = false }) {
                                    albums.forEach { album ->
                                        DropdownMenuItem(
                                            text = { Text("${album.name} (${album.photoCount} ảnh)") },
                                            onClick = {
                                                albumId = album.id
                                                albumMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SlideSource.DATE_RANGE -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { pickingFrom = true }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.DateRange, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Từ: " + (fromDate?.let { utcDateText(it) } ?: "Đầu tiên"))
                                }
                                OutlinedButton(onClick = { pickingTo = true }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.DateRange, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Đến: " + (toDate?.let { utcDateText(it) } ?: "Hôm nay"))
                                }
                            }
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AssistChip(
                                    onClick = {
                                        val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                                        fromDate = todayUtc
                                        toDate = todayUtc
                                    },
                                    label = { Text("Hôm nay") }
                                )
                                AssistChip(
                                    onClick = {
                                        val nowLd = LocalDate.now()
                                        fromDate = nowLd.minusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                                        toDate = nowLd.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                                    },
                                    label = { Text("7 ngày qua") }
                                )
                                AssistChip(
                                    onClick = {
                                        val nowLd = LocalDate.now()
                                        fromDate = nowLd.minusDays(30).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                                        toDate = nowLd.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                                    },
                                    label = { Text("30 ngày qua") }
                                )
                                if (fromDate != null || toDate != null) {
                                    AssistChip(
                                        onClick = { fromDate = null; toDate = null },
                                        label = { Text("Xóa bộ lọc ngày") }
                                    )
                                }
                            }
                        }
                    }

                    else -> {}
                }

                // Dải xem trước (Filmstrip)
                if (chosen.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Xem trước dải ${chosen.size} ảnh:",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f)
                        )
                        SlideOrder.entries.forEach { order ->
                            FilterChip(
                                selected = slideOrder == order,
                                onClick = { slideOrder = order },
                                label = { Text(order.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(chosen, key = { _, p -> p.id }) { index, photo ->
                            Box(
                                modifier = Modifier
                                    .size(74.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(File(photo.path))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = photo.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "Chưa có bức ảnh nào khớp với bộ lọc hiện tại.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // BƯỚC 2: HIỆU ỨNG & TỐC ĐỘ
            Section("2. Hiệu ứng chuyển cảnh & Tốc độ") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SlideEffect.entries.forEach { e ->
                            FilterChip(
                                selected = effect == e,
                                onClick = { effect = e },
                                label = { Text(e.label) }
                            )
                        }
                    }
                    Text(
                        text = "Mô tả hiệu ứng: ${effect.subtitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    "Thời gian mỗi bức ảnh: ${seconds.toInt()} giây (Tổng ~${chosen.size * seconds.toInt()} giây)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Slider(value = seconds, onValueChange = { seconds = it }, valueRange = 2f..10f, steps = 7)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hiển thị tiêu đề, địa điểm & ngày chụp", modifier = Modifier.weight(1f))
                    Switch(checked = showCaptions, onCheckedChange = { showCaptions = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tự động lặp lại khi chiếu hết ảnh", modifier = Modifier.weight(1f))
                    Switch(checked = loopSlideshow, onCheckedChange = { loopSlideshow = it })
                }
            }

            // BƯỚC 3: NHẠC NỀN (TÍCH HỢP SẴN + CHỌN FILE TỪ MÁY)
            Section("3. Nhạc nền trình chiếu (Background Music)") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Chọn bản nhạc có sẵn hoặc tải từ máy:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (!musicMuted && activeMusicUri != null) {
                        FilledTonalButton(
                            onClick = { togglePreview() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                if (isPreviewPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isPreviewPlaying) "Dừng thử" else "Nghe thử")
                        }
                    }
                }

                // Danh sách 3 bản nhạc nền tích hợp sẵn
                BuiltInTrack.entries.forEach { track ->
                    val isSelected = !musicMuted && selectedCustomId == null && selectedBuiltIn == track
                    MusicOptionRow(
                        title = track.title,
                        subtitle = track.subtitle,
                        selected = isSelected,
                        onClick = {
                            stopPreview()
                            vm.selectBuiltInMusic(track)
                        }
                    )
                }

                // Danh sách các bài nhạc người dùng đã thêm từ thiết bị (đã lưu bền vững)
                customTracks.forEach { track ->
                    val isSelected = !musicMuted && selectedCustomId == track.id
                    MusicOptionRow(
                        title = track.title,
                        subtitle = "Nhạc đã lưu từ thiết bị",
                        selected = isSelected,
                        onClick = {
                            stopPreview()
                            vm.selectCustomMusic(track.id)
                        },
                        onDelete = {
                            stopPreview()
                            vm.deleteCustomTrack(track.id)
                        }
                    )
                }

                // Tùy chọn Không phát nhạc
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            stopPreview()
                            vm.muteMusic()
                        }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = musicMuted,
                        onClick = {
                            stopPreview()
                            vm.muteMusic()
                        }
                    )
                    Icon(
                        Icons.Filled.MusicOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Tắt nhạc nền (Chỉ trình chiếu hình ảnh)")
                }

                OutlinedButton(
                    onClick = { musicPicker.launch(arrayOf("audio/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.AudioFile, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Thêm file nhạc MP3 / WAV / M4A từ máy...")
                }
            }

            Button(
                onClick = {
                    stopPreview()
                    vm.slideshow = SlideshowConfig(
                        photos = chosen,
                        intervalMs = (seconds * 1000).toLong(),
                        effect = effect,
                        musicUri = activeMusicUri,
                        musicTitle = activeMusicTitle,
                        loopSlideshow = loopSlideshow,
                        showCaptions = showCaptions
                    )
                    onStart()
                },
                enabled = chosen.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Bắt đầu Trình chiếu (${chosen.size} ảnh)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPhotoSelectorDialog) {
        PhotoSelectorDialog(
            allPhotos = photos,
            initialSelected = customSelectedIds.toSet(),
            onDismiss = { showPhotoSelectorDialog = false },
            onConfirm = { newIds ->
                customSelectedIds = newIds.toList()
                vm.slideshowPreselect = customSelectedIds
                showPhotoSelectorDialog = false
            }
        )
    }

    if (pickingFrom) {
        DateDialog(
            initial = fromDate,
            onDismiss = { pickingFrom = false },
            onPick = { fromDate = it; pickingFrom = false }
        )
    }
    if (pickingTo) {
        DateDialog(
            initial = toDate,
            onDismiss = { pickingTo = false },
            onPick = { toDate = it; pickingTo = false }
        )
    }
}

@Composable
private fun MusicOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Xóa bài nhạc",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PhotoSelectorDialog(
    allPhotos: List<Photo>,
    initialSelected: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit
) {
    val context = LocalContext.current
    var tempSelected by remember { mutableStateOf(initialSelected) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Chọn ảnh trình chiếu (${tempSelected.size})",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    tempSelected = if (tempSelected.size == allPhotos.size) {
                        emptySet()
                    } else {
                        allPhotos.map { it.id }.toSet()
                    }
                }) {
                    Text(if (tempSelected.size == allPhotos.size) "Bỏ hết" else "Tất cả")
                }
            }
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(allPhotos, key = { it.id }) { photo ->
                    val isChecked = photo.id in tempSelected
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isChecked) 3.dp else 0.dp,
                                color = if (isChecked) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                tempSelected = if (isChecked) tempSelected - photo.id else tempSelected + photo.id
                            }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(File(photo.path)).build(),
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Icon(
                            imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isChecked) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(22.dp)
                                .background(
                                    if (isChecked) Color.White else Color.Black.copy(alpha = 0.35f),
                                    CircleShape
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tempSelected) }) {
                Text("Xác nhận (${tempSelected.size} ảnh)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

private fun utcDateText(millis: Long): String =
    formatDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(initial: Long?, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let(onPick) },
                enabled = state.selectedDateMillis != null
            ) { Text("Chọn") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    ) {
        DatePicker(state = state)
    }
}
