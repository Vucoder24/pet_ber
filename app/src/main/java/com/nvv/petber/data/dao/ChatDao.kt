package com.nvv.petber.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nvv.petber.data.model.Conversation
import com.nvv.petber.data.model.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(conversations: List<Conversation>): List<Long>

    @Query("""
            UPDATE conversations 
            SET lastMessageContent = :content, 
                lastMessageAt = :at, 
                lastMessageSenderId = :senderId,
                otherUserName = :name,
                otherUserAvatar = :avatar,
                isSeen = 0
            WHERE conversationId = :id 
            AND (
                lastMessageAt IS NULL 
                OR lastMessageAt != :at
                OR lastMessageContent != :content
            )
        """)
    suspend fun updateIfChanged(
        id: String, content: String?, at: String?,
        senderId: String?, name: String?, avatar: String?
    )

    @Transaction
    suspend fun upsertConversations(conversations: List<Conversation>) {
        val results = insertIgnore(conversations)
        conversations.forEachIndexed { index, item ->
            if (results[index] == -1L) {
                updateIfChanged(
                    item.conversationId, item.lastMessageContent,
                    item.lastMessageAt, item.lastMessageSenderId,
                    item.otherUserName, item.otherUserAvatar
                )
            }
        }
    }

    @Query("UPDATE conversations SET isSeen = 1 WHERE conversationId = :id")
    suspend fun markAsRead(id: String)

    @Query("SELECT * FROM conversations ORDER BY lastMessageAt DESC")
    fun observeConversations(): Flow<List<Conversation>>

    @Query("DELETE FROM conversations WHERE conversationId = :id")
    suspend fun deleteConversation(id: String)

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()

    // Messages
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY createdAt DESC")
    fun observeMessages(convId: String): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE conversationId = :convId")
    suspend fun deleteMessagesByConversation(convId: String)
}