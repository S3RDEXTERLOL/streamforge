package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamForgeDao {
    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun observeAllTasks(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): DownloadTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DownloadTaskEntity): Long

    @Update
    suspend fun updateTask(task: DownloadTaskEntity)

    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM download_tasks WHERE status IN ('COMPLETED', 'CANCELED', 'FAILED')")
    suspend fun clearInactiveTasks()

    @Query("SELECT * FROM format_presets ORDER BY isDefault DESC, id ASC")
    fun observeAllPresets(): Flow<List<FormatPresetEntity>>

    @Query("SELECT COUNT(*) FROM format_presets")
    suspend fun getPresetCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: FormatPresetEntity): Long

    @Query("DELETE FROM format_presets WHERE id = :id")
    suspend fun deletePresetById(id: Long)
}

@Database(
    entities = [DownloadTaskEntity::class, FormatPresetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StreamForgeDatabase : RoomDatabase() {
    abstract fun dao(): StreamForgeDao

    companion object {
        @Volatile
        private var INSTANCE: StreamForgeDatabase? = null

        fun getInstance(context: Context): StreamForgeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StreamForgeDatabase::class.java,
                    "streamforge_studio.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
