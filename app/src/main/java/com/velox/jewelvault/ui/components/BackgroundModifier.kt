package com.velox.jewelvault.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.velox.jewelvault.ui.components.baseBackground0
import com.velox.jewelvault.ui.components.baseBackground1
import com.velox.jewelvault.ui.components.baseBackground2
import com.velox.jewelvault.ui.components.baseBackground3
import com.velox.jewelvault.ui.components.baseBackground4
import com.velox.jewelvault.ui.components.baseBackground5
import com.velox.jewelvault.ui.components.baseBackground6
import com.velox.jewelvault.ui.components.baseBackground7
import com.velox.jewelvault.ui.components.baseBackground10
import com.velox.jewelvault.ui.components.baseBackground11
import com.velox.jewelvault.ui.components.baseBackground8
import com.velox.jewelvault.ui.components.baseBackground9

fun Modifier.baseBackground0(): Modifier = composed {
    background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
}

fun Modifier.baseBackground1(): Modifier = composed {
    background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
}

fun Modifier.baseBackground2(): Modifier = composed {
    background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
}

fun Modifier.baseBackground3(): Modifier = composed {
    background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
}

fun Modifier.baseBackground4(): Modifier = composed {
    background(MaterialTheme.colorScheme.outlineVariant)
}

fun Modifier.baseBackground5(): Modifier = composed {
    background(Color.White, RoundedCornerShape(8.dp))
}

fun Modifier.baseBackground6(): Modifier = composed {
    background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
}

fun Modifier.baseBackground7(): Modifier = composed {
    background(
        brush = Brush.horizontalGradient(
            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
        ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    )
}

fun Modifier.baseBackground8(): Modifier = composed {
    background(MaterialTheme.colorScheme.surface)
}

fun Modifier.baseBackground9(): Modifier = composed {
    background(MaterialTheme.colorScheme.surfaceVariant)
}

fun Modifier.baseBackground10(): Modifier = composed {
    background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
}

fun Modifier.baseBackground11(): Modifier = composed {
    background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
}

fun Modifier.goldAnimationBackground(): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "aniBackground")

    val t1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "t1",
    )

    val t2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "t2",
    )


    Modifier.drawWithContent {

        val angle1 = (2f * PI.toFloat()) * t1
        val angle2 = (2f * PI.toFloat()) * t2

        val center1 = Offset(
            x = size.width * (0.25f + 0.25f * sin(angle1)),
            y = size.height * (0.40f + 0.22f * cos(angle1 * 1.1f)),
        )
        val center2 = Offset(
            x = size.width * (0.65f + 0.28f * sin(angle2 * 0.9f + 1.7f)),
            y = size.height * (0.60f + 0.24f * cos(angle2 + 0.8f)),
        )

        val radius1 = size.minDimension * 0.35f
        val radius2 = size.minDimension * 0.45f

        val gold1 = Color(0xFFFFF3B0)
        val gold2 = Color(0xFFFFD54F)
        val gold3 = Color(0xFFFFA000)

        val brush1 = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to gold1.copy(alpha = 0.70f),
                0.35f to gold2.copy(alpha = 0.55f),
                0.70f to gold3.copy(alpha = 0.35f),
                1.00f to Color.Transparent,
            ),
            center = center1,
            radius = radius1,
        )

        val brush2 = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to gold2.copy(alpha = 0.50f),
                0.45f to gold3.copy(alpha = 0.35f),
                1.00f to Color.Transparent,
            ),
            center = center2,
            radius = radius2,
        )

        drawRect(color = Color.Transparent)

        val blurPx = 60.dp.toPx()
        val nativePaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }

        val nativeCanvas = drawContext.canvas.nativeCanvas
        val checkpoint = nativeCanvas.saveLayer(0f, 0f, size.width, size.height, nativePaint)
        drawCircle(brush = brush1, radius = radius1, center = center1)
        drawCircle(brush = brush2, radius = radius2, center = center2)
        nativeCanvas.restoreToCount(checkpoint)

        drawContent()
    }
}
