package com.shortsblockerkids.e2e

import android.content.ComponentName
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shortsblockerkids.R
import com.shortsblockerkids.accessibility.ShortsBlockerAccessibilityService
import com.shortsblockerkids.core.tamper.TamperProtectionReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocaleMatrixInstrumentedTest {
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun localeMatrixResolvesCriticalSurfaces() {
        LOCALE_EXPECTATIONS.forEach { expectation ->
            assertCriticalSurfaces(expectation)
        }
    }

    @Test
    fun formattedResourcesPreserveArgumentsAndGooglePlayPrice() {
        LOCALE_EXPECTATIONS.forEach { expectation ->
            val resources = resourcesFor(expectation.languageTag)
            val formattedPrice = "€\u00a04,99"

            assertEquals(
                expectation.message("formatted price"),
                "${expectation.availablePricePrefix}$formattedPrice",
                resources.getString(R.string.billing_status_available_price, formattedPrice),
            )
            assertEquals(
                expectation.message("second positional integer"),
                "BILLING_UNAVAILABLE (42).",
                resources.getString(
                    R.string.billing_error_with_response_code,
                    "BILLING_UNAVAILABLE",
                    42,
                ),
            )
            assertTrue(
                expectation.message("Google Play formattedPrice changed"),
                resources
                    .getString(R.string.subscription_terms_with_price, formattedPrice)
                    .contains(formattedPrice),
            )
        }
    }

    @Test
    fun pluralMatrixUsesEnglishGermanUkrainianAndFallbackRules() {
        PLURAL_EXPECTATIONS.forEach { expectation ->
            val resources = resourcesFor(expectation.languageTag)
            expectation.values.forEach { (quantity, expected) ->
                assertEquals(
                    expectation.message("quantity=$quantity"),
                    expected,
                    resources.getQuantityString(
                        R.plurals.temporary_allow_duration_minutes,
                        quantity,
                        quantity,
                    ),
                )
            }
        }
    }

    private fun assertCriticalSurfaces(expectation: LocaleExpectation) {
        val resources = resourcesFor(expectation.languageTag)
        val packageManager = targetContext.packageManager
        val accessibilityServiceInfo =
            packageManager.getServiceInfo(
                ComponentName(targetContext, ShortsBlockerAccessibilityService::class.java),
                0,
            )
        val deviceAdminInfo =
            packageManager.getReceiverInfo(
                ComponentName(targetContext, TamperProtectionReceiver::class.java),
                0,
            )

        assertEquals(R.string.app_name, targetContext.applicationInfo.labelRes)
        assertEquals(R.string.accessibility_service_label, accessibilityServiceInfo.labelRes)
        assertEquals(R.string.tamper_protection_label, deviceAdminInfo.labelRes)
        assertEquals(R.string.tamper_protection_description, deviceAdminInfo.descriptionRes)
        expectation.surfaces.forEach { (resourceId, expected) ->
            assertEquals(
                expectation.message(targetContext.resources.getResourceEntryName(resourceId)),
                expected,
                resources.getString(resourceId),
            )
        }
    }

    private fun resourcesFor(languageTag: String): Resources {
        val configuration =
            Configuration(targetContext.resources.configuration).apply {
                setLocales(LocaleList.forLanguageTags(languageTag))
            }
        return targetContext.createConfigurationContext(configuration).resources
    }

    private data class LocaleExpectation(
        val languageTag: String,
        val availablePricePrefix: String,
        val surfaces: Map<Int, String>,
    ) {
        fun message(assertion: String): String = "$languageTag: $assertion"
    }

    private data class PluralExpectation(
        val languageTag: String,
        val values: Map<Int, String>,
    ) {
        fun message(assertion: String): String = "$languageTag: $assertion"
    }

    private companion object {
        val ENGLISH_SURFACES =
            mapOf(
                R.string.app_name to "Shorts Blocker Kids",
                R.string.accessibility_service_label to "Shorts Blocker Kids Protection",
                R.string.accessibility_service_description to
                    "Detects YouTube Shorts, TikTok short-video feed, Instagram Reels, and Facebook Reels " +
                    "locally when protection is enabled and closes the blocking screen after you leave them.",
                R.string.tamper_protection_label to "Shorts Blocker Kids Tamper Protection",
                R.string.tamper_protection_description to
                    "Optional parent-controlled Device Admin protection that makes uninstall harder while active.",
                R.string.welcome_start to "Start",
                R.string.pin_setup_title to "Create Parent PIN",
                R.string.pin_entry_title to "Enter Parent PIN",
                R.string.temporary_allow_title to "Allow short videos",
                R.string.dashboard_title to "Short Video Protection",
                R.string.blocking_overlay_title to "Short video blocked",
                R.string.billing_status_connecting to "Connecting to Google Play Billing.",
                R.string.accessibility_disclosure_title to "Accessibility Permission",
                R.string.privacy_policy_title to "Privacy Policy",
                R.string.tamper_protection_disclosure_title to "Tamper Protection",
            )

        val GERMAN_SURFACES =
            mapOf(
                R.string.app_name to "Shorts Blocker Kids",
                R.string.accessibility_service_label to "Shorts Blocker Kids-Schutz",
                R.string.accessibility_service_description to
                    "Erkennt YouTube Shorts, den TikTok-Kurzvideo-Feed, Instagram Reels und Facebook Reels " +
                    "lokal, wenn der Schutz aktiviert ist, und schließt den Blockierungsbildschirm, nachdem " +
                    "Sie diese Bereiche verlassen haben.",
                R.string.tamper_protection_label to "Shorts Blocker Kids-Manipulationsschutz",
                R.string.tamper_protection_description to
                    "Optionaler, von den Eltern gesteuerter Geräteadministrator-Schutz, der die Deinstallation " +
                    "erschwert, solange er aktiv ist.",
                R.string.welcome_start to "Starten",
                R.string.pin_setup_title to "Eltern-PIN erstellen",
                R.string.pin_entry_title to "Eltern-PIN eingeben",
                R.string.temporary_allow_title to "Kurzvideos erlauben",
                R.string.dashboard_title to "Kurzvideo-Schutz",
                R.string.blocking_overlay_title to "Kurzvideo blockiert",
                R.string.billing_status_connecting to
                    "Verbindung mit Google Play Billing wird hergestellt.",
                R.string.accessibility_disclosure_title to "Berechtigung für Bedienungshilfen",
                R.string.privacy_policy_title to "Datenschutzerklärung",
                R.string.tamper_protection_disclosure_title to "Manipulationsschutz",
            )

        val UKRAINIAN_SURFACES =
            mapOf(
                R.string.app_name to "Shorts Blocker Kids",
                R.string.accessibility_service_label to "Захист Shorts Blocker Kids",
                R.string.accessibility_service_description to
                    "Локально виявляє YouTube Shorts, стрічку коротких відео TikTok, Instagram Reels і " +
                    "Facebook Reels, коли захист увімкнено, та закриває екран блокування коротких відео " +
                    "після виходу із цих розділів.",
                R.string.tamper_protection_label to "Захист Shorts Blocker Kids від втручання",
                R.string.tamper_protection_description to
                    "Необов’язковий захист під контролем батьків із правами адміністратора пристрою, який " +
                    "ускладнює видалення застосунку, доки він активний.",
                R.string.welcome_start to "Почати",
                R.string.pin_setup_title to "Створення батьківського PIN-коду",
                R.string.pin_entry_title to "Введіть батьківський PIN-код",
                R.string.temporary_allow_title to "Тимчасово дозволити короткі відео",
                R.string.dashboard_title to "Захист від коротких відео",
                R.string.blocking_overlay_title to "Коротке відео заблоковано",
                R.string.billing_status_connecting to "Підключення до Google Play Billing.",
                R.string.accessibility_disclosure_title to "Дозвіл служби спеціальних можливостей",
                R.string.privacy_policy_title to "Політика конфіденційності",
                R.string.tamper_protection_disclosure_title to "Захист від втручання",
            )

        val LOCALE_EXPECTATIONS =
            listOf(
                LocaleExpectation("en-US", "available: ", ENGLISH_SURFACES),
                LocaleExpectation("de-DE", "verfügbar: ", GERMAN_SURFACES),
                LocaleExpectation("uk-UA", "доступно: ", UKRAINIAN_SURFACES),
                LocaleExpectation("fr-FR", "available: ", ENGLISH_SURFACES),
            )

        val PLURAL_EXPECTATIONS =
            listOf(
                PluralExpectation(
                    "en-US",
                    mapOf(
                        1 to "1 minute",
                        2 to "2 minutes",
                    ),
                ),
                PluralExpectation(
                    "de-DE",
                    mapOf(
                        1 to "1 Minute",
                        2 to "2 Minuten",
                    ),
                ),
                PluralExpectation(
                    "uk-UA",
                    mapOf(
                        1 to "1 хвилина",
                        2 to "2 хвилини",
                        5 to "5 хвилин",
                        11 to "11 хвилин",
                        21 to "21 хвилина",
                        22 to "22 хвилини",
                        25 to "25 хвилин",
                    ),
                ),
                PluralExpectation(
                    "fr-FR",
                    mapOf(
                        1 to "1 minute",
                        2 to "2 minutes",
                    ),
                ),
            )
    }
}
