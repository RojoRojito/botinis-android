package io.botinis.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.botinis.app.data.local.BotinisDatabase
import io.botinis.app.data.local.dao.SessionDao
import io.botinis.app.data.local.dao.UserProgressDao
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BotinisDatabase {
        return Room.databaseBuilder(
            context,
            BotinisDatabase::class,
            "botinis_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideUserProgressDao(db: BotinisDatabase): UserProgressDao = db.userProgressDao()

    @Provides
    @Singleton
    fun provideSessionDao(db: BotinisDatabase): SessionDao = db.sessionDao()
}
