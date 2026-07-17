package com.shortsblockerkids.core.tamper

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.shortsblockerkids.R

class TamperProtectionReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(
        context: Context,
        intent: Intent,
    ): CharSequence = context.getString(R.string.tamper_protection_disable_warning)
}
