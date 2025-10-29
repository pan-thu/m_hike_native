package dev.panthu.mhikeapplication.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.panthu.mhikeapplication.data.repository.AuthRepositoryImpl
import dev.panthu.mhikeapplication.data.repository.HikeRepositoryImpl
import dev.panthu.mhikeapplication.data.repository.ImageRepositoryImpl
import dev.panthu.mhikeapplication.data.repository.LocationRepositoryImpl
import dev.panthu.mhikeapplication.data.repository.ObservationRepositoryImpl
import dev.panthu.mhikeapplication.domain.repository.AuthRepository
import dev.panthu.mhikeapplication.domain.repository.HikeRepository
import dev.panthu.mhikeapplication.domain.repository.ImageRepository
import dev.panthu.mhikeapplication.domain.repository.LocationRepository
import dev.panthu.mhikeapplication.domain.repository.ObservationRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindHikeRepository(
        hikeRepositoryImpl: HikeRepositoryImpl
    ): HikeRepository

    @Binds
    @Singleton
    abstract fun bindObservationRepository(
        observationRepositoryImpl: ObservationRepositoryImpl
    ): ObservationRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): LocationRepository

    @Binds
    @Singleton
    abstract fun bindImageRepository(
        imageRepositoryImpl: ImageRepositoryImpl
    ): ImageRepository
}
