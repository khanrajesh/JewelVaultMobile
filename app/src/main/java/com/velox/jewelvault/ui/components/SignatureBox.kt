package com.velox.jewelvault.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.R
import androidx.compose.ui.graphics.Canvas as ComposeCanvas

@Composable
fun SignatureBox(
    modifier: Modifier = Modifier, check: Boolean, onSignatureCaptured: (ImageBitmap?) -> Unit
) {
    var paths by remember { mutableStateOf(listOf<List<Offset>>()) }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val density = LocalDensity.current

    Column(
        modifier = modifier.padding(5.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painterResource(if (check) R.drawable.check else R.drawable.check_no), null,
                modifier = Modifier.size(45.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    paths = emptyList()
                    currentPath = emptyList()
                    onSignatureCaptured(null)
                }, modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    val widthPx = with(density) { 500.dp.roundToPx() }
                    val heightPx = with(density) { 500.dp.roundToPx() }
                    val bitmap = generateSignatureBitmap(paths, widthPx, heightPx)
                    onSignatureCaptured(bitmap)
                }, modifier = Modifier.weight(1f)
            ) {
                Text("Capture Signature")
            }
        }


        Spacer(modifier = Modifier.height(5.dp))

        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.White, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { offset ->
                    if (offset.x in 0f..size.width.toFloat() && offset.y in 0f..size.height.toFloat()) {
                        currentPath = listOf(offset)
                    }
                }, onDrag = { change, _ ->
                    if (change.position.x in 0f..size.width.toFloat() && change.position.y in 0f..size.height.toFloat()) {
                        currentPath = currentPath + change.position
                    }
                }, onDragEnd = {
                    if (currentPath.isNotEmpty()) {
                        paths = paths + listOf(currentPath)
                        currentPath = emptyList()
                    }
                })
            }

        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                paths.forEach { drawSignaturePath(it) }
                drawSignaturePath(currentPath)
            }
        }


    }
}

private fun DrawScope.drawSignaturePath(points: List<Offset>) {
    if (points.isNotEmpty()) {
        val path = Path()
        path.moveTo(points.first().x, points.first().y)
        for (point in points.drop(1)) {
            path.lineTo(point.x, point.y)
        }
        drawPath(
            path = path, color = Color.Black, style = Stroke(width = 4f)
        )
    }
}

private fun generateSignatureBitmap(
    paths: List<List<Offset>>, widthPx: Int, heightPx: Int
): ImageBitmap {
    val bitmap = ImageBitmap(widthPx, heightPx)
    val canvas = ComposeCanvas(bitmap)
    val paint = Paint().apply {
        color = Color.Black
        style = PaintingStyle.Stroke
        strokeWidth = 4f
    }

    paths.forEach { points ->
        if (points.isNotEmpty()) {
            val path = Path()
            path.moveTo(points.first().x, points.first().y)
            for (point in points.drop(1)) {
                path.lineTo(point.x, point.y)
            }
            canvas.drawPath(path, paint)
        }
    }
    return bitmap
}


