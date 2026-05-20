package com.shortsblockerkids.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val colorScheme =
    lightColorScheme(
        primary = Color(0xFF2457A6),
        onPrimary = Color.White,
        secondary = Color(0xFF4B635A),
        tertiary = Color(0xFF76546D),
        background = Color(0xFFF8F9FC),
        surface = Color.White,
        error = Color(0xFFB3261E),
    )

@Composable
fun ShortsBlockerKidsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
