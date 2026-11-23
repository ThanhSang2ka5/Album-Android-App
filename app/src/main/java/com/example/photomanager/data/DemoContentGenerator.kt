package com.example.photomanager.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Bản nhạc nền tích hợp sẵn trong ứng dụng (không cần internet hay file ngoài). */
enum class BuiltInTrack(
    val id: String,
    val title: String,
    val subtitle: String,
    val fileName: String,
    val tempoBpm: Int,
    val notesHz: List<Double>
) {
    PIANO_MEMORIES(
        id = "piano_memories",
        title = "Piano Kỷ Niệm Dịu Êm",
        subtitle = "Giai điệu dương cầm nhẹ nhàng • Tích hợp sẵn",
        fileName = "piano_memories.wav",
        tempoBpm = 84,
        notesHz = listOf(
            261.63, 329.63, 392.00, 493.88,
            220.00, 261.63, 329.63, 440.00,
            174.61, 220.00, 261.63, 349.23,
            196.00, 246.94, 293.66, 392.00
        )
    ),
    ACOUSTIC_SUNSET(
        id = "acoustic_sunset",
        title = "Acoustic Chiều Hoàng Hôn",
        subtitle = "Hợp âm du lịch ấm áp • Tích hợp sẵn",
        fileName = "acoustic_sunset.wav",
        tempoBpm = 96,
        notesHz = listOf(
            196.00, 293.66, 392.00, 493.88,
            164.81, 246.94, 329.63, 392.00,
            261.63, 329.63, 392.00, 523.25,
            293.66, 369.99, 440.00, 587.33
        )
    ),
    MUSIC_BOX_DREAM(
        id = "music_box_dream",
        title = "Hộp Nhạc Tuổi Thơ",
        subtitle = "Âm sắc trong trẻo lấp lánh • Tích hợp sẵn",
        fileName = "music_box_dream.wav",
        tempoBpm = 108,
        notesHz = listOf(
            659.25, 783.99, 523.25, 587.33,
            659.25, 523.25, 392.00, 440.00,
            523.25, 698.46, 659.25, 587.33,
            493.88, 392.00, 523.25, 659.25
        )
    )
}

object DemoContentGenerator {

    private data class StarterPhotoSpec(
        val assetPath: String,
        val title: String,
        val caption: String,
        val location: String,
        val albumName: String,
        val daysAgo: Int,
        val hoursOffset: Int,
        val isFavorite: Boolean
    )

