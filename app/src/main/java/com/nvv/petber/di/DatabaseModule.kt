package com.nvv.petber.di

import android.content.Context
import androidx.room.Room
import com.nvv.petber.data.AppDatabase
import com.nvv.petber.data.dao.ChatDao
import com.nvv.petber.data.dao.PostDao
import com.nvv.petber.data.dao.ProfileDao
import com.nvv.petber.data.dao.StoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "petber_db")
            .fallbackToDestructiveMigration().build()

    @Provides
    fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()
    @Provides
    fun provideChatDao(db: AppDatabase): ChatDao = db.chatDao()

    @Provides
    @Singleton
    fun providePostDao(database: AppDatabase): PostDao {
        return database.postDao()
    }

    @Provides
    @Singleton
    fun provideStoryDao(database: AppDatabase): StoryDao {
        return database.storyDao()
    }
}