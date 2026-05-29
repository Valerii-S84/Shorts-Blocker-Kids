package com.shortsblockerkids.feature.pin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.shortsblockerkids.core.security.PinPolicy
import com.shortsblockerkids.core.security.PinVerificationResult
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun PinEntryScreen(
    onVerifyPin: suspend (String) -> PinVerificationResult,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val canSubmit = PinPolicy.isVerificationInputComplete(pin)

    fun submitPin() {
        if (isVerifying) {
            return
        }
        val submittedPin = pin
        if (!PinPolicy.isVerificationInputComplete(submittedPin)) {
            message = "Enter the 4-6 digit PIN."
            return
        }
        isVerifying = true
        coroutineScope.launch {
            when (val result = onVerifyPin(submittedPin)) {
                PinVerificationResult.Success -> onUnlocked()
                PinVerificationResult.NotConfigured -> message = "PIN is not configured."
                is PinVerificationResult.Failure -> {
                    message = "Wrong PIN. ${result.remainingAttempts} attempts left."
                }
                is PinVerificationResult.Locked -> {
                    message = "PIN locked. Try again in ${result.remainingMinutes()} min."
                }
            }
            isVerifying = false
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "Enter Parent PIN",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = {
                pin = it.filter(Char::isDigit).take(6)
                message = null
            },
            label = { Text("PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { submitPin() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        message?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { submitPin() },
            enabled = canSubmit && !isVerifying,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Unlock")
        }
    }
}

private fun PinVerificationResult.Locked.remainingMinutes(): Long {
    val remainingMillis = (untilMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    return TimeUnit.MILLISECONDS.toMinutes(remainingMillis).coerceAtLeast(1L)
}
