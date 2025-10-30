package dev.panthu.mhikeapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.panthu.mhikeapplication.data.local.dao.HikeDao
import dev.panthu.mhikeapplication.data.local.dao.ObservationDao
import dev.panthu.mhikeapplication.data.local.entity.HikeEntity
import dev.panthu.mhikeapplication.data.local.entity.ObservationEntity

/**
 * Room database for local storage of hikes and observations
 * Used in guest mode for offline-first functionality
 */
@Database(
    entities = [
        HikeEntity::class,
        ObservationEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MHikeDatabase : RoomDatabase() {

    abstract fun hikeDao(): HikeDao
    abstract fun observationDao(): ObservationDao

    companion object {
        const val DATABASE_NAME = "mhike_database"
    }
}
