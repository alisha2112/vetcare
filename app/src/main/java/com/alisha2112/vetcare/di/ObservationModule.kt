package com.alisha2112.vetcare.di

import com.alisha2112.vetcare.data.repository.InMemoryObservationRepository
import com.alisha2112.vetcare.data.repository.ObservationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ObservationModule {

    @Binds
    @Singleton
    abstract fun bindRepository(
        implementation: InMemoryObservationRepository
    ): ObservationRepository
}
