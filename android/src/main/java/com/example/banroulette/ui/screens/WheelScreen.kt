package com.example.banroulette.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.banroulette.ui.components.HammerOverlay
import com.example.banroulette.ui.components.WheelView

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WheelScreen(
    participants: List<String>,
    isSpinning: Boolean,
    isHammerStriking: Boolean,
    targetAngle: Float,
    currentAngle: Float,
    statusMessage: String?,
    getBanCountFor: (String) -> Int,
    onAddParticipant: (String) -> Boolean,
    onRemoveParticipant: (Int) -> Unit,
    onClearParticipants: () -> Unit,
    onSpinClick: () -> Unit,
    onSpinAnimationFinished: (Float) -> Unit,
    onHammerStrikeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nameInputText by remember { mutableStateOf("") }
    var impactShakeOffset by remember { mutableStateOf(Offset.Zero) }

    val handleAddName = {
        if (nameInputText.isNotBlank()) {
            val added = onAddParticipant(nameInputText)
            if (added) {
                nameInputText = ""
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Name Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = nameInputText,
                onValueChange = { nameInputText = it },
                label = { Text("Participant name") },
                singleLine = true,
                enabled = !isSpinning && !isHammerStriking,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { handleAddName() }
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = handleAddName,
                enabled = !isSpinning && !isHammerStriking && nameInputText.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Active Participants Chips FlowRow
        if (participants.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Participants (${participants.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = onClearParticipants,
                    enabled = !isSpinning && !isHammerStriking,
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear all",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                participants.forEachIndexed { index, name ->
                    val banCount = getBanCountFor(name)
                    ParticipantChip(
                        name = name,
                        banCount = banCount,
                        onRemove = { onRemoveParticipant(index) },
                        enabled = !isSpinning && !isHammerStriking
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Wheel and Hammer Overlay Stack
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            contentAlignment = Alignment.Center
        ) {
            WheelView(
                participants = participants,
                isSpinning = isSpinning,
                targetAngle = targetAngle,
                currentAngle = currentAngle,
                onSpinAnimationFinished = onSpinAnimationFinished,
                impactShakeOffset = impactShakeOffset,
                onWheelClick = {
                    if (!isSpinning && !isHammerStriking && participants.size >= 2) {
                        onSpinClick()
                    }
                }
            )

            HammerOverlay(
                isStriking = isHammerStriking,
                onImpactShake = { impactShakeOffset = it },
                onStrikeFinished = onHammerStrikeFinished
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Spin Button
        Button(
            onClick = onSpinClick,
            enabled = !isSpinning && !isHammerStriking && participants.size >= 2,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6366F1),
                disabledContainerColor = Color(0xFF6366F1).copy(alpha = 0.35f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Spin")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSpinning) "Spinning…" else "⚡ Spin!",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
        }

        // Status Message Text
        if (!statusMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ParticipantChip(
    name: String,
    banCount: Int,
    onRemove: () -> Unit,
    enabled: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            if (banCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${banCount}×",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = onRemove,
                enabled = enabled,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove $name",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
