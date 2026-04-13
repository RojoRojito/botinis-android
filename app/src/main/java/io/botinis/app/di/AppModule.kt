package io.botinis.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.botinis.app.data.remote.GroqApiService
import io.botinis.app.data.remote.RetrofitClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGroqApiService(): GroqApiService {
        return RetrofitClient.create()
    }
}
