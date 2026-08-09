package com.amj_pos.data.local.db

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "sari_sari_pos_db"
            ).fallbackToDestructiveMigration() // For development; use migrations for production
            .build()
            INSTANCE = instance
            instance
        }
    }
}
