package com.example.banroulette.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.banroulette.data.db.AppDatabase
import com.example.core.contract.AbstractBannedRepositoryContractTest
import com.example.core.repository.BannedRepository
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Android contract test verifying that {@link RoomBannedRepository} satisfies
 * the shared {@link AbstractBannedRepositoryContractTest}.
 */
@RunWith(RobolectricTestRunner::class)
class RoomBannedRepositoryContractTest : AbstractBannedRepositoryContractTest() {

    private lateinit var db: AppDatabase

    override fun createRepository(): BannedRepository {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        return RoomBannedRepository(db)
    }

    override fun cleanupRepository(repo: BannedRepository?) {
        if (::db.isInitialized && db.isOpen) {
            db.close()
        }
    }
}
