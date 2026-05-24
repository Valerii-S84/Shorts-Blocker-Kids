package com.shortsblockerkids.fixtureapps;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

import java.util.Locale;

final class FakeScreenRenderer {
    private static final int BACKGROUND = Color.rgb(12, 16, 28);
    private static final int SURFACE = Color.rgb(248, 250, 252);
    private static final int TEXT = Color.rgb(18, 24, 38);
    private static final int MUTED = Color.rgb(87, 98, 116);
    private static final int VIDEO = Color.rgb(16, 24, 39);
    private static final int ACCENT = Color.rgb(25, 118, 210);

    private final Context context;

    FakeScreenRenderer(Context context) {
        this.context = context;
    }

    View render(
            FakePlatform platform,
            String screen,
            ScreenSelectionListener listener
    ) {
        LinearLayout shell = new LinearLayout(context);
        shell.setId(R.id.fake_root);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(SURFACE);

        shell.addView(topBar(platform, screen), matchWidth(dp(54)));
        shell.addView(content(platform, screen), weightedContent());
        shell.addView(navBar(platform, screen, listener), matchWidth(dp(68)));
        return shell;
    }

    private View content(FakePlatform platform, String screen) {
        if (platform == FakePlatform.YOUTUBE) {
            return youtubeContent(screen);
        }
        if (platform == FakePlatform.TIKTOK) {
            return tiktokContent(screen);
        }
        if (platform == FakePlatform.INSTAGRAM) {
            return instagramContent(screen);
        }
        if (platform == FakePlatform.FACEBOOK) {
            return facebookContent(screen);
        }
        return unsupportedContent(screen);
    }

    private View youtubeContent(String screen) {
        switch (screen) {
            case "shorts":
                return shortVideoSurface(
                        R.id.shorts_player,
                        "Shorts",
                        "Shorts full screen video",
                        new String[]{"Like", "Dislike", "Comments", "Share"}
                );
            case "normal_video":
                return normalVideoScreen(
                        R.id.watch_video_player,
                        "Normal video",
                        "Regular YouTube watch screen"
                );
            case "search":
                return simpleListScreen(
                        R.id.youtube_search_results,
                        "Search",
                        new String[]{"Search field", "Long video result", "Channel result"}
                );
            default:
                return simpleListScreen(
                        R.id.youtube_home_feed,
                        "Home",
                        new String[]{"Home", "Recommended video", "Subscriptions", "Shorts tab"}
                );
        }
    }

    private View tiktokContent(String screen) {
        switch (screen) {
            case "profile":
                return simpleListScreen(R.id.tiktok_profile, "Profile", new String[]{"Profile", "Posts"});
            case "search":
                return simpleListScreen(R.id.tiktok_search_results, "Search", new String[]{"Search", "Users", "Videos"});
            case "settings":
                return simpleListScreen(R.id.tiktok_settings, "Settings", new String[]{"Settings", "Privacy"});
            default:
                return shortVideoSurface(
                        R.id.aweme_video_player,
                        "For You",
                        "For You short video feed",
                        new String[]{"Like", "Comments", "Share", "Save", "Follow"}
                );
        }
    }

    private View instagramContent(String screen) {
        switch (screen) {
            case "reels":
                return shortVideoSurface(
                        R.id.clips_viewer,
                        "Reels",
                        "Instagram Reels viewer",
                        new String[]{"Like", "Comments", "Share", "Send", "Save", "Audio"}
                );
            case "profile":
                return simpleListScreen(R.id.instagram_profile, "Profile", new String[]{"Profile", "Grid"});
            case "story":
                return simpleListScreen(R.id.story_viewer, "Story", new String[]{"Story", "Close friends"});
            default:
                return simpleListScreen(R.id.instagram_feed, "Feed", new String[]{"Feed", "Photo post", "Story tray"});
        }
    }

    private View facebookContent(String screen) {
        switch (screen) {
            case "reels":
                return shortVideoSurface(
                        R.id.reels_viewer,
                        "Reels",
                        "Facebook Reels viewer",
                        new String[]{"Like", "Comments", "Share", "More"}
                );
            case "profile":
                return simpleListScreen(R.id.facebook_profile, "Profile", new String[]{"Profile", "Timeline"});
            case "groups":
                return simpleListScreen(R.id.groups_page, "Groups", new String[]{"Groups", "Page", "Discussion"});
            default:
                return simpleListScreen(R.id.facebook_feed, "Feed", new String[]{"Feed", "Friends", "Page post"});
        }
    }

    private View unsupportedContent(String screen) {
        if ("reels".equals(screen)) {
            return shortVideoSurface(
                    R.id.reels_viewer,
                    "Reels",
                    "Unsupported package short-video lookalike",
                    new String[]{"Like", "Comments", "Share", "More"}
            );
        }
        return simpleListScreen(R.id.video_player, "Unsupported feed", new String[]{"Feed", "Video"});
    }

