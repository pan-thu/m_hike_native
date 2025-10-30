package dev.panthu.mhikeapplication.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.panthu.mhikeapplication.data.local.repository.LocalHikeRepositoryImpl
import dev.panthu.mhikeapplication.data.local.repository.LocalObservationRepositoryImpl
import dev.panthu.mhikeapplication.data.repository.AuthRepositoryImpl
import dev.panthu.mhikeapplication.data.repository.HikeRepositoryImpl
import dev.panthu.mhikeapplication.data.repository.ImageRepositoryImpl
import dev.panthu.mhikeapplication.data.repository.LocationRepositoryImpl
import dev.panthu.mhikeapplication.data.repository.ObservationRepositoryImpl
import dev.panthu.mhikeapplication.domain.provider.DynamicRepositoryProvider
import dev.panthu.mhikeapplication.domain.provider.RepositoryProvider
import dev.panthu.mhikeapplication.domain.repository.AuthRepository
import dev.panthu.mhikeapplication.domain.repository.HikeRepository
import dev.panthu.mhikeapplication.domain.repository.ImageRepository
import dev.panthu.mhikeapplication.domain.repository.LocationRepository
import dev.panthu.mhikeapplication.domain.repository.ObservationRepository
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    // Local repositories (Room + File storage)
    @Binds
    @Singleton
    @Named("local")
    abstract fun bindLocalHikeRepository(
        localHikeRepositoryImpl: LocalHikeRepositoryImpl
    ): HikeRepository

    @Binds
    @Singleton
    @Named("local")
    abstract fun bindLocalObservationRepository(
        localObservationRepositoryImpl: LocalObservationRepositoryImpl
    ): ObservationRepository

    // Remote repositories (Firebase)
    @Binds
    @Singleton
    @Named("remote")
    abstract fun bindRemoteHikeRepository(
        hikeRepositoryImpl: HikeRepositoryImpl
    ): HikeRepository

    @Binds
    @Singleton
    @Named("remote")
    abstract fun bindRemoteObservationRepository(
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

    // Repository provider (strategy selector)
    @Binds
    @Singleton
    abstract fun bindRepositoryProvider(
        dynamicRepositoryProvider: DynamicRepositoryProvider
    ): RepositoryProvider
}
