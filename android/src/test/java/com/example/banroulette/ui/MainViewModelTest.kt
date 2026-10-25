package com.example.banroulette.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.room.Room
import com.example.banroulette.data.db.AppDatabase
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application
    private lateinit var inMemoryDb: AppDatabase
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        inMemoryDb = Room.inMemoryDatabaseBuilder(
            app,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        viewModel = MainViewModel(app, testDispatcher, inMemoryDb)
    }

    @After
    fun tearDown() {
        if (::inMemoryDb.isInitialized && inMemoryDb.isOpen) {
            inMemoryDb.close()
        }
        Dispatchers.resetMain()
    }

    @Test
    fun testAddParticipantValidation() {
        // Blank name should fail
        val blankAdded = viewModel.addParticipant("   ")
        assertFalse(blankAdded)
        assertNotNull(viewModel.statusMessage.value)
        assertTrue(viewModel.participants.value.isEmpty())

        // Valid name should succeed
        val validAdded = viewModel.addParticipant("Alice")
        assertTrue(validAdded)
        assertEquals(listOf("Alice"), viewModel.participants.value)
        assertEquals(null, viewModel.statusMessage.value)

        // Second participant
        viewModel.addParticipant("Bob")
        assertEquals(listOf("Alice", "Bob"), viewModel.participants.value)
    }

    @Test
    fun testRemoveAndClearParticipants() {
        viewModel.addParticipant("Alice")
        viewModel.addParticipant("Bob")
        assertEquals(2, viewModel.participants.value.size)

        viewModel.removeParticipant(0)
        assertEquals(listOf("Bob"), viewModel.participants.value)

        viewModel.clearParticipants()
        assertTrue(viewModel.participants.value.isEmpty())
    }

    @Test
    fun testSpinRequiresAtLeastTwoParticipants() {
        // 0 participants
        assertFalse(viewModel.startSpin())

        // 1 participant
        viewModel.addParticipant("Solo")
        assertFalse(viewModel.startSpin())
        assertEquals("Need at least 2 participants to spin.", viewModel.statusMessage.value)

        // 2 participants
        viewModel.addParticipant("Duo")
        val started = viewModel.startSpin()
        assertTrue(started)
        assertTrue(viewModel.isSpinning.value)
        assertNotNull(viewModel.currentWinner.value)
        assertTrue(viewModel.targetWheelAngle.value > 0f)
    }

    @Test
    fun testSpinAndHammerFlow() = runTest(testDispatcher) {
        viewModel.addParticipant("Alice")
        viewModel.addParticipant("Bob")
        viewModel.startSpin()

        val winner = viewModel.currentWinner.value
        assertNotNull(winner)

        // Finish spin animation
        viewModel.onSpinAnimationFinished(1080f)
        assertFalse(viewModel.isSpinning.value)
        assertTrue(viewModel.isHammerStriking.value)

        // Finish hammer strike
        viewModel.onHammerStrikeFinished()
        advanceUntilIdle()

        assertFalse(viewModel.isHammerStriking.value)
        assertTrue(viewModel.showResultDialog.value)
        assertEquals(1, viewModel.winnerBanCount.value)
        assertEquals(1, viewModel.getBanCountFor(winner!!))

        // Dismiss dialog
        viewModel.dismissResultDialog()
        assertFalse(viewModel.showResultDialog.value)
    }

    @Test
    fun testNavigationScreens() {
        assertEquals(AppScreen.SPIN, viewModel.currentScreen.value)

        viewModel.setScreen(AppScreen.PARTICIPANTS)
        assertEquals(AppScreen.PARTICIPANTS, viewModel.currentScreen.value)

        viewModel.setScreen(AppScreen.SAVED_NAMES)
        assertEquals(AppScreen.SAVED_NAMES, viewModel.currentScreen.value)

        viewModel.setScreen(AppScreen.BANNED_HISTORY)
        assertEquals(AppScreen.BANNED_HISTORY, viewModel.currentScreen.value)

        viewModel.setScreen(AppScreen.SPIN)
        assertEquals(AppScreen.SPIN, viewModel.currentScreen.value)
    }
}
