package com.example.banroulette.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.banroulette.data.db.AppDatabase
import com.example.banroulette.data.entity.BannedNameEntity
import com.example.banroulette.data.entity.SavedNameEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomDaosTest {

    private lateinit var db: AppDatabase
    private lateinit var savedNamesDao: SavedNamesDao
    private lateinit var bannedDao: BannedDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        savedNamesDao = db.savedNamesDao()
        bannedDao = db.bannedDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── SavedNamesDao Tests ──────────────────────────────────────────────────

    @Test
    fun testSavedNamesBasicCrud() {
        assertEquals(0, savedNamesDao.count())

        val now = System.currentTimeMillis()
        val id1 = savedNamesDao.insert(SavedNameEntity(name = "Alice", lastUsedAt = now))
        assertTrue(id1 > 0)
        assertEquals(1, savedNamesDao.count())

        val found = savedNamesDao.findByName("alice") // case-insensitive
        assertNotNull(found)
        assertEquals("Alice", found?.name)

        savedNamesDao.deleteByName("Alice")
        assertEquals(0, savedNamesDao.count())
    }

    @Test
    fun testSavedNamesTwoMinuteInterval() {
        val now = System.currentTimeMillis()
        val threeMinutesAgo = now - 180_000L

        // Old usage
        savedNamesDao.insert(SavedNameEntity(name = "OldParticipant", lastUsedAt = threeMinutesAgo))
        // Recent usages
        savedNamesDao.insert(SavedNameEntity(name = "Zack", lastUsedAt = now))
        savedNamesDao.insert(SavedNameEntity(name = "Ben", lastUsedAt = now - 30_000L))

        val recent = savedNamesDao.findLastUsedNames()
        assertEquals(2, recent.size)
        // Should exclude OldParticipant and return Ben and Zack ordered alphabetically
        assertEquals("Ben", recent[0])
        assertEquals("Zack", recent[1])
    }

    // ── BannedDao Tests ──────────────────────────────────────────────────────

    @Test
    fun testBannedDaoIncrementAndTotal() {
        assertEquals(0, bannedDao.count())
        assertEquals(0, bannedDao.totalBans())

        val now = System.currentTimeMillis()
        bannedDao.insert(BannedNameEntity(name = "Gareth", banCount = 1, bannedAt = now))
        assertEquals(1, bannedDao.count())
        assertEquals(1, bannedDao.totalBans())
        assertEquals(1, bannedDao.getBanCount("gareth"))

        val updatedRows = bannedDao.incrementBan("Gareth", now + 1000L)
        assertEquals(1, updatedRows)
        assertEquals(1, bannedDao.count())
        assertEquals(2, bannedDao.totalBans())
        assertEquals(2, bannedDao.getBanCount("Gareth"))

        val found = bannedDao.findByName("gareth")
        assertNotNull(found)
        assertEquals(2, found?.banCount)

        bannedDao.deleteById(found!!.id)
        assertEquals(0, bannedDao.count())
        assertEquals(0, bannedDao.totalBans())
        assertNull(bannedDao.findByName("gareth"))
    }
}
