package com.example.photomanager.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.photomanager.data.AlbumWithCount
import com.example.photomanager.data.Photo
import java.io.File

enum class TimelineGroupMode(val label: String) {
    BY_DAY("Theo ngày"),
    BY_MONTH("Theo tháng"),
    NONE("Lưới liền mạch")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoGrid(
    photos: List<Photo>,
    groupMode: TimelineGroupMode,
    columns: Int,
    selected: Set<Long>,
    imageRevision: Int = 0,
    onClick: (Photo) -> Unit,
    onLongClick: (Photo) -> Unit,
    onToggleGroup: ((List<Photo>) -> Unit)? = null,
    headerContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val selecting = selected.isNotEmpty()
    val groups = remember(photos, groupMode) {
        when (groupMode) {
            TimelineGroupMode.BY_DAY -> photos.groupBy { dayLabel(it.dateTaken) }
            TimelineGroupMode.BY_MONTH -> photos.groupBy { monthLabel(it.dateTaken) }
            TimelineGroupMode.NONE -> mapOf("" to photos)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (headerContent != null) {
            item(key = "top_header_banner", span = { GridItemSpan(maxLineSpan) }) {
                headerContent()
            }
        }

        groups.forEach { (label, list) ->
            if (groupMode != TimelineGroupMode.NONE && label.isNotEmpty()) {
                item(key = "header_$label", span = { GridItemSpan(maxLineSpan) }) {
                    val allInGroupSelected = list.isNotEmpty() && list.all { it.id in selected }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 8.dp, top = 16.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${list.size} ảnh",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        if (onToggleGroup != null) {
                            IconButton(
                                onClick = { onToggleGroup(list) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (allInGroupSelected) {
                                        Icons.Filled.CheckCircle
                                    } else {
                                        Icons.Outlined.CheckCircleOutline
                                    },
                                    contentDescription = "Chọn cả nhóm",
                                    tint = if (allInGroupSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            items(list, key = { it.id }) { photo ->
                PhotoCell(
                    photo = photo,
                    columns = columns,
                    imageRevision = imageRevision,
                    selecting = selecting,
                    isSelected = photo.id in selected,
                    onClick = { onClick(photo) },
                    onLongClick = { onLongClick(photo) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoCell(
    photo: Photo,
    columns: Int,
    imageRevision: Int,
    selecting: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val inset by animateDpAsState(if (isSelected) 10.dp else 0.dp, label = "inset")
    val corner by animateDpAsState(if (isSelected) 14.dp else 4.dp, label = "corner")
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(File(photo.path))
                .setParameter("rev", imageRevision)
                .crossfade(true)
                .build(),
            contentDescription = photo.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(inset)
                .clip(RoundedCornerShape(corner))
        )

        // Biểu tượng Yêu thích ❤️ góc trên bên phải
        if (photo.isFavorite) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Yêu thích",
                    tint = Color(0xFFFF4D6D),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Hiển thị tiêu đề ở chế độ lưới 2 cột lớn
        if (columns == 2 && photo.title.isNotBlank() && !isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = photo.title,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (selecting) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(24.dp)
                    .background(if (isSelected) Color.White else Color.Black.copy(alpha = 0.25f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryActionText: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAction) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(actionText)
            }
        }
        if (secondaryActionText != null && onSecondaryAction != null) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onSecondaryAction) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(secondaryActionText)
            }
        }
    }
}

@Composable
fun AlbumDialog(
    title: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit,
    initialName: String = "",
    initialDescription: String = ""
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Tên album") },
                    placeholder = { Text("Ví dụ: Du lịch Đà Lạt, Gia đình...") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    maxLines = 3,
                    label = { Text("Mô tả ngắn (tùy chọn)") },
                    placeholder = { Text("Ghi chú kỷ niệm cho album này...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), description.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text(confirmText)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
fun AlbumPickerDialog(
    albums: List<AlbumWithCount>,
    onPick: (Long) -> Unit,
    onCreate: (name: String, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        AlbumDialog(
            title = "Tạo album mới",
            confirmText = "Tạo và chuyển ảnh",
            onDismiss = onDismiss,
            onConfirm = onCreate
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chuyển ảnh vào Album", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { creating = true }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CreateNewFolder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.size(16.dp))
                        Text(
                            "Tạo album mới",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (albums.isNotEmpty()) HorizontalDivider()
                }
                items(albums, key = { it.id }) { album ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(album.id) }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(album.name, fontWeight = FontWeight.Medium)
                            if (album.description.isNotBlank()) {
                                Text(
                                    album.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            "${album.photoCount} ảnh",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng") } }
    )
}
