package com.example.photomanager

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photomanager.data.Album
import com.example.photomanager.data.AlbumWithCount
import com.example.photomanager.data.AppDatabase
import com.example.photomanager.data.BuiltInTrack
import com.example.photomanager.data.DemoContentGenerator
import com.example.photomanager.data.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

enum class SlideEffect(val label: String, val subtitle: String) {
    KEN_BURNS("Ken Burns", "Phóng nhẹ & lia máy điện ảnh"),
    FADE("Mờ dần", "Chuyển cảnh êm dịu cổ điển"),
    SLIDE("Trượt ngang", "Chuyển trang mượt mà"),
    DEPTH_ZOOM("Thu phóng 3D", "Hiệu ứng chiều sâu không gian")
}

enum class ThemeMode(val label: String) {
    SYSTEM("Theo hệ thống"),
    LIGHT("Giao diện Sáng"),
    DARK("Giao diện Tối")
}

data class SavedCustomTrack(
    val id: String,
    val title: String,
    val path: String
)

data class SlideshowConfig(
    val photos: List<Photo>,
    val intervalMs: Long,
    val effect: SlideEffect,
    val musicUri: Uri?,
    val musicTitle: String?,
    val loopSlideshow: Boolean = true,
    val showCaptions: Boolean = true
)

class PhotoViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val photoDao = db.photoDao()
    private val albumDao = db.albumDao()
    private val prefs = app.getSharedPreferences("lumina_gallery_prefs", Context.MODE_PRIVATE)

    var photosLoaded by mutableStateOf(false)
        private set

    val photos: StateFlow<List<Photo>> = photoDao.observeAll()
        .onEach { photosLoaded = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val albums: StateFlow<List<AlbumWithCount>> = albumDao.observeWithCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var isImporting by mutableStateOf(false)
        private set

    var themeMode by mutableStateOf(ThemeMode.SYSTEM)

    /** Thông báo ngắn hiển thị qua Snackbar. */
    var snackbarMessage by mutableStateOf<String?>(null)

    /** Đếm số lần thay đổi file ảnh (ví dụ sau khi xoay ảnh) để làm mới cache hiển thị. */
    var imageRevision by mutableIntStateOf(0)
        private set

    /** Cấu hình slideshow đang phát. */
    var slideshow by mutableStateOf<SlideshowConfig?>(null)

    /** Danh sách ID ảnh được chọn trước để tạo slideshow. */
    var slideshowPreselect by mutableStateOf<List<Long>>(emptyList())

    /** Danh sách nhạc nền người dùng đã chọn từ máy (được lưu vĩnh viễn trong bộ nhớ app). */
    var customTracks by mutableStateOf<List<SavedCustomTrack>>(emptyList())
        private set

    var selectedBuiltInTrack by mutableStateOf<BuiltInTrack?>(BuiltInTrack.PIANO_MEMORIES)
        private set

    var selectedCustomTrackId by mutableStateOf<String?>(null)
        private set

    var musicMuted by mutableStateOf(false)
        private set

    private var pendingCameraFile: File? = null

    init {
        loadSavedMusicPrefs()
        viewModelScope.launch(Dispatchers.IO) {
            // Đảm bảo nhạc nền mẫu luôn sẵn sàng
            BuiltInTrack.entries.forEach { track ->
                runCatching { DemoContentGenerator.ensureBuiltInTrackFile(app, track) }
            }
            // Tự động nạp bộ 5 ảnh khởi đầu do người dùng cung cấp (thay thế ảnh vẽ mẫu cũ nếu có)
            val seeded = prefs.getBoolean("demo_seeded_v3", false)
            if (!seeded) {
                isImporting = true
                runCatching {
                    DemoContentGenerator.seedDemoData(app, albumDao, photoDao)
                    prefs.edit().putBoolean("demo_seeded_v3", true).apply()
                }
                isImporting = false
            }
        }
    }

    fun clearSnackbar() {
        snackbarMessage = null
    }

    fun cycleThemeMode() {
        val next = when (themeMode) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        themeMode = next
        snackbarMessage = "Đã chuyển: ${next.label}"
    }

    // ---------------------------------------------------------------- Quản lý ảnh

    fun generateDemoGallery() {
        val app = getApplication<Application>()
        viewModelScope.launch {
            isImporting = true
            val count = withContext(Dispatchers.IO) {
                runCatching { DemoContentGenerator.seedDemoData(app, albumDao, photoDao) }.getOrDefault(0)
            }
            isImporting = false
            if (count > 0) {
                snackbarMessage = "Đã nạp lại $count ảnh khởi đầu!"
            }
        }
    }

    fun addPhotos(uris: List<Uri>, albumId: Long? = null) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            isImporting = true
            val imported = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri -> runCatching { importOne(uri, albumId) }.getOrNull() }
            }
            if (imported.isNotEmpty()) {
                photoDao.insertAll(imported)
                snackbarMessage = "Đã lưu ${imported.size} ảnh vào ứng dụng"
            }
            isImporting = false
        }
    }

    fun createCameraCaptureUri(): Uri? {
        val context = getApplication<Application>()
        return runCatching {
            val dir = File(context.cacheDir, "camera").apply { mkdirs() }
            val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
            pendingCameraFile = file
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    }

    fun onCameraResult(success: Boolean, albumId: Long? = null) {
        val capturedFile = pendingCameraFile
        pendingCameraFile = null
        if (!success || capturedFile == null || !capturedFile.exists() || capturedFile.length() == 0L) {
            capturedFile?.delete()
            return
        }
        viewModelScope.launch {
            isImporting = true
            val savedPhoto = withContext(Dispatchers.IO) {
                runCatching {
                    val context = getApplication<Application>()
                    val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
                    val destFile = File(photosDir, "cam_${UUID.randomUUID().toString().take(8)}.jpg")
                    capturedFile.copyTo(destFile, overwrite = true)
                    capturedFile.delete()
                    val (w, h) = readDimensions(destFile)
                    val now = System.currentTimeMillis()
                    Photo(
                        path = destFile.absolutePath,
                        title = "Ảnh chụp Camera",
                        caption = "Chụp trực tiếp từ máy ảnh",
                        locationTag = "",
                        dateTaken = now,
                        albumId = albumId,
                        isFavorite = false,
                        width = w,
                        height = h,
                        sizeBytes = destFile.length()
                    )
                }.getOrNull()
            }
            if (savedPhoto != null) {
                photoDao.insert(savedPhoto)
                snackbarMessage = "Đã lưu ảnh chụp mới!"
            }
            isImporting = false
        }
    }

    fun toggleFavorite(photo: Photo) {
        viewModelScope.launch {
            val next = !photo.isFavorite
            photoDao.setFavorite(photo.id, next)
            snackbarMessage = if (next) "Đã thêm vào Yêu thích ❤️" else "Đã bỏ khỏi Yêu thích"
        }
    }

    fun setFavorites(ids: Collection<Long>, favorite: Boolean) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.toList().chunked(500).forEach { photoDao.setFavoriteForIds(it, favorite) }
            snackbarMessage = if (favorite) {
                "Đã thêm ${ids.size} ảnh vào Yêu thích ❤️"
            } else {
                "Đã bỏ Yêu thích ${ids.size} ảnh"
            }
        }
    }

    fun updatePhotoDetails(
        id: Long,
        title: String,
        caption: String,
        locationTag: String,
        dateTaken: Long,
        albumId: Long?
    ) {
        viewModelScope.launch {
            photoDao.updateDetails(
                id = id,
                title = title.trim(),
                caption = caption.trim(),
                locationTag = locationTag.trim(),
                dateTaken = dateTaken,
                albumId = albumId
            )
            snackbarMessage = "Đã lưu thông tin ảnh"
        }
    }

    fun rotatePhoto90(photo: Photo) {
        viewModelScope.launch {
            val updated = withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(photo.path)
                    if (!file.exists()) return@runCatching false
                    val original = BitmapFactory.decodeFile(file.absolutePath) ?: return@runCatching false
                    val matrix = Matrix().apply { postRotate(90f) }
                    val rotated = Bitmap.createBitmap(
                        original, 0, 0, original.width, original.height, matrix, true
                    )
                    file.outputStream().use { out ->
                        rotated.compress(Bitmap.CompressFormat.JPEG, 94, out)
                    }
                    val newW = rotated.width
                    val newH = rotated.height
                    if (rotated != original) original.recycle()
                    rotated.recycle()
                    photoDao.updateDimensions(photo.id, newW, newH, file.length())
                    true
                }.getOrDefault(false)
            }
            if (updated) {
                imageRevision++
                snackbarMessage = "Đã xoay ảnh 90°"
            }
        }
    }

    fun deletePhotos(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val idSet = ids.toSet()
            val targets = photos.value.filter { it.id in idSet }
            withContext(Dispatchers.IO) { targets.forEach { File(it.path).delete() } }
            targets.map { it.id }.chunked(500).forEach { photoDao.deleteByIds(it) }
            snackbarMessage = "Đã xóa ${targets.size} ảnh"
        }
    }

    fun moveToAlbum(ids: List<Long>, albumId: Long?) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.chunked(500).forEach { photoDao.moveToAlbum(it, albumId) }
            val albumName = albums.value.firstOrNull { it.id == albumId }?.name
            snackbarMessage = if (albumId == null) {
                "Đã gỡ ${ids.size} ảnh khỏi album"
            } else {
                "Đã chuyển ${ids.size} ảnh vào album \"${albumName ?: ""}\""
            }
        }
    }

    // -------------------------------------------------------------- Quản lý Album

    fun createAlbum(name: String, description: String = "", movePhotoIds: List<Long> = emptyList()) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val id = albumDao.insert(Album(name = clean, description = description.trim()))
            if (movePhotoIds.isNotEmpty()) {
                movePhotoIds.chunked(500).forEach { photoDao.moveToAlbum(it, id) }
            }
            snackbarMessage = "Đã tạo album \"$clean\""
        }
    }

    fun updateAlbum(id: Long, name: String, description: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            albumDao.update(id, clean, description.trim())
            snackbarMessage = "Đã cập nhật album \"$clean\""
        }
    }

    /** Xóa album nhưng vẫn giữ lại các ảnh trong Dòng thời gian. */
    fun deleteAlbum(id: Long) {
        viewModelScope.launch {
            photoDao.clearAlbum(id)
            albumDao.delete(id)
            snackbarMessage = "Đã xóa album (ảnh vẫn được giữ trong Dòng thời gian)"
        }
    }

    fun getBuiltInTrackUri(track: BuiltInTrack): Uri {
        return DemoContentGenerator.ensureBuiltInTrackFile(getApplication(), track)
    }

    // ------------------------------------------------------------ Quản lý Nhạc nền

    fun importCustomMusic(uri: Uri) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(app.filesDir, "music/custom").apply { mkdirs() }
                    val rawName = queryFileName(app, uri) ?: "Nhạc nền ${customTracks.size + 1}.mp3"
                    val ext = rawName.substringAfterLast('.', "").lowercase(Locale.US)
                        .ifBlank {
                            val mime = app.contentResolver.getType(uri)
                            MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "mp3"
                        }
                    val id = UUID.randomUUID().toString().take(8)
                    val file = File(dir, "track_$id.$ext")
                    val input = app.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Không đọc được file nhạc")
                    input.use { src -> file.outputStream().use { out -> src.copyTo(out) } }
                    val displayTitle = rawName.substringBeforeLast('.').ifBlank { rawName }
                    SavedCustomTrack(id = id, title = displayTitle, path = file.absolutePath)
                }.getOrNull()
            }
            if (saved != null) {
                customTracks = customTracks + saved
                selectedCustomTrackId = saved.id
                selectedBuiltInTrack = null
                musicMuted = false
                persistMusicPrefs()
                snackbarMessage = "Đã lưu bài nhạc \"${saved.title}\""
            } else {
                snackbarMessage = "Không thể lưu file nhạc này"
            }
        }
    }

    fun selectBuiltInMusic(track: BuiltInTrack) {
        selectedBuiltInTrack = track
        selectedCustomTrackId = null
        musicMuted = false
        persistMusicPrefs()
    }

    fun selectCustomMusic(trackId: String) {
        selectedCustomTrackId = trackId
        selectedBuiltInTrack = null
        musicMuted = false
        persistMusicPrefs()
    }

    fun muteMusic() {
        musicMuted = true
        persistMusicPrefs()
    }

    fun deleteCustomTrack(trackId: String) {
        val target = customTracks.firstOrNull { it.id == trackId } ?: return
        customTracks = customTracks.filterNot { it.id == trackId }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { File(target.path).delete() }
        }
        if (selectedCustomTrackId == trackId) {
            selectedCustomTrackId = customTracks.firstOrNull()?.id
            if (selectedCustomTrackId == null) {
                selectedBuiltInTrack = BuiltInTrack.PIANO_MEMORIES
            }
        }
        persistMusicPrefs()
        snackbarMessage = "Đã xóa bài nhạc \"${target.title}\""
    }

    private fun loadSavedMusicPrefs() {
        val jsonStr = prefs.getString("custom_tracks_json", null)
        val loaded = mutableListOf<SavedCustomTrack>()
        if (!jsonStr.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val path = obj.getString("path")
                    if (File(path).exists()) {
                        loaded.add(
                            SavedCustomTrack(
                                id = obj.getString("id"),
                                title = obj.getString("title"),
                                path = path
                            )
                        )
                    }
                }
            }
        }
        customTracks = loaded

        musicMuted = prefs.getBoolean("music_muted", false)
        val savedCustomId = prefs.getString("selected_custom_track_id", null)
        val savedBuiltInId = prefs.getString("selected_builtin_track_id", BuiltInTrack.PIANO_MEMORIES.id)

        if (savedCustomId != null && loaded.any { it.id == savedCustomId }) {
            selectedCustomTrackId = savedCustomId
            selectedBuiltInTrack = null
        } else {
            selectedCustomTrackId = null
            selectedBuiltInTrack = BuiltInTrack.entries.firstOrNull { it.id == savedBuiltInId }
                ?: BuiltInTrack.PIANO_MEMORIES
        }
    }

    private fun persistMusicPrefs() {
        val arr = JSONArray()
        customTracks.forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("path", t.path)
                }
            )
        }
        prefs.edit()
            .putString("custom_tracks_json", arr.toString())
            .putBoolean("music_muted", musicMuted)
            .putString("selected_custom_track_id", selectedCustomTrackId)
            .putString("selected_builtin_track_id", selectedBuiltInTrack?.id)
            .apply()
    }

    // ------------------------------------------------------------ Tiện ích nội bộ

    private fun importOne(uri: Uri, albumId: Long?): Photo {
        val context = getApplication<Application>()
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val mime = context.contentResolver.getType(uri)
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
        val file = File(dir, "img_${UUID.randomUUID().toString().take(8)}.$ext")
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Không mở được ảnh")
        input.use { source -> file.outputStream().use { out -> source.copyTo(out) } }

        val rawName = queryFileName(context, uri)?.substringBeforeLast('.') ?: "Ảnh kỷ niệm"
        val (w, h) = readDimensions(file)
        val (dateTaken, exifDesc) = readExifMetadata(file)

        return Photo(
            path = file.absolutePath,
            title = rawName,
            caption = exifDesc,
            locationTag = "",
            dateTaken = dateTaken,
            albumId = albumId,
            isFavorite = false,
            width = w,
            height = h,
            sizeBytes = file.length()
        )
    }

    private fun queryFileName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

    private fun readDimensions(file: File): Pair<Int, Int> = runCatching {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        opts.outWidth.coerceAtLeast(0) to opts.outHeight.coerceAtLeast(0)
    }.getOrDefault(0 to 0)

    private fun readExifMetadata(file: File): Pair<Long, String> {
        val fallback = System.currentTimeMillis()
        return try {
            val exif = ExifInterface(file.absolutePath)
            val rawDate = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            val date = if (rawDate == null) {
                fallback
            } else {
                SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(rawDate)?.time ?: fallback
            }
            val desc = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)?.trim().orEmpty()
            date to desc
        } catch (e: Exception) {
            fallback to ""
        }
    }
}
