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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.shortsblockerkids.core.security.PinPolicy
import com.shortsblockerkids.core.security.PinValidationResult

@Composable
fun PinSetupScreen(
    onPinCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "Create Parent PIN",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        PinField(
            value = pin,
            label = "PIN",
            onValueChange = {
                pin = it.keepPinDigits()
                error = null
            },
        )
        Spacer(modifier = Modifier.height(12.dp))
        PinField(
            value = confirmation,
            label = "Confirm PIN",
            onValueChange = {
                confirmation = it.keepPinDigits()
                error = null
            },
        )
        Spacer(modifier = Modifier.height(12.dp))
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                when (val result = PinPolicy.validate(pin, confirmation)) {
                    PinValidationResult.Valid -> onPinCreated(pin)
                    is PinValidationResult.Invalid -> error = result.message
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Create PIN")
        }
    }
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
