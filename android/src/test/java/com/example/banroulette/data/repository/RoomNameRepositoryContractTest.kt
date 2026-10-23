package com.example.banroulette.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.banroulette.data.db.AppDatabase
import com.example.core.contract.AbstractNameRepositoryContractTest
import com.example.core.repository.NameRepository
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Android contract test verifying that {@link RoomNameRepository} satisfies
 * the shared {@link AbstractNameRepositoryContractTest}.
 */
@RunWith(RobolectricTestRunner::class)
class RoomNameRepositoryContractTest : AbstractNameRepositoryContractTest() {

    private lateinit var db: AppDatabase

    override fun createRepository(): NameRepository {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        return RoomNameRepository(db)
    }

    override fun cleanupRepository(repo: NameRepository?) {
        if (::db.isInitialized && db.isOpen) {
            db.close()
        }
    }
}
