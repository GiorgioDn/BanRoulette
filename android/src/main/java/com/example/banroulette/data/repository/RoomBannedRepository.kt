package com.example.banroulette.data.repository

import com.example.banroulette.data.db.AppDatabase
import com.example.banroulette.data.entity.BannedNameEntity
import com.example.banroulette.data.entity.SavedNameEntity
import com.example.core.repository.BannedRepository
import com.example.model.BannedName
import com.example.model.Participant
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class RoomBannedRepository(private val db: AppDatabase) : BannedRepository {

    private fun Long.toLocalDateTime(): LocalDateTime {
        return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    override fun findAll(): List<BannedName> {
        return db.bannedDao().findAll().map { entity ->
            BannedName(entity.id, entity.name, entity.banCount, entity.bannedAt.toLocalDateTime())
        }
    }

    override fun ban(participant: Participant): BannedName {
        requireNotNull(participant) { "Participant must not be null" }
        return ban(participant.name)
    }

    override fun ban(name: String): BannedName {
        require(name.isNotBlank()) { "Name must not be blank" }
        val trimmed = name.trim()
        return db.runInTransaction<BannedName> {
            val now = System.currentTimeMillis()
            val rowsUpdated = db.bannedDao().incrementBan(trimmed, now)
            if (rowsUpdated == 0) {
                db.bannedDao().insert(BannedNameEntity(name = trimmed, banCount = 1, bannedAt = now))
            }
            // Ensure name is present in saved_names catalog (like desktop BannedDao)
            val existingSaved = db.savedNamesDao().findByName(trimmed)
            if (existingSaved != null) {
                db.savedNamesDao().updateLastUsed(existingSaved.id, now)
            } else {
                db.savedNamesDao().insert(SavedNameEntity(name = trimmed, lastUsedAt = now))
            }
            val entity = db.bannedDao().findByName(trimmed)
                ?: error("Failed to retrieve banned entity for $trimmed")
            BannedName(entity.id, entity.name, entity.banCount, entity.bannedAt.toLocalDateTime())
        }
    }

    override fun restore(id: Long) {
        db.bannedDao().deleteById(id)
    }

    override fun findByName(name: String?): BannedName? {
        if (name.isNullOrBlank()) return null
        val entity = db.bannedDao().findByName(name.trim()) ?: return null
        return BannedName(entity.id, entity.name, entity.banCount, entity.bannedAt.toLocalDateTime())
    }

    override fun getBanCount(name: String?): Int {
        if (name.isNullOrBlank()) return 0
        return db.bannedDao().getBanCount(name.trim()) ?: 0
    }

    override fun count(): Int {
        return db.bannedDao().count()
    }

    override fun totalBans(): Int {
        return db.bannedDao().totalBans()
    }

    override fun clear() {
        db.bannedDao().deleteAll()
    }
}
