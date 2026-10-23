package com.example.banroulette.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_names",
    indices = [Index(value = ["name"], unique = true)]
)
data class SavedNameEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long = System.currentTimeMillis()
)
