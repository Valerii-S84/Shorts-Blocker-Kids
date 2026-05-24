package com.shortsblockerkids.fixtureapps;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public final class FakeSocialActivity extends Activity {
    static final String EXTRA_SCREEN = "screen";

    private FakePlatform platform;
    private FakeScreenRenderer renderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        platform = FakePlatform.fromPackageName(getPackageName());
        renderer = new FakeScreenRenderer(this);
        render(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        render(intent);
    }

    private void render(Intent intent) {
        String screen = platform.normalizeScreen(intent.getStringExtra(EXTRA_SCREEN));
        setTitle("Fake " + platform.displayName + " " + screen);
        setContentView(renderer.render(platform, screen, this::showScreen));
    }

    private void showScreen(String screen) {
        Intent intent = new Intent(this, FakeSocialActivity.class);
        intent.putExtra(EXTRA_SCREEN, screen);
        setIntent(intent);
        render(intent);
    }
}
