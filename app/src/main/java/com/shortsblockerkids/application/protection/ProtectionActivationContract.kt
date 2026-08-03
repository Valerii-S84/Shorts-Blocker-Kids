package com.shortsblockerkids.application.protection

enum class ProtectionActivationIntent {
    COMPLETE_CURRENT_CONFIGURATION,
    ENABLE_PROTECTION,
}

enum class ProtectionActivationResult {
    ACTIVATED,
    ALREADY_STARTED,
    PREREQUISITES_NOT_MET,
}
