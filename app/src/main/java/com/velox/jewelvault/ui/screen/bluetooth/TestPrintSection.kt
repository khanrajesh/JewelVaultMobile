package com.velox.jewelvault.ui.screen.bluetooth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@Composable
fun TestPrintSection(
    printerAddress: String,
    supportedLanguages: List<String> = emptyList(),
    onTestPrint: (String) -> Unit,
    onTestResult: (String, Boolean) -> Unit = { _, _ -> }
) {
    val protocols = listOf("CPCL", "TSPL", "ESC1", "ESC2", "PPLB", "Generic")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Test Print Protocols",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Protocol buttons in rows
            protocols.chunked(3).forEach { protocolRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    protocolRow.forEach { protocol ->
                        ProtocolTestButton(
                            protocol = protocol,
                            printerAddress = printerAddress,
                            isAlreadySupported = supportedLanguages.contains(protocol),
                            onTestPrint = onTestPrint,
                            onTestResult = onTestResult,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Fill remaining space if row has less than 3 items
                    repeat(3 - protocolRow.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                
                if (protocolRow != protocols.chunked(3).last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun ProtocolTestButton(
    protocol: String,
    printerAddress: String,
    isAlreadySupported: Boolean,
    onTestPrint: (String) -> Unit,
    onTestResult: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var testState by remember { mutableStateOf<TestState>(if (isAlreadySupported) TestState.Pass else TestState.Idle) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    
    Button(
        onClick = {
            if (isAlreadySupported) {
                // Already supported, no need to test again
                return@Button
            }
            testState = TestState.Testing
            onTestPrint(protocol)
            // Show confirmation dialog after test
            showConfirmationDialog = true
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = when (testState) {
                TestState.Idle -> MaterialTheme.colorScheme.primary
                TestState.Testing -> MaterialTheme.colorScheme.secondary
                TestState.Pass -> Color(0xFF4CAF50) // Green
                TestState.Fail -> Color(0xFFF44336) // Red
            }
        ),
        enabled = testState != TestState.Testing
    ) {
        when (testState) {
            TestState.Idle -> {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    protocol,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            TestState.Testing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Testing",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            TestState.Pass -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    protocol,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
            TestState.Fail -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    protocol,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
    
    // Confirmation dialog for test result
    if (showConfirmationDialog) {
        TestResultConfirmationDialog(
            protocol = protocol,
            onPass = {
                testState = TestState.Pass
                onTestResult(protocol, true)
                showConfirmationDialog = false
            },
            onFail = {
                testState = TestState.Fail
                onTestResult(protocol, false)
                showConfirmationDialog = false
            },
            onDismiss = {
                testState = TestState.Idle
                showConfirmationDialog = false
            }
        )
    }
}

@Composable
fun TestResultConfirmationDialog(
    protocol: String,
    onPass: () -> Unit,
    onFail: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Test Print Result",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = "Did the $protocol test print successfully?\n\nPlease check if the printer produced the expected output.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onPass,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pass")
            }
        },
        dismissButton = {
            Button(
                onClick = onFail,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Fail")
            }
        }
    )
}

enum class TestState {
    Idle,
    Testing,
    Pass,
    Fail
}
