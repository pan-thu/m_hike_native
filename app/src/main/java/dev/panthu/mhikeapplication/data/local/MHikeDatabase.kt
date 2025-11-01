package dev.panthu.mhikeapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MHikeDatabase : RoomDatabase() {

    abstract fun hikeDao(): HikeDao
    abstract fun observationDao(): ObservationDao

    companion object {
        const val DATABASE_NAME = "mhike_database"

        /**
         * Migration 1 -> 2: Make imageUrls nullable for better null safety
         * This is a schema relaxation - SQLite handles NULL values gracefully
         * Main change is in TypeConverter to handle null input
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No actual SQL changes needed - SQLite allows NULL by default
                // Migration is primarily for type converter changes
                // Just increment version to indicate schema compatibility change
            }
        }
    }
}
