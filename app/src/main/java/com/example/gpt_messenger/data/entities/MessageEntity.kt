package com.example.gpt_messenger.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dialogId: Int,
    val userId: Int,
    val messageText: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

