package com.example.banroulette.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.banroulette.data.db.AppDatabase
import com.example.banroulette.data.repository.RoomBannedRepository
import com.example.banroulette.data.repository.RoomNameRepository
import com.example.core.calculator.SpinCalculator
import com.example.core.geometry.WheelGeometry
import com.example.core.selector.WinnerSelector
import com.example.core.service.BanService
import com.example.core.validator.NameValidator
import com.example.model.BannedName
import com.example.model.SavedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.SecureRandom

enum class AppScreen {
    WHEEL,
    SAVED_NAMES,
    BANNED_HISTORY
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val nameRepository = RoomNameRepository(db)
    private val bannedRepository = RoomBannedRepository(db)
    val banService = BanService(nameRepository, bannedRepository)

    private val winnerSelector = WinnerSelector()
    private val random = SecureRandom()

    // Screen navigation
    private val _currentScreen = MutableStateFlow(AppScreen.WHEEL)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Wheel participants
    private val _participants = MutableStateFlow<List<String>>(emptyList())
    val participants: StateFlow<List<String>> = _participants.asStateFlow()

    // Persistent catalogs
    private val _savedNames = MutableStateFlow<List<SavedName>>(emptyList())
    val savedNames: StateFlow<List<SavedName>> = _savedNames.asStateFlow()

    private val _bannedList = MutableStateFlow<List<BannedName>>(emptyList())
    val bannedList: StateFlow<List<BannedName>> = _bannedList.asStateFlow()

    // Wheel animation states
    private val _isSpinning = MutableStateFlow(false)
    val isSpinning: StateFlow<Boolean> = _isSpinning.asStateFlow()

    private val _isHammerStriking = MutableStateFlow(false)
    val isHammerStriking: StateFlow<Boolean> = _isHammerStriking.asStateFlow()

    private val _currentWheelAngle = MutableStateFlow(0f)
    val currentWheelAngle: StateFlow<Float> = _currentWheelAngle.asStateFlow()

    private val _targetWheelAngle = MutableStateFlow(0f)
    val targetWheelAngle: StateFlow<Float> = _targetWheelAngle.asStateFlow()

    // Winner & Dialog states
    private val _currentWinner = MutableStateFlow<String?>(null)
    val currentWinner: StateFlow<String?> = _currentWinner.asStateFlow()

    private val _winnerBanCount = MutableStateFlow(1)
    val winnerBanCount: StateFlow<Int> = _winnerBanCount.asStateFlow()

    private val _showResultDialog = MutableStateFlow(false)
    val showResultDialog: StateFlow<Boolean> = _showResultDialog.asStateFlow()

    // Status / Alert message
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch(Dispatchers.IO) {
            loadSavedNamesInternal()
            loadBannedHistoryInternal()
            loadLastUsedParticipantsInternal()
        }
    }

    private fun loadSavedNamesInternal() {
        val list = banService.savedNames
        _savedNames.value = list
    }

    private fun loadBannedHistoryInternal() {
        val list = banService.bannedHistory
        _bannedList.value = list
    }

    private fun loadLastUsedParticipantsInternal() {
        val lastUsed = banService.lastUsedNames
        if (_participants.value.isEmpty() && lastUsed.isNotEmpty()) {
            _participants.value = lastUsed
        }
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun addParticipant(name: String): Boolean {
        if (_isSpinning.value || _isHammerStriking.value) return false

        val result = NameValidator.validateForAdd(
            name,
            _participants.value.size,
            WheelGeometry.MAX_PARTICIPANTS
        )

        if (!result.isValid) {
            _statusMessage.value = result.errorMessage
            return false
        }

        _participants.value = _participants.value + result.cleanedName
        _statusMessage.value = null
        return true
    }

    fun removeParticipant(index: Int) {
        if (_isSpinning.value || _isHammerStriking.value) return
        val current = _participants.value.toMutableList()
        if (index in current.indices) {
            val removed = current.removeAt(index)
            _participants.value = current
            _statusMessage.value = "Removed \"$removed\"."
        }
    }

    fun clearParticipants() {
        if (_isSpinning.value || _isHammerStriking.value) return
        _participants.value = emptyList()
        _statusMessage.value = null
    }

    fun addParticipantFromSaved(name: String): Boolean {
        return addParticipant(name)
    }

    fun addAllSavedToWheel() {
        if (_isSpinning.value || _isHammerStriking.value) return
        val current = _participants.value.toMutableList()
        for (sn in _savedNames.value) {
            if (current.size >= WheelGeometry.MAX_PARTICIPANTS) break
            current.add(sn.name)
        }
        _participants.value = current
    }

    fun deleteSavedName(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            banService.removeSavedName(id)
            loadSavedNamesInternal()
        }
    }

    fun deleteBannedName(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            banService.restore(id)
            loadBannedHistoryInternal()
        }
    }

    fun clearAllSavedNames() {
        viewModelScope.launch(Dispatchers.IO) {
            banService.clearAllSavedNames()
            loadSavedNamesInternal()
        }
    }

    fun clearAllBannedHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            banService.clearAllBanned()
            loadBannedHistoryInternal()
        }
    }

    fun startSpin(): Boolean {
        if (_isSpinning.value || _isHammerStriking.value) return false
        val count = _participants.value.size
        if (!NameValidator.canSpin(count)) {
            _statusMessage.value = "Need at least 2 participants to spin."
            return false
        }

        val winnerIndex = winnerSelector.selectWinnerIndex(count)
        val winnerName = _participants.value[winnerIndex]
        _currentWinner.value = winnerName

        val offsetFactor = (random.nextDouble() * 2.0) - 1.0
        val offset = SpinCalculator.computeRandomOffset(count, offsetFactor)
        val fullSpins = 5 + random.nextInt(3) // 5 to 7 full revolutions

        val targetAngle = SpinCalculator.computeTargetAngle(
            _currentWheelAngle.value.toDouble(),
            winnerIndex,
            count,
            fullSpins,
            offset
        ).toFloat()

        _targetWheelAngle.value = targetAngle
        _isSpinning.value = true
        _statusMessage.value = "Spinning the wheel…"

        // Persist participants on wheel in background
        val currentNames = _participants.value
        viewModelScope.launch(Dispatchers.IO) {
            banService.recordSpinUsage(currentNames)
            loadSavedNamesInternal()
        }

        return true
    }

    fun onSpinAnimationFinished(finalAngle: Float) {
        _currentWheelAngle.value = (finalAngle % 360f).let { if (it < 0f) it + 360f else it }
        _isSpinning.value = false
        _isHammerStriking.value = true
        _statusMessage.value = "⚡ Ban hammer incoming for ${_currentWinner.value}…"
    }

    fun onHammerStrikeFinished() {
        _isHammerStriking.value = false
        val winner = _currentWinner.value
        if (winner != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val record = banService.ban(winner)
                _winnerBanCount.value = record.banCount
                loadBannedHistoryInternal()
                loadSavedNamesInternal()
                _showResultDialog.value = true
            }
        }
        _statusMessage.value = "🔨 $winner was chosen by the roulette!"
    }

    fun dismissResultDialog() {
        _showResultDialog.value = false
    }

    fun getBanCountFor(name: String): Int {
        return _bannedList.value.firstOrNull { it.name.equals(name, ignoreCase = true) }?.banCount ?: 0
    }
}
