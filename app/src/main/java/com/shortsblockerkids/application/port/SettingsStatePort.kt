package com.shortsblockerkids.application.port

import com.shortsblockerkids.application.model.AppSettingsSnapshot
import kotlinx.coroutines.flow.Flow

fun interface SettingsStatePort {
    fun readSettings(): Flow<AppSettingsSnapshot>
}
