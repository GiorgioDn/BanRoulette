package com.example.banroulette.data.repository

import com.example.banroulette.data.db.AppDatabase
import com.example.banroulette.data.entity.SavedNameEntity
import com.example.core.repository.NameRepository
import com.example.model.Participant
import com.example.model.SavedName
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class RoomNameRepository(private val db: AppDatabase) : NameRepository {

    private val dao = db.savedNamesDao()

    private fun Long.toLocalDateTime(): LocalDateTime {
        return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    override fun findAll(): List<Participant> {
        return dao.findAll().map { entity ->
            Participant(entity.id, entity.name, entity.lastUsedAt.toLocalDateTime())
        }
    }

    override fun add(name: String): Participant {
        require(name.isNotBlank()) { "Name cannot be null or blank" }
        val trimmed = name.trim()
        val existing = dao.findByName(trimmed)
        val now = System.currentTimeMillis()
        return if (existing != null) {
            dao.updateLastUsed(existing.id, now)
            Participant(existing.id, existing.name, now.toLocalDateTime())
        } else {
            val id = dao.insert(SavedNameEntity(name = trimmed, lastUsedAt = now))
            Participant(id, trimmed, now.toLocalDateTime())
        }
    }

    override fun remove(id: Long) {
        dao.deleteById(id)
    }

    override fun removeByName(name: String) {
        dao.deleteByName(name.trim())
    }

    override fun findAllSaved(): List<SavedName> {
        return dao.findAll().map { entity ->
            SavedName(entity.id, entity.name, entity.lastUsedAt.toLocalDateTime())
        }
    }

    override fun recordUsage(names: Collection<String>?) {
        if (names.isNullOrEmpty()) return
        val now = System.currentTimeMillis()
        db.runInTransaction {
            for (name in names) {
                if (name.isBlank()) continue
                val trimmed = name.trim()
                val existing = dao.findByName(trimmed)
                if (existing != null) {
                    dao.updateLastUsed(existing.id, now)
                } else {
                    dao.insert(SavedNameEntity(name = trimmed, lastUsedAt = now))
                }
            }
        }
    }

    override fun findLastUsedNames(): List<String> {
        return dao.findLastUsedNames()
    }

    override fun count(): Int {
        return dao.count()
    }

    override fun clear() {
        dao.deleteAll()
    }
}
