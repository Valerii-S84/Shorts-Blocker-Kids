package com.shortsblockerkids.core.tamper

import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import com.shortsblockerkids.R
import org.junit.Assert.assertEquals
import org.junit.Test

class TamperProtectionReceiverTest {
    @Test
    fun disableRequestUsesTamperProtectionWarningResource() {
        val receiver = TamperProtectionReceiver()

        val warning =
            receiver.onDisableRequested(
                WarningContext,
                android.content.Intent(),
            )

        assertEquals("Keep Shorts Blocker Kids protected", warning)
    }

    private object WarningContext : ContextWrapper(null) {
        override fun getResources(): Resources = WarningResources
    }

    private object WarningResources : Resources(null, DisplayMetrics(), Configuration()) {
        override fun getString(id: Int): String {
            assertEquals(R.string.tamper_protection_disable_warning, id)
            return "Keep Shorts Blocker Kids protected"
        }
    }
}
