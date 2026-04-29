package com.nvv.petber.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nvv.petber.data.converter.DataConverters
import com.nvv.petber.data.converter.StringListConverter
import com.nvv.petber.data.dao.ChatDao
import com.nvv.petber.data.dao.ProfileDao
import com.nvv.petber.data.model.Comment
import com.nvv.petber.data.model.Conversation
import com.nvv.petber.data.model.Follow
import com.nvv.petber.data.model.Hashtag
import com.nvv.petber.data.model.MessageEntity
import com.nvv.petber.data.model.Notification
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.PetFollow
import com.nvv.petber.data.model.PetImage
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.PostHashtag
import com.nvv.petber.data.model.PostLike
import com.nvv.petber.data.model.PostMedia
import com.nvv.petber.data.model.Story
import com.nvv.petber.data.model.User

@Database(
    entities = [User::class, Pet::class, Post::class, Story::class, Follow::class,
        Comment::class, Hashtag::class, Notification::class, PetFollow::class, MessageEntity::class,
        PetImage::class, PostHashtag::class, PostLike::class, PostMedia::class, Conversation::class],
    version = 25,
    exportSchema = false
)
@TypeConverters(DataConverters::class, StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun chatDao(): ChatDao
}