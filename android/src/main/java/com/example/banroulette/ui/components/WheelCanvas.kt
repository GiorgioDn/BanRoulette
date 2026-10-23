package com.example.banroulette.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.geometry.WheelGeometry
import com.example.core.geometry.WheelPalette

@Composable
fun WheelView(
    participants: List<String>,
    isSpinning: Boolean,
    targetAngle: Float,
    currentAngle: Float,
    onSpinAnimationFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    impactShakeOffset: Offset = Offset.Zero,
    onWheelClick: () -> Unit = {}
) {
    val rotationAnimatable = remember { Animatable(currentAngle) }

    LaunchedEffect(isSpinning, targetAngle) {
        if (isSpinning) {
            rotationAnimatable.animateTo(
                targetValue = targetAngle,
                animationSpec = tween(
                    durationMillis = 5000,
                    easing = FastOutSlowInEasing
                )
            )
            onSpinAnimationFinished(rotationAnimatable.value)
        } else {
            // Keep in sync when not spinning
            if (rotationAnimatable.value != currentAngle) {
                rotationAnimatable.snapTo(currentAngle)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (participants.size < 2) {
            // Empty placeholder state
            Box(
                modifier = Modifier
                    .fillMaxSize(0.9f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (participants.isEmpty())
                        "Add names to load the roulette"
                    else
                        "Add at least one more name",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            // 1. Rotating Wheel Canvas with impact shake offset
            Canvas(
                modifier = Modifier
                    .fillMaxSize(0.88f)
                    .graphicsLayer {
                        translationX = impactShakeOffset.x
                        translationY = impactShakeOffset.y
                        rotationZ = rotationAnimatable.value
                    }
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isSpinning && participants.size >= 2,
                        onClick = onWheelClick
                    )
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f
                val count = participants.size

                drawWheelSlices(
                    participants = participants,
                    radius = radius,
                    center = center
                )
            }

            // 2. Stationary 12 o'clock pointer (stays fixed at top)
            Canvas(
                modifier = Modifier
                    .fillMaxSize(0.88f)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f
                drawPointer(radius = radius, center = center)
            }
        }
    }
}

private fun DrawScope.drawWheelSlices(
    participants: List<String>,
    radius: Float,
    center: Offset
) {
    val count = participants.size
    val palette = WheelPalette.getInstance()
    val density = this

    // Slices
    for (i in 0 until count) {
        val pathString = WheelGeometry.slicePath(i, count, radius.toDouble())
        val slicePath = PathParser().parsePathString(pathString).toPath()
        val sliceHex = palette.getSliceColor(i)
        val sliceColor = Color(android.graphics.Color.parseColor(sliceHex))

        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.translate(center.x, center.y)

        drawPath(
            path = slicePath,
            color = sliceColor
        )

        // Radial divider line
        val dividerStr = WheelGeometry.dividerPath(i, count, radius.toDouble())
        val dividerPath = PathParser().parsePathString(dividerStr).toPath()
        drawPath(
            path = dividerPath,
            color = Color.White.copy(alpha = 0.45f),
            style = Stroke(width = with(density) { 2.dp.toPx() })
        )

        // Participant label
        val labelPos = WheelGeometry.labelPosition(i, count, radius.toDouble())
        val fontSize = WheelGeometry.adaptiveFontSize(count).toFloat()
        val textColorHex = WheelPalette.getContrastTextColor(sliceHex)
        val textColorInt = android.graphics.Color.parseColor(textColorHex)

        val paint = Paint().apply {
            isAntiAlias = true
            color = textColorInt
            textSize = fontSize * with(density) { 1.35.sp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(3f, 1f, 1f, android.graphics.Color.argb(90, 0, 0, 0))
        }

        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.translate(labelPos.x.toFloat(), labelPos.y.toFloat())
        drawContext.canvas.nativeCanvas.rotate(labelPos.rotationDegrees.toFloat())

        val truncatedName = WheelGeometry.truncate(participants[i])
        // Center text vertically
        val textBounds = android.graphics.Rect()
        paint.getTextBounds(truncatedName, 0, truncatedName.length, textBounds)
        val yOffset = textBounds.height() / 2f

        drawContext.canvas.nativeCanvas.drawText(
            truncatedName,
            0f,
            yOffset,
            paint
        )

        drawContext.canvas.nativeCanvas.restore()
        drawContext.canvas.nativeCanvas.restore()
    }

    // Outer wheel rim
    drawCircle(
        color = Color(0xFF0F172A),
        radius = radius - with(density) { 2.dp.toPx() },
        center = center,
        style = Stroke(width = with(density) { 4.dp.toPx() })
    )

    // Decorative Center Hub
    val hub = WheelGeometry.centerHub(radius.toDouble())
    val outerHubColor = Color(android.graphics.Color.parseColor(palette.hubOuterColor))
    val innerHubColor = Color(android.graphics.Color.parseColor(palette.hubInnerColor))

    drawCircle(
        color = outerHubColor,
        radius = hub.outerRadius.toFloat(),
        center = center
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.25f),
        radius = hub.outerRadius.toFloat(),
        center = center,
        style = Stroke(width = with(density) { 2.dp.toPx() })
    )
    drawCircle(
        color = innerHubColor,
        radius = hub.innerRadius.toFloat(),
        center = center
    )
}

private fun DrawScope.drawPointer(
    radius: Float,
    center: Offset
) {
    val palette = WheelPalette.getInstance()
    val pointerHex = palette.pointerColor
    val pointerColor = Color(android.graphics.Color.parseColor(pointerHex))

    val pathString = WheelGeometry.pointerPath(radius.toDouble())
    val pointerPath = PathParser().parsePathString(pathString).toPath()

    drawContext.canvas.nativeCanvas.save()
    drawContext.canvas.nativeCanvas.translate(center.x, center.y)

    // Pointer shadow
    drawPath(
        path = pointerPath,
        color = Color.Black.copy(alpha = 0.35f),
        style = Stroke(width = 6f)
    )

    // Pointer body
    drawPath(
        path = pointerPath,
        color = pointerColor
    )

    // Pointer border
    drawPath(
        path = pointerPath,
        color = Color.White.copy(alpha = 0.9f),
        style = Stroke(width = 3f)
    )

    drawContext.canvas.nativeCanvas.restore()
}
