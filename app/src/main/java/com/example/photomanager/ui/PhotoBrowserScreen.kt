package com.example.photomanager.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.photomanager.PhotoViewModel
import com.example.photomanager.data.Photo

private enum class QuickFilter(val label: String) {
    ALL("Tất cả"),
    FAVORITES("Yêu thích ❤️"),
    WITH_LOCATION("Có địa điểm 📍"),
    WITH_CAPTION("Có ghi chú 📝")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoBrowserScreen(
    vm: PhotoViewModel,
    title: String,
    subtitle: String? = null,
    photos: List<Photo>,
    albumId: Long?,
    showDashboard: Boolean = false,
    onOpenViewer: (Photo) -> Unit,
    onSlideshowWith: (List<Long>) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val albums by vm.albums.collectAsState()

    var selected by remember { mutableStateOf(setOf<Long>()) }
    var columns by rememberSaveable { mutableIntStateOf(3) }
    var newestFirst by rememberSaveable { mutableStateOf(true) }
    var groupMode by rememberSaveable { mutableStateOf(TimelineGroupMode.BY_DAY) }
    var quickFilter by rememberSaveable { mutableStateOf(QuickFilter.ALL) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchVisible by rememberSaveable { mutableStateOf(false) }

    var showAlbumPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var shareTargets by remember { mutableStateOf<List<Photo>?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    val selecting = selected.isNotEmpty()

    val filteredPhotos = remember(photos, newestFirst, quickFilter, searchQuery) {
        val base = photos.filter { p ->
            val matchFilter = when (quickFilter) {
                QuickFilter.ALL -> true
                QuickFilter.FAVORITES -> p.isFavorite
                QuickFilter.WITH_LOCATION -> p.locationTag.isNotBlank()
                QuickFilter.WITH_CAPTION -> p.caption.isNotBlank()
            }
            val q = searchQuery.trim()
            val matchQuery = if (q.isEmpty()) {
                true
            } else {
                p.title.contains(q, ignoreCase = true) ||
                    p.caption.contains(q, ignoreCase = true) ||
                    p.locationTag.contains(q, ignoreCase = true) ||
                    dayLabel(p.dateTaken).contains(q, ignoreCase = true)
            }
            matchFilter && matchQuery
        }
        if (newestFirst) base else base.asReversed()
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> -> vm.addPhotos(uris, albumId) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean -> vm.onCameraResult(success, albumId) }

    fun launchPicker() {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun launchCamera() {
        val uri = vm.createCameraCaptureUri()
        if (uri != null) {
            cameraLauncher.launch(uri)
        }
    }

    fun toggle(photo: Photo) {
        selected = if (photo.id in selected) selected - photo.id else selected + photo.id
    }

    fun toggleGroup(groupPhotos: List<Photo>) {
        val ids = groupPhotos.map { it.id }.toSet()
        selected = if (ids.all { it in selected }) selected - ids else selected + ids
    }

    BackHandler(enabled = selecting) { selected = emptySet() }

    val largeScroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(largeScroll.nestedScrollConnection),
        topBar = {
            if (selecting) {
                TopAppBar(
                    title = { Text("${selected.size} đã chọn", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { selected = emptySet() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Bỏ chọn")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val allIds = filteredPhotos.map { it.id }.toSet()
                            selected = if (selected.size == allIds.size) emptySet() else allIds
                        }) {
                            Icon(Icons.Filled.SelectAll, contentDescription = "Chọn tất cả")
                        }
                        IconButton(onClick = {
                            vm.setFavorites(selected, true)
                            selected = emptySet()
                        }) {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = "Yêu thích",
                                tint = Color(0xFFFF4D6D)
                            )
                        }
                        IconButton(onClick = {
                            shareTargets = photos.filter { it.id in selected }
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = "Chia sẻ mạng xã hội")
                        }
                        IconButton(onClick = {
                            onSlideshowWith(photos.filter { it.id in selected }.map { it.id })
                            selected = emptySet()
                        }) {
                            Icon(Icons.Filled.Slideshow, contentDescription = "Trình chiếu")
                        }
                        IconButton(onClick = { showAlbumPicker = true }) {
                            Icon(Icons.Filled.PhotoAlbum, contentDescription = "Thêm vào album")
                        }
                        if (albumId != null && albumId > 0) {
                            IconButton(onClick = {
                                vm.moveToAlbum(selected.toList(), null)
                                selected = emptySet()
                            }) {
                                Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "Gỡ khỏi album")
                            }
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Xóa")
                        }
                    }
                )
            } else {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (!subtitle.isNullOrBlank()) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            searchVisible = !searchVisible
                            if (!searchVisible) searchQuery = ""
                        }) {
                            Icon(Icons.Filled.Search, contentDescription = "Tìm kiếm")
                        }
                        IconButton(onClick = { newestFirst = !newestFirst }) {
                            Icon(Icons.Filled.SwapVert, contentDescription = "Đổi thứ tự sắp xếp")
                        }
                        IconButton(onClick = { columns = if (columns >= 4) 2 else columns + 1 }) {
                            Icon(Icons.Filled.GridView, contentDescription = "Đổi số cột")
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Tùy chọn thêm")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Trình chiếu danh sách này") },
                                    leadingIcon = { Icon(Icons.Filled.Slideshow, null) },
                                    onClick = {
                                        menuExpanded = false
                                        if (filteredPhotos.isNotEmpty()) {
                                            onSlideshowWith(filteredPhotos.map { it.id })
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Chia sẻ tất cả ảnh đang lọc") },
                                    leadingIcon = { Icon(Icons.Filled.Share, null) },
                                    onClick = {
                                        menuExpanded = false
                                        if (filteredPhotos.isNotEmpty()) {
                                            shareTargets = filteredPhotos
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Tạo bộ ảnh mẫu Demo") },
                                    leadingIcon = { Icon(Icons.Filled.AutoAwesome, null) },
                                    onClick = {
                                        menuExpanded = false
                                        vm.generateDemoGallery()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Giao diện: ${vm.themeMode.label}") },
                                    leadingIcon = { Icon(Icons.Filled.DarkMode, null) },
                                    onClick = {
                                        menuExpanded = false
                                        vm.cycleThemeMode()
                                    }
                                )
                            }
                        }
                    },
                    scrollBehavior = largeScroll
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(visible = !selecting, enter = fadeIn(), exit = fadeOut()) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { launchCamera() },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Chụp ảnh mới")
                    }
                    ExtendedFloatingActionButton(
                        onClick = { launchPicker() },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("Thêm ảnh") }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (vm.isImporting) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            when {
                !vm.photosLoaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                photos.isEmpty() -> EmptyState(
                    icon = Icons.Filled.PhotoLibrary,
                    title = if (albumId == null) "Thư viện chưa có ảnh nào" else "Album này đang trống",
                    subtitle = "Chọn ảnh từ máy, chụp ảnh mới bằng Camera hoặc tạo bộ ảnh mẫu nghệ thuật chỉ với 1 chạm.",
                    actionText = "Chọn ảnh từ máy",
                    onAction = { launchPicker() },
                    secondaryActionText = "Tạo 10 ảnh & 4 album mẫu Demo",
                    onSecondaryAction = { vm.generateDemoGallery() }
                )
                else -> PhotoGrid(
                    photos = filteredPhotos,
                    groupMode = groupMode,
                    columns = columns,
                    selected = selected,
                    imageRevision = vm.imageRevision,
                    onClick = { photo -> if (selecting) toggle(photo) else onOpenViewer(photo) },
                    onLongClick = { photo ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        toggle(photo)
                    },
                    onToggleGroup = { groupList -> toggleGroup(groupList) },
                    headerContent = {
                        BrowserHeaderSection(
                            showDashboard = showDashboard,
                            totalPhotos = photos.size,
                            favoriteCount = photos.count { it.isFavorite },
                            albumCount = albums.size,
                            totalBytes = photos.sumOf { it.sizeBytes },
                            searchVisible = searchVisible,
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            quickFilter = quickFilter,
                            onQuickFilterChange = { quickFilter = it },
                            groupMode = groupMode,
                            onGroupModeChange = {
                                groupMode = when (groupMode) {
                                    TimelineGroupMode.BY_DAY -> TimelineGroupMode.BY_MONTH
                                    TimelineGroupMode.BY_MONTH -> TimelineGroupMode.NONE
                                    TimelineGroupMode.NONE -> TimelineGroupMode.BY_DAY
                                }
                            },
                            onStartQuickSlideshow = {
                                if (filteredPhotos.isNotEmpty()) {
                                    onSlideshowWith(filteredPhotos.map { it.id })
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 128.dp)
                )
            }
        }
    }

    if (showAlbumPicker) {
        AlbumPickerDialog(
            albums = albums,
            onPick = { id ->
                vm.moveToAlbum(selected.toList(), id)
                selected = emptySet()
                showAlbumPicker = false
            },
            onCreate = { name, desc ->
                vm.createAlbum(name, desc, selected.toList())
                selected = emptySet()
                showAlbumPicker = false
            },
            onDismiss = { showAlbumPicker = false }
        )
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Xóa ${selected.size} ảnh đã chọn?",
            message = "Các bức ảnh này sẽ bị xóa khỏi bộ nhớ ứng dụng và không thể hoàn tác.",
            confirmText = "Xóa vĩnh viễn",
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                vm.deletePhotos(selected)
                selected = emptySet()
                showDeleteConfirm = false
            }
        )
    }

