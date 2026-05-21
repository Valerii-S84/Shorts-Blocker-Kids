package com.shortsblockerkids.accessibility

class PhoneHomeExitController(
    private val phoneHomeAction: PhoneHomeAction,
    private val onExitCompleted: () -> Unit,
) {
    fun exitToPhoneHome(): Boolean {
        if (!phoneHomeAction.exitToPhoneHome()) {
            return false
        }

        onExitCompleted()
        return true
    }
}

fun interface PhoneHomeAction {
    fun exitToPhoneHome(): Boolean
}
