package com.shortsblockerkids.core.tamper

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.shortsblockerkids.R

object TamperProtectionStatus {
    fun isActive(context: Context): Boolean {
        val manager =
            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return false
        return manager.isAdminActive(receiverComponent(context))
    }

    fun enableIntent(context: Context): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, receiverComponent(context))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                context.getString(R.string.tamper_protection_system_explanation),
            )
        }

    private fun receiverComponent(context: Context): ComponentName = ComponentName(context, TamperProtectionReceiver::class.java)
}
