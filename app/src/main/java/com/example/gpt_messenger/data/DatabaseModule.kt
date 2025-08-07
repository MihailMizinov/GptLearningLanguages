package com.example.gpt_messenger.data


import android.content.Context
import androidx.room.Room
import com.example.gpt_messenger.data.database.AppDatabase

object DatabaseModule {
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "gpt_messenger.db"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}
