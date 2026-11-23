package com.example.photomanager.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.photomanager.data.Photo
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

enum class SocialPlatform(
    val label: String,
    val subtitle: String,
    val packages: List<String>,
    val colorHex: Long,
    val badgeText: String,
    val webShareUrl: String? = null
) {
    FACEBOOK(
        label = "Facebook",
        subtitle = "Đăng bảng tin / Story",
        packages = listOf("com.facebook.katana", "com.facebook.lite", "com.facebook.wakizashi"),
        colorHex = 0xFF1877F2,
        badgeText = "f",
        webShareUrl = "https://www.facebook.com/sharer/sharer.php?quote="
    ),
    TWITTER(
        label = "Twitter / X",
        subtitle = "Đăng bài Tweet kèm ảnh",
        packages = listOf("com.twitter.android", "com.twitter.android.lite"),
        colorHex = 0xFF0F1419,
        badgeText = "𝕏",
        webShareUrl = "https://twitter.com/intent/tweet?text="
    ),
    MESSENGER(
        label = "Messenger",
        subtitle = "Gửi tin nhắn bạn bè",
        packages = listOf("com.facebook.orca", "com.facebook.mlite"),
        colorHex = 0xFF00B2FF,
        badgeText = "M"
    ),
    INSTAGRAM(
        label = "Instagram",
        subtitle = "Chia sẻ Feed / Story",
        packages = listOf("com.instagram.android"),
        colorHex = 0xFFE1306C,
        badgeText = "IG"
    ),
    ZALO(
        label = "Zalo",
        subtitle = "Gửi ảnh chất lượng cao",
        packages = listOf("com.zing.zalo"),
        colorHex = 0xFF0068FF,
        badgeText = "Z"
    ),
    SYSTEM_ALL(
        label = "Ứng dụng khác",
        subtitle = "Gmail, Tin nhắn, Drive, Bluetooth...",
        packages = emptyList(),
        colorHex = 0xFF455A64,
        badgeText = "•••"
    )
}

fun Context.isPlatformInstalled(platform: SocialPlatform): Boolean {
    if (platform.packages.isEmpty()) return true
    val pm = packageManager
    return platform.packages.any { pkg ->
        runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess ||
            pm.getLaunchIntentForPackage(pkg) != null
    }
}

fun Context.copyToClipboard(text: String) {
    if (text.isBlank()) return
    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("Caption ảnh", text))
}

/**
 * Tạo Intent chia sẻ 1 hoặc nhiều ảnh qua FileProvider kèm ClipData chuẩn Android 10-15.
 */
private fun Context.buildShareIntent(photos: List<Photo>, caption: String): Intent? {
    val validFiles = photos.map { File(it.path) }.filter { it.exists() }
    if (validFiles.isEmpty()) return null

    val authority = "$packageName.fileprovider"
    val uris = ArrayList<Uri>(
        validFiles.map { FileProvider.getUriForFile(this, authority, it) }
    )

    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uris[0])
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
    }

    intent.type = "image/*"
    if (caption.isNotBlank()) {
        intent.putExtra(Intent.EXTRA_TEXT, caption)
        intent.putExtra(Intent.EXTRA_TITLE, caption)
        intent.putExtra(Intent.EXTRA_SUBJECT, "Chia sẻ ảnh từ Album")
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    // Đính kèm ClipData để mọi ứng dụng nhận được quyền đọc URI ảnh
    val clipData = ClipData.newUri(contentResolver, "Shared Photo", uris[0])
    for (i in 1 until uris.size) {
        clipData.addItem(ClipData.Item(uris[i]))
    }
    intent.clipData = clipData
    return intent
}

/**
 * Chia sẻ ảnh qua hộp thoại hệ thống mặc định.
 */
fun Context.sharePhotosSystem(photos: List<Photo>, caption: String = "") {
    val baseIntent = buildShareIntent(photos, caption) ?: return
    if (caption.isNotBlank()) copyToClipboard(caption)
    val chooser = Intent.createChooser(baseIntent, "Chia sẻ ${photos.size} ảnh")
    if (this !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(chooser) }
}

