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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.shortsblockerkids.R
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
    var message by remember { mutableStateOf<PinEntryMessage?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val canSubmit = PinPolicy.isVerificationInputComplete(pin)

    fun submitPin() {
        if (isVerifying) {
            return
        }
        val submittedPin = pin
        if (!PinPolicy.isVerificationInputComplete(submittedPin)) {
            message = PinEntryMessage.Incomplete
            return
        }
        isVerifying = true
        coroutineScope.launch {
            when (val result = onVerifyPin(submittedPin)) {
                PinVerificationResult.Success -> onUnlocked()
                PinVerificationResult.NotConfigured -> message = PinEntryMessage.NotConfigured
                is PinVerificationResult.Failure -> {
                    message = PinEntryMessage.WrongPin(result.remainingAttempts)
                }
                is PinVerificationResult.Locked -> {
                    message = PinEntryMessage.Locked(result.remainingMinutes())
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
            text = stringResource(R.string.pin_entry_title),
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
            label = { Text(stringResource(R.string.pin_input_label)) },
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
        message?.let { currentMessage ->
            Text(
                text = currentMessage.localizedMessage(),
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
            Text(stringResource(R.string.pin_entry_submit))
        }
    }
}

@Composable
private fun PinEntryMessage.localizedMessage(): String =
    when (this) {
        PinEntryMessage.Incomplete -> stringResource(R.string.pin_entry_error_incomplete)
        PinEntryMessage.NotConfigured -> stringResource(R.string.pin_entry_error_not_configured)
        is PinEntryMessage.WrongPin ->
            pluralStringResource(
                R.plurals.pin_entry_attempts_remaining,
                remainingAttempts,
                remainingAttempts,
            )
        is PinEntryMessage.Locked ->
            pluralStringResource(
                R.plurals.pin_entry_lockout_minutes,
                remainingMinutes.toInt(),
                remainingMinutes,
            )
    }

private fun PinVerificationResult.Locked.remainingMinutes(): Long {
    val remainingMillis = (untilMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    return TimeUnit.MILLISECONDS.toMinutes(remainingMillis).coerceAtLeast(1L)
}

private sealed interface PinEntryMessage {
    data object Incomplete : PinEntryMessage

    data object NotConfigured : PinEntryMessage

    data class WrongPin(
        val remainingAttempts: Int,
    ) : PinEntryMessage

    data class Locked(
        val remainingMinutes: Long,
    ) : PinEntryMessage
}
