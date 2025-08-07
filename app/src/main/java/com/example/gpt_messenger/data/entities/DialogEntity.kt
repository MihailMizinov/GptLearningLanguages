package com.example.gpt_messenger.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "dialogs")
data class DialogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val title: String
)
