package com.shortsblockerkids.feature.pin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.shortsblockerkids.R
import com.shortsblockerkids.core.security.PinPolicy
import com.shortsblockerkids.core.security.PinValidationResult

@Composable
fun PinSetupScreen(
    onPinCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<PinSetupError?>(null) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.pin_setup_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        PinField(
            value = pin,
            label = stringResource(R.string.pin_input_label),
            onValueChange = {
                pin = it.keepPinDigits()
                error = null
            },
        )
        Spacer(modifier = Modifier.height(12.dp))
        PinField(
            value = confirmation,
            label = stringResource(R.string.pin_confirmation_input_label),
            onValueChange = {
                confirmation = it.keepPinDigits()
                error = null
            },
        )
        Spacer(modifier = Modifier.height(12.dp))
        error?.let { currentError ->
            Text(
                text = currentError.localizedMessage(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                when (PinPolicy.validate(pin, confirmation)) {
                    PinValidationResult.Valid -> onPinCreated(pin)
                    is PinValidationResult.Invalid -> error = pinSetupError(pin)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.pin_setup_submit))
        }
    }
}

@Composable
private fun PinSetupError.localizedMessage(): String =
    stringResource(
        when (this) {
            PinSetupError.INVALID_LENGTH -> R.string.pin_setup_error_invalid_length
            PinSetupError.WEAK_PIN -> R.string.pin_setup_error_weak_pin
            PinSetupError.CONFIRMATION_MISMATCH -> R.string.pin_setup_error_confirmation_mismatch
        },
    )

private fun pinSetupError(pin: String): PinSetupError =
    when {
        !PinPolicy.isVerificationInputComplete(pin) -> PinSetupError.INVALID_LENGTH
        PinPolicy.validate(pin, pin) is PinValidationResult.Invalid -> PinSetupError.WEAK_PIN
        else -> PinSetupError.CONFIRMATION_MISMATCH
    }

@Composable
private fun PinField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun String.keepPinDigits(): String = filter(Char::isDigit).take(6)

private enum class PinSetupError {
    INVALID_LENGTH,
    WEAK_PIN,
    CONFIRMATION_MISMATCH,
}
