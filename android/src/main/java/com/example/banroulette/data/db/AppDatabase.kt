package com.example.banroulette.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.banroulette.data.dao.BannedDao
import com.example.banroulette.data.dao.SavedNamesDao
import com.example.banroulette.data.entity.BannedNameEntity
import com.example.banroulette.data.entity.SavedNameEntity

@Database(
    entities = [SavedNameEntity::class, BannedNameEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun savedNamesDao(): SavedNamesDao
    abstract fun bannedDao(): BannedDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ban_roulette.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
