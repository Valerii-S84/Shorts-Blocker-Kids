package com.shortsblockerkids.core.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class BillingPresentationResourceInvariantTest {
    @Test
    fun billingPresentationReferencesExistingDefaultResources() {
        val presentation = repoFile(BILLING_PRESENTATION_SOURCE).readText()
        val referencedResources =
            BILLING_RESOURCE_REFERENCE
                .findAll(presentation)
                .map { match -> match.groupValues[1] }
                .toSet()
        val missingResources = referencedResources - resourceNames()

        assertTrue("Billing presentation must reference resources", referencedResources.isNotEmpty())
        assertTrue("Missing billing resources: $missingResources", missingResources.isEmpty())
    }

    @Test
    fun billingFormattingContractsPreservePlayPriceAndResponseCodeTypes() {
        val statusWithPrice = stringValue("billing_status_available_price")
        val termsWithPrice = stringValue("subscription_terms_with_price")
        val errorWithCode = stringValue("billing_error_with_response_code")

        assertTrue(statusWithPrice.contains("%1\$s"))
        assertTrue(termsWithPrice.contains("%1\$s"))
        assertTrue(termsWithPrice.contains("free test"))
        assertTrue(termsWithPrice.contains("Google Play"))
        assertTrue(errorWithCode.contains("%1\$s"))
        assertTrue(errorWithCode.contains("%2\$d"))
    }

    @Test
    fun billingUiLocalizesAtTheComposePresentationBoundary() {
        val presentation = repoFile(BILLING_PRESENTATION_SOURCE).readText()
        val dashboard = repoFile(DASHBOARD_SOURCE).readText()
        val debugScreen = repoFile(DEBUG_SOURCE).readText()
        val repository = repoFile(BILLING_REPOSITORY_SOURCE).readText()

        assertTrue(
            presentation.contains(
                "stringResource(R.string.subscription_terms_with_price, productPrice)",
            ),
        )
        assertTrue(repository.contains("?.formattedPrice"))
        assertTrue(dashboard.contains("billingSubscriptionTermsText(billingUiState.productPrice)"))
        assertTrue(debugScreen.contains("billingMessageText(uiState.billing.uiState.message)"))
        assertFalse(dashboard.contains("BillingCopy"))
        assertFalse(dashboard.contains("statusMessage"))
        assertFalse(repository.contains("statusMessage"))
    }

    @Test
    fun billingDomainNoLongerContainsMovedPresentationCopy() {
        val billingDomain =
            BILLING_DOMAIN_SOURCES.joinToString(separator = "\n") { source ->
                repoFile(source).readText()
            }

        MOVED_PRESENTATION_PHRASES.forEach { phrase ->
            assertFalse("Billing domain still contains moved copy: $phrase", billingDomain.contains(phrase))
        }
    }

    private fun resourceNames(): Set<String> {
        val nodes = resources.getElementsByTagName("string")
        return buildSet {
            for (index in 0 until nodes.length) {
                val element = nodes.item(index) as Element
                add(element.getAttribute("name"))
            }
        }
    }

    private fun stringValue(name: String): String {
        val nodes = resources.getElementsByTagName("string")
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as Element
            if (element.getAttribute("name") == name) {
                return element.textContent.trim()
            }
        }
        error("Missing string resource: $name")
    }

    private val resources by lazy {
        DocumentBuilderFactory
            .newInstance()
            .apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }.newDocumentBuilder()
            .parse(repoFile(STRINGS_XML))
    }

    private fun repoFile(relativePath: String): File {
        val userDir = requireNotNull(System.getProperty("user.dir"))
        var root: File? = File(userDir)
        while (root != null) {
            val candidate = File(root, relativePath)
            if (candidate.isFile) {
                return candidate
            }
            root = root.parentFile
        }
        error("Missing repository file: $relativePath")
    }

    private companion object {
        const val STRINGS_XML = "app/src/main/res/values/strings.xml"
        const val BILLING_PRESENTATION_SOURCE =
            "app/src/main/java/com/shortsblockerkids/feature/billing/BillingPresentation.kt"
        const val DASHBOARD_SOURCE =
            "app/src/main/java/com/shortsblockerkids/feature/dashboard/DashboardScreen.kt"
        const val DEBUG_SOURCE =
            "app/src/debug/java/com/shortsblockerkids/feature/debug/DetectorPlaygroundScreen.kt"
        const val BILLING_REPOSITORY_SOURCE =
            "app/src/main/java/com/shortsblockerkids/core/billing/PlayBillingRepository.kt"

        val BILLING_RESOURCE_REFERENCE =
            Regex("""R\.string\.((?:billing|subscription)_[a-z0-9_]+)""")

        val BILLING_DOMAIN_SOURCES =
            setOf(
                "app/src/main/java/com/shortsblockerkids/application/billing/" +
                    "BillingPurchaseSummary.kt",
                "app/src/main/java/com/shortsblockerkids/application/billing/" +
                    "BillingSyncOutcome.kt",
                "app/src/main/java/com/shortsblockerkids/application/billing/" +
                    "SyncBillingEntitlementUseCase.kt",
                "app/src/main/java/com/shortsblockerkids/core/billing/BillingAvailability.kt",
                "app/src/main/java/com/shortsblockerkids/core/billing/BillingUiState.kt",
                "app/src/main/java/com/shortsblockerkids/core/billing/BillingVerificationPolicy.kt",
                BILLING_REPOSITORY_SOURCE,
            )

        val MOVED_PRESENTATION_PHRASES =
            setOf(
                "Connecting to Google Play Billing.",
                "Subscription is not ready yet.",
                "Purchase canceled.",
                "Subscription active.",
                "Purchase pending.",
                "No active Google Play subscription found.",
                "Could not restore purchases",
                "Subscription verification unavailable.",
            )
    }
}