/**
 * Chia sẻ trực tiếp lên mạng xã hội được chọn (Facebook, Twitter/X, Messenger, Zalo...).
 * Trả về true nếu mở trực tiếp được app mục tiêu, false nếu máy chưa cài app đó.
 */
fun Context.shareToSocialPlatform(
    platform: SocialPlatform,
    photos: List<Photo>,
    caption: String
): Boolean {
    if (caption.isNotBlank()) {
        copyToClipboard(caption)
    }
    if (platform == SocialPlatform.SYSTEM_ALL) {
        sharePhotosSystem(photos, caption)
        return true
    }

    val baseIntent = buildShareIntent(photos, caption) ?: return false
    val pm = packageManager

    for (pkg in platform.packages) {
        val targeted = Intent(baseIntent).apply {
            setPackage(pkg)
            if (this@shareToSocialPlatform !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        val resolved = pm.queryIntentActivities(targeted, 0)
        if (resolved.isNotEmpty()) {
            val result = runCatching {
                startActivity(targeted)
                Toast.makeText(
                    this,
                    "Đang mở ${platform.label} (đã sao chép sẵn lời tựa vào bộ nhớ tạm)",
                    Toast.LENGTH_SHORT
                ).show()
                true
            }.getOrDefault(false)
            if (result) return true
        }
    }
    return false
}

/**
 * Mở trình đăng bài nền tảng web của Facebook / Twitter(X) khi trên máy ảo chưa cài ứng dụng gốc.
 */
fun Context.openSocialWebFallback(platform: SocialPlatform, caption: String) {
    val baseUrl = platform.webShareUrl ?: return
    val encoded = Uri.encode(caption.ifBlank { "Chia sẻ khoảnh khắc từ Album" })
    val url = if (platform == SocialPlatform.FACEBOOK) {
        "https://www.facebook.com/sharer/sharer.php?u=${Uri.encode("https://photos.google.com")}&quote=$encoded"
    } else {
        "$baseUrl$encoded"
    }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

fun Context.displayName(uri: Uri): String? = runCatching {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }
}.getOrNull()

fun localDateOf(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

private fun weekdayName(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Thứ Hai"
    DayOfWeek.TUESDAY -> "Thứ Ba"
    DayOfWeek.WEDNESDAY -> "Thứ Tư"
    DayOfWeek.THURSDAY -> "Thứ Năm"
    DayOfWeek.FRIDAY -> "Thứ Sáu"
    DayOfWeek.SATURDAY -> "Thứ Bảy"
    DayOfWeek.SUNDAY -> "Chủ Nhật"
}

/** Nhãn ngày cho tiêu đề nhóm trên dòng thời gian. */
fun dayLabel(millis: Long): String {
    val date = localDateOf(millis)
    val today = LocalDate.now()
    return when (date) {
        today -> "Hôm nay • ${date.dayOfMonth} thg ${date.monthValue}"
        today.minusDays(1) -> "Hôm qua • ${date.dayOfMonth} thg ${date.monthValue}"
        else -> "${weekdayName(date.dayOfWeek)}, ${date.dayOfMonth} tháng ${date.monthValue}, ${date.year}"
    }
}

/** Nhãn tháng cho chế độ nhóm theo tháng. */
fun monthLabel(millis: Long): String {
    val date = localDateOf(millis)
    return "Tháng ${date.monthValue}, ${date.year}"
}

fun formatDate(date: LocalDate): String =
    "%02d/%02d/%d".format(date.dayOfMonth, date.monthValue, date.year)

fun formatDateTime(millis: Long): String {
    val zoned = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    return "%02d/%02d/%d • %02d:%02d".format(
        zoned.dayOfMonth, zoned.monthValue, zoned.year, zoned.hour, zoned.minute
    )
}

fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