    private View shortVideoSurface(
            int surfaceId,
            String explicitSignal,
            String title,
            String[] actions
    ) {
        FrameLayout frame = new FrameLayout(context);
        frame.setBackgroundColor(BACKGROUND);

        ScrollView video = new ScrollView(context);
        video.setId(surfaceId);
        video.setContentDescription(explicitSignal + " " + title);
        video.setFillViewport(true);
        video.setBackgroundColor(VIDEO);
        video.addView(videoText(title), matchWidth(dp(1800)));
        frame.addView(video, frameMatchParent());

        frame.addView(actionRail(actions), actionRailParams());
        return frame;
    }

    private View normalVideoScreen(int videoId, String title, String description) {
        LinearLayout layout = verticalContent();
        TextView player = label(title, Color.WHITE, 22);
        player.setId(videoId);
        player.setContentDescription(description);
        player.setGravity(Gravity.CENTER);
        player.setBackgroundColor(VIDEO);
        layout.addView(player, matchWidth(dp(260)));
        layout.addView(label("Like   Comment   Share", MUTED, 16), matchWidth(dp(56)));
        layout.addView(label("Comments and related long-form videos", MUTED, 16), matchWidth(dp(120)));
        return layout;
    }

    private View simpleListScreen(int viewId, String title, String[] rows) {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setId(viewId);
        scrollView.setContentDescription(title);
        LinearLayout content = verticalContent();
        content.addView(label(title, TEXT, 24), matchWidth(dp(64)));
        for (String row : rows) {
            content.addView(listRow(row), matchWidth(dp(72)));
        }
        scrollView.addView(content, matchWidth(ViewGroup.LayoutParams.WRAP_CONTENT));
        return scrollView;
    }

    private View topBar(FakePlatform platform, String screen) {
        TextView title = label(platform.displayName + " - " + screenLabel(screen), Color.WHITE, 18);
        title.setId(R.id.fake_screen_title);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(18), 0, dp(18), 0);
        title.setBackgroundColor(BACKGROUND);
        return title;
    }

    private View navBar(
            FakePlatform platform,
            String currentScreen,
            ScreenSelectionListener listener
    ) {
        LinearLayout row = new LinearLayout(context);
        row.setId(R.id.fake_bottom_nav);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setBackgroundColor(Color.rgb(232, 236, 242));
        for (String screen : platform.screens) {
            Button button = new Button(context);
            button.setText(screenLabel(screen));
            button.setContentDescription(screenLabel(screen));
            button.setAllCaps(false);
            button.setEnabled(!screen.equals(currentScreen));
            button.setOnClickListener(view -> listener.onScreenSelected(screen));
            row.addView(button, weightedNavItem());
        }
        return row;
    }

    private View actionRail(String[] actions) {
        LinearLayout rail = new LinearLayout(context);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setGravity(Gravity.CENTER);
        for (int index = 0; index < actions.length; index++) {
            Button button = new Button(context);
            button.setText(actions[index].substring(0, 1));
            button.setContentDescription(actions[index]);
            button.setAllCaps(false);
            rail.addView(button, actionButtonParams(index == 0));
        }
        return rail;
    }

    private TextView videoText(String title) {
        TextView text = label(title + "\n\nSwipeable full-screen video feed", Color.WHITE, 24);
        text.setGravity(Gravity.CENTER);
        return text;
    }

    private LinearLayout verticalContent() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(20), dp(20), dp(20));
        layout.setBackgroundColor(SURFACE);
        return layout;
    }

    private TextView listRow(String text) {
        TextView row = label(text, TEXT, 18);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), 0, dp(16), 0);
        row.setBackgroundColor(Color.rgb(238, 242, 247));
        row.setContentDescription(text);
        return row;
    }

    private TextView label(String text, int color, int sp) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextColor(color);
        label.setTextSize(sp);
        label.setContentDescription(text);
        return label;
    }

    private String screenLabel(String screen) {
        return screen.replace('_', ' ').toUpperCase(Locale.US);
    }

    private LinearLayout.LayoutParams weightedContent() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
    }

    private LinearLayout.LayoutParams weightedNavItem() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
    }

    private LinearLayout.LayoutParams actionButtonParams(boolean first) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(86), dp(58));
        if (!first) {
            params.topMargin = dp(118);
        }
        return params;
    }

    private FrameLayout.LayoutParams actionRailParams() {
        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(dp(112), ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        params.rightMargin = dp(14);
        return params;
    }

    private FrameLayout.LayoutParams frameMatchParent() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
    }

    private LinearLayout.LayoutParams matchWidth(int height) {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    interface ScreenSelectionListener {
        void onScreenSelected(String screen);
    }
}
