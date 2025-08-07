package com.example.gpt_messenger.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.gpt_messenger.data.entities.DialogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DialogDao {
    @Query("SELECT * FROM dialogs ORDER BY id")
    fun getAllDialogs(): Flow<List<DialogEntity>>

    @Query("SELECT * FROM dialogs WHERE id = :id LIMIT 1")
    suspend fun getDialogById(id: Int): DialogEntity?

    @Insert
    suspend fun insertDialog(dialog: DialogEntity): Long

    @Delete
    suspend fun deleteDialog(dialog: DialogEntity)

    @Query("SELECT * FROM dialogs WHERE userId = :userId ORDER BY id")
    fun getDialogsForUser(userId: Int): Flow<List<DialogEntity>>

}