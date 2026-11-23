package com.example.photomanager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.photomanager.PhotoViewModel
import com.example.photomanager.data.Photo
import java.io.File
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(
    vm: PhotoViewModel,
    photos: List<Photo>,
    startPhotoId: Long,
    onBack: () -> Unit,
    onDelete: (Photo) -> Unit
) {
    if (photos.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val albums by vm.albums.collectAsState()
    val startIndex = remember { photos.indexOfFirst { it.id == startPhotoId }.coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = startIndex) { photos.size }

    var chromeVisible by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAlbumPicker by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val current = photos.getOrNull(pagerState.currentPage) ?: photos.last()
    val albumName = albums.firstOrNull { it.id == current.albumId }?.name ?: "Chưa phân loại"

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            userScrollEnabled = !zoomed,
            key = { photos[it].id }
        ) { page ->
            ZoomableImage(
                photo = photos[page],
                imageRevision = vm.imageRevision,
                isCurrent = page == pagerState.currentPage,
                onTap = { chromeVisible = !chromeVisible },
                onZoomChanged = { zoomed = it }
            )
        }

        // Thanh tiêu đề phía trên
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(0.75f), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                }
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(
                        text = current.title.ifBlank { formatDateTime(current.dateTaken) },
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatDateTime(current.dateTaken)} • ${pagerState.currentPage + 1}/${photos.size}",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = { vm.rotatePhoto90(current) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.RotateRight,
                        contentDescription = "Xoay ảnh 90°",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { showAlbumPicker = true }) {
                    Icon(
                        Icons.Filled.PhotoAlbum,
                        contentDescription = "Chuyển vào Album",
                        tint = Color.White
                    )
                }
            }
        }

        // Thanh công cụ & Chú thích phía dưới
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.85f))))
                    .navigationBarsPadding()
            ) {
                if (current.caption.isNotBlank() || current.locationTag.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (current.locationTag.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "📍 ${current.locationTag}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (current.caption.isNotBlank()) {
                            Text(
                                text = current.caption,
                                color = Color.White.copy(alpha = 0.92f),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val tint = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    IconButton(onClick = { vm.toggleFavorite(current) }, colors = tint) {
                        Icon(
                            imageVector = if (current.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Yêu thích",
                            tint = if (current.isFavorite) Color(0xFFFF4D6D) else Color.White
                        )
                    }
                    IconButton(onClick = { showShareSheet = true }, colors = tint) {
                        Icon(Icons.Filled.Share, contentDescription = "Chia sẻ mạng xã hội")
                    }
                    IconButton(onClick = { showEditDialog = true }, colors = tint) {
                        Icon(Icons.Filled.Edit, contentDescription = "Sửa ghi chú")
                    }
                    IconButton(onClick = { showInfo = true }, colors = tint) {
                        Icon(Icons.Filled.Info, contentDescription = "Thông tin EXIF")
                    }
                    IconButton(onClick = { confirmDelete = true }, colors = tint) {
                        Icon(Icons.Filled.Delete, contentDescription = "Xóa")
                    }
                }
            }
        }
    }

    if (showShareSheet) {
        SocialShareBottomSheet(
            photos = listOf(current),
            onDismiss = { showShareSheet = false }
        )
    }

    if (showAlbumPicker) {
        AlbumPickerDialog(
            albums = albums,
            onPick = { albumId ->
                vm.moveToAlbum(listOf(current.id), albumId)
                showAlbumPicker = false
            },
            onCreate = { name, desc ->
                vm.createAlbum(name, desc, listOf(current.id))
                showAlbumPicker = false
            },
            onDismiss = { showAlbumPicker = false }
        )
    }

    if (showEditDialog) {
        var editTitle by remember(current) { mutableStateOf(current.title) }
        var editCaption by remember(current) { mutableStateOf(current.caption) }
        var editLocation by remember(current) { mutableStateOf(current.locationTag) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Chỉnh sửa thông tin ảnh", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Tiêu đề bức ảnh") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        label = { Text("Địa điểm (Ví dụ: Đà Lạt, Hà Nội...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCaption,
                        onValueChange = { editCaption = it },
                        label = { Text("Ghi chú / Lời tựa kỷ niệm") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.updatePhotoDetails(
                        id = current.id,
                        title = editTitle,
                        caption = editCaption,
                        locationTag = editLocation,
                        dateTaken = current.dateTaken,
                        albumId = current.albumId
                    )
                    showEditDialog = false
                }) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Hủy") }
            }
        )
    }

    if (showInfo) {
        val file = File(current.path)
        val mp = if (current.width > 0 && current.height > 0) {
            "%.1f MP (%d × %d px)".format((current.width * current.height) / 1_000_000.0, current.width, current.height)
        } else {
            "Tự động"
        }
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Chi tiết & Thông số EXIF", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Tiêu đề: ${current.title.ifBlank { "Không có" }}")
                    Text("• Thời gian chụp: ${formatDateTime(current.dateTaken)}")
                    Text("• Album: $albumName")
                    if (current.locationTag.isNotBlank()) {
                        Text("• Địa điểm: ${current.locationTag}")
                    }
                    Text("• Độ phân giải: $mp")
                    Text("• Dung lượng: ${formatFileSize(file.length())}")
                    Text("• Tên tệp: ${file.name}")
                    if (current.caption.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("• Ghi chú: ${current.caption}")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Đóng") } }
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Xóa bức ảnh này?",
            message = "Ảnh sẽ bị xóa khỏi bộ nhớ của ứng dụng và không thể khôi phục.",
            confirmText = "Xóa",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onDelete(current)
            }
        )
    }
}

@Composable
private fun ZoomableImage(
    photo: Photo,
    imageRevision: Int,
    isCurrent: Boolean,
    onTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(isCurrent) {
        if (!isCurrent) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                        onZoomChanged(scale > 1f)
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val multiTouch = event.changes.size > 1
                        if (multiTouch || scale > 1f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val newScale = (scale * zoom).coerceIn(1f, 6f)
                            val maxX = size.width * (newScale - 1f) / 2f
                            val maxY = size.height * (newScale - 1f) / 2f
                            scale = newScale
                            offset = if (newScale <= 1f) {
                                Offset.Zero
                            } else {
                                Offset(
                                    (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    (offset.y + pan.y).coerceIn(-maxY, maxY)
                                )
                            }
                            onZoomChanged(scale > 1.01f)
                            if (abs(zoom - 1f) > 0.001f || scale > 1f) {
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(File(photo.path))
                .setParameter("rev", imageRevision)
                .crossfade(true)
                .build(),
            contentDescription = photo.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}