    /**
     * Thay thế các ảnh vẽ mẫu cũ (demo_*.jpg) bằng 5 bức ảnh thật người dùng cung cấp
     * được đóng gói sẵn trong assets/starter_photos/.
     */
    suspend fun seedDemoData(context: Context, albumDao: AlbumDao, photoDao: PhotoDao): Int {
        // 1. Xóa các ảnh vẽ mẫu cũ (demo_*.jpg hoặc starter_*.jpg cũ) để thay bằng bộ 5 ảnh mới
        val allPhotos = photoDao.getAllOnce()
        val oldDemoPhotos = allPhotos.filter { p ->
            val fname = File(p.path).name
            fname.startsWith("demo_") || fname.startsWith("starter_")
        }
        if (oldDemoPhotos.isNotEmpty()) {
            oldDemoPhotos.forEach { runCatching { File(it.path).delete() } }
            photoDao.deleteByIds(oldDemoPhotos.map { it.id })
        }

        // 2. Dọn các album mẫu cũ nếu trống và đảm bảo các album cho 5 ảnh mới
        val albumDefs = mapOf(
            "Yêu thích & Cosplay" to "Bộ sưu tập ảnh chân dung, cosplay và nhân vật yêu thích",
            "Nghệ thuật & Ký họa" to "Tranh vẽ kiến trúc, meme và hình minh họa độc đáo",
            "Phong cảnh & Du lịch" to "Những khung cảnh hoàng hôn và điểm đến ấn tượng"
        )
        val currentAlbums = albumDao.getAllOnce().associateBy { it.name }.toMutableMap()
        val albumIds = mutableMapOf<String, Long>()
        for ((name, desc) in albumDefs) {
            val existing = currentAlbums[name]
            albumIds[name] = existing?.id ?: albumDao.insert(Album(name = name, description = desc))
        }

        // Xóa các album mẫu cũ không còn ảnh nào (nếu có)
        val remainingPhotos = photoDao.getAllOnce()
        val usedAlbumIds = remainingPhotos.mapNotNull { it.albumId }.toSet()
        val legacyNames = setOf(
            "Du lịch Việt Nam",
            "Kỷ niệm Gia đình & Bạn bè",
            "Thiên nhiên & Phong cảnh",
            "Ẩm thực & Đời sống"
        )
        for (alb in albumDao.getAllOnce()) {
            if (alb.name in legacyNames && alb.id !in usedAlbumIds) {
                albumDao.delete(alb.id)
            }
        }

        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L
        val hourMs = 3_600_000L

        val specs = listOf(
            StarterPhotoSpec(
                assetPath = "starter_photos/photo_1.jpg",
                title = "Cosplay Furina Blue",
                caption = "Khoảnh khắc cosplay tông xanh dương lung linh.",
                location = "Studio",
                albumName = "Yêu thích & Cosplay",
                daysAgo = 0,
                hoursOffset = 1,
                isFavorite = true
            ),
            StarterPhotoSpec(
                assetPath = "starter_photos/photo_3.jpg",
                title = "Hoàng hôn Phố biển Amalfi",
                caption = "Bậc thang hoa giấy rực rỡ nhìn ra bờ biển lúc chiều tà.",
                location = "Amalfi Coast",
                albumName = "Phong cảnh & Du lịch",
                daysAgo = 0,
                hoursOffset = 4,
                isFavorite = true
            ),
            StarterPhotoSpec(
                assetPath = "starter_photos/photo_2.jpg",
                title = "Ký họa Thành phố Góc cao",
                caption = "Bản vẽ phác thảo kiến trúc đô thị bằng nét bút mực đen.",
                location = "Art Gallery",
                albumName = "Nghệ thuật & Ký họa",
                daysAgo = 1,
                hoursOffset = 3,
                isFavorite = false
            ),
            StarterPhotoSpec(
                assetPath = "starter_photos/photo_5.jpg",
                title = "Bocchi Pink Avatar",
                caption = "Hình đại diện nhân vật anime tóc hồng dễ thương.",
                location = "Anime Collection",
                albumName = "Yêu thích & Cosplay",
                daysAgo = 2,
                hoursOffset = 5,
                isFavorite = true
            ),
            StarterPhotoSpec(
                assetPath = "starter_photos/photo_4.jpg",
                title = "Chibi Trái Tim Đáng Yêu",
                caption = "Tranh vẽ chibi nét cọ đơn giản biểu cảm mắt trái tim.",
                location = "Sticker Art",
                albumName = "Nghệ thuật & Ký họa",
                daysAgo = 3,
                hoursOffset = 2,
                isFavorite = false
            )
        )

        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val createdPhotos = mutableListOf<Photo>()

        specs.forEachIndexed { idx, spec ->
            val dateTaken = now - (spec.daysAgo * dayMs) - (spec.hoursOffset * hourMs)
            val file = File(dir, "starter_${idx + 1}_${UUID.randomUUID().toString().take(8)}.jpg")

            val copied = runCatching {
                context.assets.open(spec.assetPath).use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                true
            }.getOrDefault(false)

            if (copied && file.exists() && file.length() > 0L) {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, opts)
                val w = opts.outWidth.coerceAtLeast(0)
                val h = opts.outHeight.coerceAtLeast(0)

                runCatching {
                    val exif = ExifInterface(file.absolutePath)
                    val exifDate = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date(dateTaken))
                    exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, exifDate)
                    exif.setAttribute(ExifInterface.TAG_DATETIME, exifDate)
                    exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, spec.caption)
                    exif.saveAttributes()
                }

                createdPhotos.add(
                    Photo(
                        path = file.absolutePath,
                        title = spec.title,
                        caption = spec.caption,
                        locationTag = spec.location,
                        dateTaken = dateTaken,
                        albumId = albumIds[spec.albumName],
                        isFavorite = spec.isFavorite,
                        width = w,
                        height = h,
                        sizeBytes = file.length()
                    )
                )
            }
        }

        if (createdPhotos.isNotEmpty()) {
            photoDao.insertAll(createdPhotos)
        }
        BuiltInTrack.entries.forEach { ensureBuiltInTrackFile(context, it) }
        return createdPhotos.size
    }

    /**
     * Tổng hợp bản nhạc nền chuẩn định dạng WAV (PCM 16-bit, 22050Hz) với âm sắc ấm áp
     * để trình chiếu Slideshow chạy mượt mà ngay cả khi máy ảo không có file MP3.
     */
    fun ensureBuiltInTrackFile(context: Context, track: BuiltInTrack): Uri {
        val musicDir = File(context.filesDir, "music").apply { mkdirs() }
        val file = File(musicDir, track.fileName)
        if (!file.exists() || file.length() < 1024L) {
            val wavBytes = synthesizeMelodyWav(track)
            file.writeBytes(wavBytes)
        }
        return Uri.fromFile(file)
    }

    private fun synthesizeMelodyWav(track: BuiltInTrack): ByteArray {
        val sampleRate = 22050
        val noteDurationSec = 60.0 / track.tempoBpm
        val samplesPerNote = (sampleRate * noteDurationSec).toInt()
        val totalNotes = track.notesHz.size
        val totalSamples = samplesPerNote * totalNotes
        val pcm = ShortArray(totalSamples)

        for (n in 0 until totalNotes) {
            val freq = track.notesHz[n]
            val bassFreq = freq * 0.5
            val fifthFreq = freq * 1.4983
            val startSample = n * samplesPerNote

            for (i in 0 until samplesPerNote) {
                val idx = startSample + i
                if (idx >= totalSamples) break
                val t = i.toDouble() / sampleRate
                val attack = (i.toDouble() / (sampleRate * 0.018)).coerceAtMost(1.0)
                val decay = exp(-2.6 * t / noteDurationSec)
                val env = attack * decay

                val fundamental = sin(2.0 * PI * freq * t)
                val harmonic2 = 0.35 * sin(2.0 * PI * (freq * 2.0) * t) * exp(-4.0 * t)
                val harmonic3 = 0.15 * sin(2.0 * PI * fifthFreq * t)
                val warmBass = if (n % 4 == 0) 0.38 * sin(2.0 * PI * bassFreq * t) * exp(-1.2 * t) else 0.0

                val raw = (fundamental + harmonic2 + harmonic3 + warmBass) * env * 0.38
                val clamped = raw.coerceIn(-0.95, 0.95)
                pcm[idx] = (clamped * Short.MAX_VALUE).toInt().toShort()
            }
        }

        val dataSize = totalSamples * 2
        val out = ByteArrayOutputStream(44 + dataSize)
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }
        out.write(header.array())
        val pcmBuffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in pcm) {
            pcmBuffer.putShort(sample)
        }
        out.write(pcmBuffer.array())
        return out.toByteArray()
    }
}
