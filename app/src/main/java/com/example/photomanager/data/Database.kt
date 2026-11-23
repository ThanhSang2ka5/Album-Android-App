package com.example.photomanager.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Một album do người dùng tạo. */
@Entity(tableName = "albums")
data class Album(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** Một ảnh đã được lưu trữ trong bộ nhớ riêng của ứng dụng. */
@Entity(tableName = "photos")
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val title: String = "",
    val caption: String = "",
    val locationTag: String = "",
    val dateTaken: Long,
    val albumId: Long? = null,
    val isFavorite: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0L
)

/** Album kèm số lượng ảnh và ảnh bìa mới nhất. */
data class AlbumWithCount(
    val id: Long,
    val name: String,
    val description: String,
    val photoCount: Int,
    val coverPath: String?
)

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<Photo>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: Photo): Long

    @Query("SELECT * FROM photos ORDER BY dateTaken DESC, id DESC")
    fun observeAll(): Flow<List<Photo>>

    @Query("SELECT * FROM photos")
    suspend fun getAllOnce(): List<Photo>

    @Query("SELECT COUNT(*) FROM photos")
    suspend fun count(): Int

    @Query("UPDATE photos SET albumId = :albumId WHERE id IN (:ids)")
    suspend fun moveToAlbum(ids: List<Long>, albumId: Long?)

    @Query("UPDATE photos SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE photos SET isFavorite = :favorite WHERE id IN (:ids)")
    suspend fun setFavoriteForIds(ids: List<Long>, favorite: Boolean)

    @Query("UPDATE photos SET title = :title, caption = :caption, locationTag = :locationTag, dateTaken = :dateTaken, albumId = :albumId WHERE id = :id")
    suspend fun updateDetails(
        id: Long,
        title: String,
        caption: String,
        locationTag: String,
        dateTaken: Long,
        albumId: Long?
    )

    @Query("UPDATE photos SET width = :width, height = :height, sizeBytes = :sizeBytes WHERE id = :id")
    suspend fun updateDimensions(id: Long, width: Int, height: Int, sizeBytes: Long)

    @Query("DELETE FROM photos WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE photos SET albumId = NULL WHERE albumId = :albumId")
    suspend fun clearAlbum(albumId: Long)
}

@Dao
interface AlbumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: Album): Long

    @Query("UPDATE albums SET name = :name, description = :description WHERE id = :id")
    suspend fun update(id: Long, name: String, description: String)

    @Query("DELETE FROM albums WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        """
        SELECT a.id AS id,
               a.name AS name,
               a.description AS description,
               (SELECT COUNT(*) FROM photos p WHERE p.albumId = a.id) AS photoCount,
               (SELECT p.path FROM photos p WHERE p.albumId = a.id ORDER BY p.dateTaken DESC, p.id DESC LIMIT 1) AS coverPath
        FROM albums a
        ORDER BY a.name COLLATE NOCASE
        """
    )
    fun observeWithCount(): Flow<List<AlbumWithCount>>

    @Query("SELECT * FROM albums")
    suspend fun getAllOnce(): List<Album>
}

@Database(entities = [Album::class, Photo::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun albumDao(): AlbumDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "photo_manager.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
