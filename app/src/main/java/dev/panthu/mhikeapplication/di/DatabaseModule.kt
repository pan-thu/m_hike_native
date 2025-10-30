package dev.panthu.mhikeapplication.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.panthu.mhikeapplication.data.local.MHikeDatabase
import dev.panthu.mhikeapplication.data.local.dao.HikeDao
import dev.panthu.mhikeapplication.data.local.dao.ObservationDao
import javax.inject.Singleton

/**
 * Hilt module providing Room database and DAO dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMHikeDatabase(
        @ApplicationContext context: Context
    ): MHikeDatabase {
        return Room.databaseBuilder(
            context,
            MHikeDatabase::class.java,
            MHikeDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development - remove in production
            .build()
    }

    @Provides
    @Singleton
    fun provideHikeDao(database: MHikeDatabase): HikeDao {
        return database.hikeDao()
    }

    @Provides
    @Singleton
    fun provideObservationDao(database: MHikeDatabase): ObservationDao {
        return database.observationDao()
    }
}
