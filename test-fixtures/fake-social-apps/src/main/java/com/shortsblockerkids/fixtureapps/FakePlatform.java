package com.shortsblockerkids.fixtureapps;

enum FakePlatform {
    YOUTUBE(
            "com.shortsblockerkids.fixture.youtube",
            "YouTube",
            "home",
            new String[]{"home", "search", "normal_video", "shorts"}
    ),
    TIKTOK(
            "com.shortsblockerkids.fixture.tiktok",
            "TikTok",
            "for_you",
            new String[]{"for_you", "profile", "search", "settings"}
    ),
    INSTAGRAM(
            "com.shortsblockerkids.fixture.instagram",
            "Instagram",
            "feed",
            new String[]{"feed", "profile", "reels", "story"}
    ),
    FACEBOOK(
            "com.shortsblockerkids.fixture.facebook",
            "Facebook",
            "feed",
            new String[]{"feed", "profile", "reels", "groups"}
    ),
    UNSUPPORTED(
            "com.example.shortvideo.fixture",
            "Unsupported",
            "reels",
            new String[]{"reels", "feed"}
    );

    final String packageName;
    final String displayName;
    final String defaultScreen;
    final String[] screens;

    FakePlatform(
            String packageName,
            String displayName,
            String defaultScreen,
            String[] screens
    ) {
        this.packageName = packageName;
        this.displayName = displayName;
        this.defaultScreen = defaultScreen;
        this.screens = screens;
    }

    String normalizeScreen(String requestedScreen) {
        if (requestedScreen == null || requestedScreen.isBlank()) {
            return defaultScreen;
        }

        for (String screen : screens) {
            if (screen.equals(requestedScreen)) {
                return screen;
            }
        }
        return defaultScreen;
    }

    static FakePlatform fromPackageName(String packageName) {
        for (FakePlatform platform : values()) {
            if (platform.packageName.equals(packageName)) {
                return platform;
            }
        }
        return UNSUPPORTED;
    }
}
