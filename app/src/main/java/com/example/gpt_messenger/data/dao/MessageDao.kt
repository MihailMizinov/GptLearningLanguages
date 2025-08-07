package com.example.gpt_messenger.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gpt_messenger.data.entities.MessageEntity
import com.example.gpt_messenger.data.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE dialogId = :dialogId AND userId = :userId ORDER BY id ASC")
    fun getMessagesForDialogAndUser(dialogId: Int, userId: Int): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE userId = :userId")
    suspend fun deleteMessagesForUser(userId: Int)

    @Query("SELECT * FROM messages WHERE dialogId = :dialogId ORDER BY id ASC")
    fun getMessagesForDialog(dialogId: Int): Flow<List<MessageEntity>>

}
