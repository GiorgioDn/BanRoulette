package com.example.banroulette.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.banroulette.R
import kotlinx.coroutines.delay

@Composable
fun HammerOverlay(
    isStriking: Boolean,
    onImpactShake: (Offset) -> Unit,
    onStrikeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isStriking) return

    val hammerAngle = remember { Animatable(0f) }
    val hammerTranslateX = remember { Animatable(60f) }
    val hammerTranslateY = remember { Animatable(-220f) }
    val hammerAlpha = remember { Animatable(0f) }
    val hammerScale = remember { Animatable(1.1f) }

    LaunchedEffect(isStriking) {
        if (isStriking) {
            // Initial appearance
            hammerAngle.snapTo(-10f)
            hammerTranslateX.snapTo(80f)
            hammerTranslateY.snapTo(-200f)
            hammerAlpha.snapTo(1f)

            // Step 1: Wind-up (raise hammer back)
            hammerAngle.animateTo(
                targetValue = -45f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
            hammerTranslateY.animateTo(
                targetValue = -240f,
                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
            )

            // Step 2: Strike! (violent forward swing down onto the wheel)
            hammerAngle.animateTo(
                targetValue = 35f,
                animationSpec = tween(durationMillis = 110, easing = FastOutLinearInEasing)
            )
            hammerTranslateY.animateTo(
                targetValue = -40f,
                animationSpec = tween(durationMillis = 110, easing = FastOutLinearInEasing)
            )
            hammerTranslateX.animateTo(
                targetValue = 20f,
                animationSpec = tween(durationMillis = 110, easing = FastOutLinearInEasing)
            )

            // Step 3: Impact Shake! (multi-frame rapid vibration)
            onImpactShake(Offset(-16f, 10f))
            delay(35)
            onImpactShake(Offset(14f, -12f))
            delay(35)
            onImpactShake(Offset(-10f, 6f))
            delay(35)
            onImpactShake(Offset(8f, -6f))
            delay(35)
            onImpactShake(Offset(-4f, 2f))
            delay(35)
            onImpactShake(Offset.Zero)

            // Step 4: Rebound / fade out
            hammerAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
            )

            // Trigger completion
            onStrikeFinished()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_hammer),
            contentDescription = "Ban Hammer",
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer {
                    alpha = hammerAlpha.value
                    rotationZ = hammerAngle.value
                    translationX = hammerTranslateX.value
                    translationY = hammerTranslateY.value
                    scaleX = hammerScale.value
                    scaleY = hammerScale.value
                    // Pivot at the bottom-right of the handle for realistic swing
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.9f)
                }
        )
    }
}
