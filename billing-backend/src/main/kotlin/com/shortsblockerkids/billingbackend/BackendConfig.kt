package com.shortsblockerkids.billingbackend

import java.nio.file.Path

data class BackendConfig(
    val port: Int,
    val packageName: String,
    val productId: String,
    val storeFile: Path,
    val rtdnSharedSecret: String?,
) {
    companion object {
        fun fromEnvironment(env: Map<String, String>): BackendConfig =
            BackendConfig(
                port = env["SBK_BACKEND_PORT"]?.toIntOrNull() ?: 8080,
                packageName = env["SBK_PACKAGE_NAME"] ?: "com.shortsblockerkids",
                productId = env["SBK_PLAY_SUBSCRIPTION_PRODUCT_ID"] ?: "shorts_blocker_kids_monthly",
                storeFile =
                    Path.of(
                        env["SBK_BACKEND_STORE_FILE"] ?: "billing-backend-data.json",
                    ),
                rtdnSharedSecret = env["SBK_RTDN_SHARED_SECRET"]?.takeIf { it.isNotBlank() },
            )
    }
}
