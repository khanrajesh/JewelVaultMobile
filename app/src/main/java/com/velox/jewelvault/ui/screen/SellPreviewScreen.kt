package com.velox.jewelvault.ui.screen

import androidx.compose.runtime.Composable
import com.velox.jewelvault.ui.components.SignatureBox

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp

@Composable
fun SellPreviewScreen() {
    var capturedSignature by remember { mutableStateOf<ImageBitmap?>(null) }

    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        SignatureBox(
            modifier = Modifier.height(200.dp).width(400.dp) .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
            onSignatureCaptured = { bitmap ->
                capturedSignature = bitmap
            }
        )

        Spacer(modifier = Modifier.width(24.dp))

        capturedSignature?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "Captured Signature",
                modifier = Modifier
                    .size(200.dp)
            )
        }
    }
}
