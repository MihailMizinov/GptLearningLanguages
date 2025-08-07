package com.example.gpt_messenger.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.gpt_messenger.data.dao.DialogDao
import com.example.gpt_messenger.data.dao.MessageDao
import com.example.gpt_messenger.data.dao.UserDao
import com.example.gpt_messenger.data.entities.DialogEntity
import com.example.gpt_messenger.data.entities.UserEntity
import com.example.gpt_messenger.data.entities.MessageEntity

@Database(entities = [UserEntity::class, MessageEntity::class, DialogEntity::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun dialogDao(): DialogDao
}

