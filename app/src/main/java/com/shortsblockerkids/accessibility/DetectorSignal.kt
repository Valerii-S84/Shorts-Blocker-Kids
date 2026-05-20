package com.shortsblockerkids.accessibility

enum class DetectorSignal(
    val id: String,
    val reason: String,
) {
    SHORTS_IDENTIFIER(
        id = "shorts_identifier",
        reason = "Shorts identifier signal",
    ),
    REEL_CONTAINER(
        id = "reel_container",
        reason = "Reel or Shorts player view ID signal",
    ),
    ACTION_RAIL(
        id = "action_rail",
        reason = "Vertical Shorts action rail signal",
    ),
    VERTICAL_FULLSCREEN(
        id = "vertical_fullscreen",
        reason = "Fullscreen vertical layout signal",
    ),
    REPEATED_VERTICAL_FEED(
        id = "repeated_vertical_feed",
        reason = "Repeated vertical feed signal",
    ),
}
