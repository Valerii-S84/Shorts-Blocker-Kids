package com.shortsblockerkids.platform.locale

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList

object AppLanguage {
    private val supportedTags = setOf("en", "de", "uk")
    private const val PREFERENCES = "app_language"
    private const val LANGUAGE_TAG = "language_tag"

    fun currentTag(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (!locales.isEmpty) {
                return locales[0].language.takeIf { it in supportedTags } ?: "en"
            }
        }
        return context
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(LANGUAGE_TAG, "en")
            .takeIf { it in supportedTags } ?: "en"
    }

    fun localizedContext(context: Context): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList.forLanguageTags(currentTag(context)))
        return context.createConfigurationContext(configuration)
    }

    fun select(
        activity: Activity,
        languageTag: String,
    ) {
        require(languageTag in supportedTags)
        activity
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE_TAG, languageTag)
            .apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(languageTag)
        } else {
            activity.recreate()
        }
    }
}
