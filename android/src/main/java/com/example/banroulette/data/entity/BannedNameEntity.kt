package com.example.banroulette.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "banned_names",
    indices = [Index(value = ["name"], unique = true)]
)
data class BannedNameEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "ban_count")
    val banCount: Int = 1,

    @ColumnInfo(name = "banned_at")
    val bannedAt: Long = System.currentTimeMillis()
)
