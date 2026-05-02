package com.nvv.petber.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nvv.petber.data.model.Post
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun getPostsFlow(): Flow<List<Post>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<Post>)

    // Dành cho tính năng Refresh (kéo thả từ trên xuống)
    @Query("DELETE FROM posts")
    suspend fun clearAllPosts()

    // Phục vụ Optimistic Update (Like bài viết)
    @Query("UPDATE posts SET isLiked = :isLiked, likeCount = :likeCount WHERE id = :postId")
    suspend fun updatePostLike(postId: String, isLiked: Boolean, likeCount: Int)
}