package com.shortsblockerkids.billingbackend

import com.google.auth.oauth2.GoogleCredentials
import java.nio.file.Files

object GooglePlayAuth {
    fun load(config: BackendConfig): GoogleCredentials {
        val credentials =
            config.googleCredentialsFile?.let { path ->
                Files.newInputStream(path).use(GoogleCredentials::fromStream)
            } ?: GoogleCredentials.getApplicationDefault()
        return credentials.createScoped(listOf(ANDROID_PUBLISHER_SCOPE))
    }

    private const val ANDROID_PUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher"
}
