package com.shortsblockerkids.presentation.dashboard

import androidx.annotation.StringRes
import com.shortsblockerkids.R

internal object DashboardPlatformUiMapper {
    fun create(input: DashboardStateInput): DashboardPlatformsUiModel {
        val items =
            input.platforms.map { platform ->
                val support = platform.supportStatusName.toPlatformSupport()
                ProtectedPlatformItemUiModel(
                    platformId = platform.platformId,
                    nameRes = platform.nameRes,
                    packageName = platform.packageName,
                    statusRes = support.statusRes,
                    isSupported = support.isSupported,
                    isSelected =
                        support.isSupported &&
                            platform.platformId in input.protection.enabledPlatformIds,
                    isEnabled = support.isSupported,
                )
            }
        return DashboardPlatformsUiModel(
            protected = items.filter(ProtectedPlatformItemUiModel::isSupported),
            unsupported = items.filterNot(ProtectedPlatformItemUiModel::isSupported),
        )
    }

    private fun String.toPlatformSupport(): PlatformSupport =
        when (this) {
            PLATFORM_SUPPORTED ->
                PlatformSupport(
                    isSupported = true,
                    statusRes = R.string.platform_status_supported,
                )
            PLATFORM_SUPPORTED_NEEDS_QA ->
                PlatformSupport(
                    isSupported = true,
                    statusRes = R.string.platform_status_supported_needs_qa,
                )
            else ->
                PlatformSupport(
                    isSupported = false,
                    statusRes = R.string.platform_status_not_supported,
                )
        }

    private data class PlatformSupport(
        val isSupported: Boolean,
        @param:StringRes val statusRes: Int,
    )

    private const val PLATFORM_SUPPORTED = "SUPPORTED"
    private const val PLATFORM_SUPPORTED_NEEDS_QA =
        "SUPPORTED_BY_CODE_NEEDS_REAL_DEVICE_QA"
}
