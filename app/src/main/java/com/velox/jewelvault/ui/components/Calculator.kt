package com.velox.jewelvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.objecthunter.exp4j.ExpressionBuilder
import com.velox.jewelvault.utils.to3FString

@Composable
fun CalculatorScreen(modifier: Modifier) {
    var expression by remember { mutableStateOf("") }

    // Layout to calculate button size based on screen
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val buttonRows = listOf(
            listOf("7", "8", "9", "/"),
            listOf("4", "5", "6", "*"),
            listOf("1", "2", "3", "-"),
            listOf(".", "0", "%", "+"),
            listOf("C", "=")
        )

        val rowCount = buttonRows.size
        val buttonHeight = maxHeight / (rowCount + 1)

        Column(modifier = Modifier.fillMaxSize()) {
            // Result Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = expression,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, end = 8.dp),
                    fontSize = (buttonHeight.value * 0.4).sp,
                    textAlign = TextAlign.End,
                    maxLines = 2
                )

                Box( modifier = Modifier
                    .bounceClick {
                        if (expression.isNotEmpty()) {
                            expression = expression.dropLast(1)
                        }
                    }.background(MaterialTheme.colorScheme.primary,ButtonDefaults.shape)
                    .size(buttonHeight * 0.8f), contentAlignment = Alignment.Center) {

                    Text(text = "⌫", fontSize = (buttonHeight.value * 0.3).sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }

            }


            // Buttons Grid
            for (row in buttonRows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    val buttonWidth = this@BoxWithConstraints.maxWidth / row.size
                    for (label in row) {
                        Button(
                            onClick = {
                                expression = when (label) {
                                    "C" -> ""
                                    "=" -> try {
                                        evaluateExpression(expression)
                                    } catch (e: Exception) {
                                        "Error"
                                    }

                                    "%" -> expression + "/100"
                                    else -> expression + label
                                }
                            },
                            modifier = Modifier
                                .width(buttonWidth)
                                .height(buttonHeight)
                                .padding(2.dp)
                        ) {
                            Text(text = label, fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

// Evaluate arithmetic expression and return float result with 2 decimal places
fun evaluateExpression(expr: String): String {
    return try {
        val result = ExpressionBuilder(expr).build().evaluate()
        result.to3FString()
    } catch (e: Exception) {
        "Error"
    }
}