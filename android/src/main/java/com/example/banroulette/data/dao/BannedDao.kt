package com.example.banroulette.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.banroulette.data.entity.BannedNameEntity

@Dao
interface BannedDao {

    @Query("SELECT * FROM banned_names ORDER BY ban_count DESC, banned_at DESC, id DESC")
    fun findAll(): List<BannedNameEntity>

    @Query("SELECT * FROM banned_names WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    fun findByName(name: String): BannedNameEntity?

    @Query("SELECT * FROM banned_names WHERE id = :id LIMIT 1")
    fun findById(id: Long): BannedNameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: BannedNameEntity): Long

    @Query("UPDATE banned_names SET ban_count = ban_count + 1, banned_at = :bannedAt WHERE LOWER(name) = LOWER(:name)")
    fun incrementBan(name: String, bannedAt: Long): Int

    @Query("DELETE FROM banned_names WHERE id = :id")
    fun deleteById(id: Long)

    @Query("DELETE FROM banned_names")
    fun deleteAll()

    @Query("SELECT COUNT(*) FROM banned_names")
    fun count(): Int

    @Query("SELECT COALESCE(SUM(ban_count), 0) FROM banned_names")
    fun totalBans(): Int

    @Query("SELECT ban_count FROM banned_names WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    fun getBanCount(name: String): Int?
}
