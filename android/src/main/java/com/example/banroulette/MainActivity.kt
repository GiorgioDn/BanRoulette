package com.example.banroulette

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.banroulette.ui.AppScreen
import com.example.banroulette.ui.MainViewModel
import com.example.banroulette.ui.components.BanResultDialog
import com.example.banroulette.ui.screens.BannedHistoryScreen
import com.example.banroulette.ui.screens.SavedNamesScreen
import com.example.banroulette.ui.screens.WheelScreen
import com.example.banroulette.ui.theme.BanRouletteTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BanRouletteTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val savedNames by viewModel.savedNames.collectAsState()
    val bannedList by viewModel.bannedList.collectAsState()
    val isSpinning by viewModel.isSpinning.collectAsState()
    val isHammerStriking by viewModel.isHammerStriking.collectAsState()
    val currentAngle by viewModel.currentWheelAngle.collectAsState()
    val targetAngle by viewModel.targetWheelAngle.collectAsState()
    val winnerName by viewModel.currentWinner.collectAsState()
    val winnerBanCount by viewModel.winnerBanCount.collectAsState()
    val showResultDialog by viewModel.showResultDialog.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    // Handle back button: return to Wheel screen if currently in Saved or Banned
    BackHandler(enabled = currentScreen != AppScreen.WHEEL) {
        viewModel.setScreen(AppScreen.WHEEL)
    }

    val totalBans = bannedList.sumOf { it.banCount }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.WHEEL,
                    onClick = { viewModel.setScreen(AppScreen.WHEEL) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Wheel") },
                    label = { Text("Wheel") }
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.SAVED_NAMES,
                    onClick = { viewModel.setScreen(AppScreen.SAVED_NAMES) },
                    icon = {
                        if (savedNames.isNotEmpty()) {
                            BadgedBox(badge = { Badge { Text("${savedNames.size}") } }) {
                                Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Saved Names")
                            }
                        } else {
                            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Saved Names")
                        }
                    },
                    label = { Text("Saved") }
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.BANNED_HISTORY,
                    onClick = { viewModel.setScreen(AppScreen.BANNED_HISTORY) },
                    icon = {
                        if (totalBans > 0) {
                            BadgedBox(badge = { Badge { Text("$totalBans") } }) {
                                Icon(Icons.Default.Block, contentDescription = "Banned History")
                            }
                        } else {
                            Icon(Icons.Default.Block, contentDescription = "Banned History")
                        }
                    },
                    label = { Text("Banned") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.WHEEL -> {
                    WheelScreen(
                        participants = participants,
                        isSpinning = isSpinning,
                        isHammerStriking = isHammerStriking,
                        targetAngle = targetAngle,
                        currentAngle = currentAngle,
                        statusMessage = statusMessage,
                        getBanCountFor = { viewModel.getBanCountFor(it) },
                        onAddParticipant = { viewModel.addParticipant(it) },
                        onRemoveParticipant = { viewModel.removeParticipant(it) },
                        onClearParticipants = { viewModel.clearParticipants() },
                        onSpinClick = { viewModel.startSpin() },
                        onSpinAnimationFinished = { viewModel.onSpinAnimationFinished(it) },
                        onHammerStrikeFinished = { viewModel.onHammerStrikeFinished() }
                    )
                }

                AppScreen.SAVED_NAMES -> {
                    SavedNamesScreen(
                        savedNames = savedNames,
                        onAddNameToWheel = { viewModel.addParticipantFromSaved(it) },
                        onDeleteSavedName = { viewModel.deleteSavedName(it) },
                        onAddAllToWheel = { viewModel.addAllSavedToWheel() },
                        onClearAllSaved = { viewModel.clearAllSavedNames() }
                    )
                }

                AppScreen.BANNED_HISTORY -> {
                    BannedHistoryScreen(
                        bannedList = bannedList,
                        onDeleteBannedName = { viewModel.deleteBannedName(it) },
                        onClearAllBanned = { viewModel.clearAllBannedHistory() }
                    )
                }
            }

            BanResultDialog(
                show = showResultDialog,
                winnerName = winnerName,
                banCount = winnerBanCount,
                onDismiss = { viewModel.dismissResultDialog() }
            )
        }
    }
}