    shareTargets?.let { targets ->
        SocialShareBottomSheet(
            photos = targets,
            onDismiss = {
                shareTargets = null
                selected = emptySet()
            }
        )
    }
}

@Composable
private fun BrowserHeaderSection(
    showDashboard: Boolean,
    totalPhotos: Int,
    favoriteCount: Int,
    albumCount: Int,
    totalBytes: Long,
    searchVisible: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    quickFilter: QuickFilter,
    onQuickFilterChange: (QuickFilter) -> Unit,
    groupMode: TimelineGroupMode,
    onGroupModeChange: () -> Unit,
    onStartQuickSlideshow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showDashboard) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Kho lưu trữ kỷ niệm của bạn",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "$totalPhotos bức ảnh • ${formatFileSize(totalBytes)}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            FilledTonalButton(
                                onClick = onStartQuickSlideshow,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Chiếu ngay", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatPill("Tổng ảnh", "$totalPhotos", Modifier.weight(1f))
                            StatPill("Yêu thích ❤️", "$favoriteCount", Modifier.weight(1f))
                            StatPill("Album", "$albumCount", Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = searchVisible || searchQuery.isNotBlank()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Tìm theo tên ảnh, ghi chú, địa điểm, ngày...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Xóa tìm kiếm")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Hàng bộ lọc nhanh và nút chuyển chế độ nhóm dòng thời gian
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = true,
                onClick = onGroupModeChange,
                label = { Text(groupMode.label) },
                leadingIcon = {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )
            QuickFilter.entries.forEach { filter ->
                FilterChip(
                    selected = quickFilter == filter,
                    onClick = { onQuickFilterChange(filter) },
                    label = { Text(filter.label) }
                )
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(vertical = 8.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
