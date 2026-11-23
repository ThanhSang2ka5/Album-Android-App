package com.example.photomanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.photomanager.PhotoViewModel
import com.example.photomanager.data.AlbumWithCount
import java.io.File

const val SMART_ALBUM_FAVORITES = -2L
const val SMART_ALBUM_RECENT = -3L
const val SMART_ALBUM_UNASSIGNED = -4L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    vm: PhotoViewModel,
    onOpenAlbum: (Long) -> Unit,
    onStartAlbumSlideshow: (List<Long>) -> Unit
) {
    val albums by vm.albums.collectAsState()
    val photos by vm.photos.collectAsState()

    var showCreate by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<AlbumWithCount?>(null) }
    var deleteTarget by remember { mutableStateOf<AlbumWithCount?>(null) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val favCount = remember(photos) { photos.count { it.isFavorite } }
    val sevenDaysAgo = remember { System.currentTimeMillis() - 7L * 86_400_000L }
    val recentCount = remember(photos) { photos.count { it.dateTaken >= sevenDaysAgo } }
    val unassignedCount = remember(photos) { photos.count { it.albumId == null } }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Bộ sưu tập & Album")
                        Text(
                            text = "${albums.size} album cá nhân • ${photos.size} bức ảnh",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                scrollBehavior = scroll
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Album mới") }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "smart_section", span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Bộ sưu tập thông minh",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SmartAlbumChipCard(
                            title = "Yêu thích",
                            count = favCount,
                            icon = Icons.Filled.Favorite,
                            tint = Color(0xFFFF4D6D),
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenAlbum(SMART_ALBUM_FAVORITES) }
                        )
                        SmartAlbumChipCard(
                            title = "7 ngày qua",
                            count = recentCount,
                            icon = Icons.Filled.History,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenAlbum(SMART_ALBUM_RECENT) }
                        )
                        SmartAlbumChipCard(
                            title = "Chưa xếp",
                            count = unassignedCount,
                            icon = Icons.Filled.FolderOff,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenAlbum(SMART_ALBUM_UNASSIGNED) }
                        )
                    }
                    Text(
                        text = "Album cá nhân",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            if (albums.isEmpty()) {
                item(key = "empty_albums", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        icon = Icons.Filled.PhotoAlbum,
                        title = "Chưa có album cá nhân",
                        subtitle = "Tạo album để phân loại ảnh theo chuyến đi, sự kiện, gia đình hoặc tạo bộ dữ liệu mẫu.",
                        actionText = "Tạo album mới",
                        onAction = { showCreate = true },
                        secondaryActionText = "Tạo 4 album & 10 ảnh mẫu Demo",
                        onSecondaryAction = { vm.generateDemoGallery() }
                    )
                }
            } else {
                items(albums, key = { it.id }) { album ->
                    AlbumCard(
                        album = album,
                        imageRevision = vm.imageRevision,
                        onClick = { onOpenAlbum(album.id) },
                        onPlaySlideshow = {
                            val ids = photos.filter { it.albumId == album.id }.map { it.id }
                            if (ids.isNotEmpty()) onStartAlbumSlideshow(ids)
                        },
                        onEdit = { editTarget = album },
                        onDelete = { deleteTarget = album }
                    )
                }
            }
        }
    }

    if (showCreate) {
        AlbumDialog(
            title = "Tạo Album mới",
            confirmText = "Tạo album",
            onDismiss = { showCreate = false },
            onConfirm = { name, desc ->
                vm.createAlbum(name, desc)
                showCreate = false
            }
        )
    }

    editTarget?.let { target ->
        AlbumDialog(
            title = "Chỉnh sửa Album",
            confirmText = "Lưu thay đổi",
            initialName = target.name,
            initialDescription = target.description,
            onDismiss = { editTarget = null },
            onConfirm = { name, desc ->
                vm.updateAlbum(target.id, name, desc)
                editTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "Xóa album \"${target.name}\"?",
            message = "Chỉ xóa thư mục album này. Tất cả ${target.photoCount} bức ảnh bên trong vẫn được giữ an toàn trong Dòng thời gian.",
            confirmText = "Xóa album",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                vm.deleteAlbum(target.id)
                deleteTarget = null
            }
        )
    }
}

@Composable
private fun SmartAlbumChipCard(
    title: String,
    count: Int,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AlbumCard(
    album: AlbumWithCount,
    imageRevision: Int,
    onClick: () -> Unit,
    onPlaySlideshow: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.05f)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            if (album.coverPath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(album.coverPath))
                        .setParameter("rev", imageRevision)
                        .crossfade(true)
                        .build(),
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                            )
                        )
                )
            } else {
                Icon(
                    Icons.Filled.PhotoAlbum,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                )
            }

            // Badge số lượng ảnh ở góc dưới trái của ảnh bìa
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = "${album.photoCount} ảnh",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // Nút phát nhanh Slideshow ở góc dưới phải của ảnh bìa
            if (album.photoCount > 0) {
                IconButton(
                    onClick = onPlaySlideshow,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Trình chiếu album",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.38f))
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Tùy chọn", tint = Color.White)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (album.photoCount > 0) {
                        DropdownMenuItem(
                            text = { Text("Trình chiếu album") },
                            leadingIcon = { Icon(Icons.Filled.Slideshow, null) },
                            onClick = { menuOpen = false; onPlaySlideshow() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Đổi tên & mô tả") },
                        leadingIcon = { Icon(Icons.Filled.Edit, null) },
                        onClick = { menuOpen = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Xóa album") },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                album.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = album.description.ifBlank { "Chạm để xem & quản lý ảnh trong album" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
