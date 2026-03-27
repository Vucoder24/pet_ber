package com.nvv.petber.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUser(userId: String): Flow<User?>

    @Query("SELECT * FROM pets WHERE ownerId = :userId")
    fun getPets(userId: String): Flow<List<Pet>>

    @Query("SELECT * FROM posts WHERE userId = :userId")
    fun getPosts(userId: String): Flow<List<Post>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPets(pets: List<Pet>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<Post>)

    @Update
    suspend fun updatePost(post: Post)

    @Update
    suspend fun updateUser(user: User)

    @Query("DELETE FROM pets WHERE ownerId = :userId")
    suspend fun deletePetsByUser(userId: String)

    @Query("DELETE FROM posts WHERE userId = :userId")
    suspend fun deletePostsByUser(userId: String)

    @Transaction
    suspend fun syncPetsData(userId: String, pets: List<Pet>) {
        deletePetsByUser(userId)
        insertPets(pets)
    }

    @Transaction
    suspend fun syncPostsData(userId: String, posts: List<Post>) {
        deletePostsByUser(userId)
        insertPosts(posts)
    }
}