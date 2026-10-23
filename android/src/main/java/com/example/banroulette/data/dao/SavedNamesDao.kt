package com.example.banroulette.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.banroulette.data.entity.SavedNameEntity

@Dao
interface SavedNamesDao {

    @Query("SELECT * FROM saved_names ORDER BY LOWER(name) ASC, id ASC")
    fun findAll(): List<SavedNameEntity>

    @Query("SELECT * FROM saved_names WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    fun findByName(name: String): SavedNameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: SavedNameEntity): Long

    @Query("UPDATE saved_names SET last_used_at = :lastUsedAt WHERE id = :id")
    fun updateLastUsed(id: Long, lastUsedAt: Long)

    @Query("DELETE FROM saved_names WHERE id = :id")
    fun deleteById(id: Long)

    @Query("DELETE FROM saved_names WHERE LOWER(name) = LOWER(:name)")
    fun deleteByName(name: String)

    @Query("DELETE FROM saved_names")
    fun deleteAll()

    @Query("SELECT COUNT(*) FROM saved_names")
    fun count(): Int

    @Query("""
        SELECT name FROM saved_names
        WHERE last_used_at IS NOT NULL
          AND last_used_at >= ((SELECT MAX(last_used_at) FROM saved_names WHERE last_used_at IS NOT NULL) - 120000)
        ORDER BY LOWER(name) ASC, id ASC
    """)
    fun findLastUsedNames(): List<String>
}
