package com.shortsblockerkids.presentation.dashboard

import androidx.annotation.StringRes

data class ProtectedPlatformItemUiModel(
    val platformId: String,
    @param:StringRes val nameRes: Int,
    val packageName: String,
    @param:StringRes val statusRes: Int,
    val isSupported: Boolean,
    val isSelected: Boolean,
    val isEnabled: Boolean,
) {
    val isAvailable: Boolean
        get() = isSupported

    val isClickable: Boolean
        get() = isEnabled
}
