package com.example.photomanager.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.photomanager.data.Photo
import java.io.File

/**
 * Bảng chia sẻ trực tiếp lên mạng xã hội (Facebook, Twitter/X, Messenger, Instagram, Zalo & Hệ thống).
 * Hỗ trợ soạn lời tựa (caption), gắn hashtag nhanh và xem trước các ảnh sẽ chia sẻ.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialShareBottomSheet(
    photos: List<Photo>,
    onDismiss: () -> Unit
) {
    if (photos.isEmpty()) return
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val defaultCaption = remember(photos) {
        val first = photos.first()
        buildString {
            if (first.title.isNotBlank()) append(first.title)
            if (first.caption.isNotBlank()) {
                if (isNotEmpty()) append(" — ")
                append(first.caption)
            }
            if (first.locationTag.isNotBlank()) {
                if (isNotEmpty()) append("\n")
                append("📍 ").append(first.locationTag)
            }
            if (isEmpty()) append("Chia sẻ khoảnh khắc đẹp từ Album ✨")
        }
    }

    var caption by remember { mutableStateOf(defaultCaption) }
    var missingPlatform by remember { mutableStateOf<SocialPlatform?>(null) }

    val hashtags = listOf("#KỷNiệm", "#DuLịch", "#NhiếpẢnh", "#Album", "#CuộcSống")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Chia sẻ trực tiếp lên Mạng xã hội",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${photos.size} ảnh đã chọn • Tổng ${formatFileSize(photos.sumOf { File(it.path).length() })}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dải ảnh xem trước
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(photos, key = { it.id }) { photo ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(photo.path))
                            .crossfade(true)
                            .build(),
                        contentDescription = photo.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(82.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }

            // Ô nhập nội dung bài đăng / Caption
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Lời tựa bài đăng (Caption kèm ảnh)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                trailingIcon = {
                    IconButton(onClick = {
                        context.copyToClipboard(caption)
                        Toast.makeText(context, "Đã sao chép lời tựa!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Sao chép lời tựa")
                    }
                }
            )

            // Hàng gợi ý Hashtag nhanh
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                hashtags.forEach { tag ->
                    AssistChip(
                        onClick = {
                            if (!caption.contains(tag)) {
                                caption = (caption.trimEnd() + " " + tag).trim()
                            }
                        },
                        label = { Text(tag) }
                    )
                }
            }

            Text(
                text = "Chọn nền tảng chia sẻ trực tiếp",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            // Nút nổi bật cho Facebook & Twitter (X) đúng trọng tâm đề bài
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimarySocialCard(
                    platform = SocialPlatform.FACEBOOK,
                    installed = context.isPlatformInstalled(SocialPlatform.FACEBOOK),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val ok = context.shareToSocialPlatform(SocialPlatform.FACEBOOK, photos, caption)
                        if (ok) onDismiss() else missingPlatform = SocialPlatform.FACEBOOK
                    }
                )
                PrimarySocialCard(
                    platform = SocialPlatform.TWITTER,
                    installed = context.isPlatformInstalled(SocialPlatform.TWITTER),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val ok = context.shareToSocialPlatform(SocialPlatform.TWITTER, photos, caption)
                        if (ok) onDismiss() else missingPlatform = SocialPlatform.TWITTER
                    }
                )
            }

            // Hàng các ứng dụng phổ biến khác (Messenger, Instagram, Zalo, Hệ thống)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    SocialPlatform.MESSENGER,
                    SocialPlatform.INSTAGRAM,
                    SocialPlatform.ZALO,
                    SocialPlatform.SYSTEM_ALL
                ).forEach { platform ->
                    SecondarySocialCircle(
                        platform = platform,
                        onClick = {
                            val ok = context.shareToSocialPlatform(platform, photos, caption)
                            if (ok) onDismiss() else missingPlatform = platform
                        }
                    )
                }
            }
        }
    }

    // Hộp thoại thông minh khi máy ảo chưa cài sẵn app Facebook / Twitter gốc
    missingPlatform?.let { platform ->
        AlertDialog(
            onDismissRequest = { missingPlatform = null },
            title = { Text("Chia sẻ lên ${platform.label}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Thiết bị/máy ảo hiện chưa cài đặt ứng dụng ${platform.label} gốc. " +
                            "Lời tựa bài viết đã được tự động sao chép vào bộ nhớ tạm!"
                    )
                    Text(
                        "Bạn muốn mở Hộp thoại Chia sẻ hệ thống (để gửi ảnh qua các ứng dụng có sẵn trên máy) " +
                            "hay mở trang đăng bài ${platform.label} trên trình duyệt?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val target = platform
                    missingPlatform = null
                    context.sharePhotosSystem(photos, caption)
                    onDismiss()
                }) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Chia sẻ hệ thống")
                }
            },
            dismissButton = {
                Row {
                    if (platform.webShareUrl != null) {
                        OutlinedButton(onClick = {
                            val target = platform
                            missingPlatform = null
                            context.openSocialWebFallback(target, caption)
                            onDismiss()
                        }) {
                            Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Mở ${platform.label} Web")
                        }
                    }
                    TextButton(onClick = { missingPlatform = null }) {
                        Text("Đóng")
                    }
                }
            }
        )
    }
}

@Composable
private fun PrimarySocialCard(
    platform: SocialPlatform,
    installed: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val brandColor = Color(platform.colorHex)
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = brandColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = platform.badgeText,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = platform.label,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = if (installed) platform.subtitle else "Chia sẻ nhanh",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SecondarySocialCircle(
    platform: SocialPlatform,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(platform.colorHex), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = platform.badgeText,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = platform.label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
