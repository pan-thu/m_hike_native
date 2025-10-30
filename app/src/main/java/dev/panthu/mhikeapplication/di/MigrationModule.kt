package dev.panthu.mhikeapplication.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.panthu.mhikeapplication.data.service.MigrationServiceImpl
import dev.panthu.mhikeapplication.domain.service.MigrationService
import javax.inject.Singleton

/**
 * Hilt module for migration service dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MigrationModule {

    @Binds
    @Singleton
    abstract fun bindMigrationService(
        migrationServiceImpl: MigrationServiceImpl
    ): MigrationService
}
