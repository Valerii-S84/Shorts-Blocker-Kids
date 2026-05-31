# Short Video Blocker Play Store Readiness Plan

Status: Planning. This document defines the production package required before
the full Short Video Blocker can be submitted to Google Play. It does not claim
that TikTok, Instagram Reels, Facebook Reels, billing, backend, AAB upload, or
Play Console declarations are complete today.

## Readiness Goals

- Ship a policy-accurate full Short Video Blocker.
- Avoid claiming unsupported platform behavior.
- Keep AccessibilityService use prominent, narrow, and disclosed.
- Keep Privacy Policy and Data Safety consistent with the shipped app.
- Complete AAB, internal testing, closed testing, and staged rollout gates.

## Accessibility Disclosure

The in-app disclosure must be shown before opening Android Accessibility
settings and must require affirmative consent.

For the full product, disclosure must state:

- The app uses Android AccessibilityService.
- The app detects supported short-video surfaces:
  - YouTube Shorts;
  - TikTok short-video feed or TikTok usage if app-level fallback is shipped;
  - Instagram Reels;
  - Facebook Reels.
- The app shows a blocking overlay.
- Parent PIN can temporarily allow access.
- The app does not collect or send child viewing history, video titles, URLs,
  messages, screen recordings, audio, location, contacts, or browsing history.
- Rules and PIN protection are stored locally.
- If backend/billing is included, backend receives only billing technical data.
- Blocking depends on Android AccessibilityService being enabled.

Consent rules:

- Consent button must be explicit, for example `I agree and continue`.
- Decline must not open Android Accessibility settings.
- Back/home/navigation away must not count as consent.
- The disclosure must be separate from general privacy policy text.

Do not set `android:isAccessibilityTool="true"` unless the app is truly
positioned as an accessibility tool for people with disabilities. The planned
product is a parental digital wellbeing app, not a disability-assistance tool.

## Play Accessibility Declaration

Suggested declaration answer for app functionality:

```text
Short Video Blocker uses Android AccessibilityService to detect supported
short-video surfaces on a child device, including YouTube Shorts, TikTok
short-video feed or disclosed TikTok usage blocking, Instagram Reels, and
Facebook Reels. When a blocked surface is detected and protection is enabled,
the app shows a blocking overlay. A parent PIN can temporarily allow access.
```

Data answer:

```text
The app does not collect or share personal or sensitive data through
AccessibilityService. Detection runs locally on the device. The app does not
store or send child viewing history, video titles, URLs, account names,
messages, screen recordings, audio, location, contacts, browsing history, or raw
Accessibility tree dumps.
```

If TikTok support uses app-level fallback instead of feed-specific detection,
the declaration and disclosure must say TikTok usage blocking, not only
short-video feed blocking.

## Privacy Policy Requirements

Privacy Policy must be hosted at a public URL and linked in Play Console.

It must state:

- The app is a parental digital wellbeing / short-video blocking app.
- The app uses Android AccessibilityService for local detection of supported
  blocked surfaces.
- The app stores local settings and parent PIN hash metadata on the device.
- The app does not collect child viewing history.
- The app does not collect video titles, URLs, account names, messages,
  screenshots, screen recordings, audio, location, contacts, or browsing
  history.
- The app does not sell data.
- The app has no ads.
- The app has no analytics unless explicitly added later and disclosed.
- If backend billing verification is included, backend receives only billing
  technical data such as purchase token, product ID, package name, and random
  app install ID.
- Google Play Billing may process payment data under Google Play terms.
- Parent can manage subscription through Google Play.

The Privacy Policy must be updated before each release if supported apps,
backend behavior, billing behavior, SDKs, permissions, or data handling change.

## Data Safety

Data Safety form must be completed even if the app collects no user data.

Current intended Data Safety posture:

- No collection or sharing of child viewing history.
- No collection or sharing of YouTube/TikTok/Instagram/Facebook content data.
- No collection or sharing of messages, location, contacts, audio, screen
  recordings, or browsing history.
- Local-only settings and PIN hash metadata stay on device.
- If backend billing verification ships, disclose billing technical data
  handling accurately.
- If Play Billing SDK processes payment data, disclose according to Google Play
  requirements and SDK behavior.

Data Safety must be reviewed against the final merged manifest, SDK list, and
network behavior before release.

## Target Audience

Target audience decision must be explicit in Play Console.

Planning assumption:

- The paying/controller user is the parent.
- The protected device may be used by a child.
- Store listing and screenshots may attract parents, not children.

Policy implications:

- If any target audience includes children, Families policy requirements must be
  satisfied.
- No ads are planned.
- No analytics SDK is planned.
- No child-directed SDK that is not approved for child-directed services should
  be added.
- Play Console Target Audience and Content answers must be accurate.

Legal review is required before final target age selection.

## Demo Video Script

Required video for Accessibility review:

1. Open the app from launcher.
2. Show onboarding and parent PIN creation.
3. Show the full Accessibility disclosure; scroll slowly if needed.
4. Tap decline / `Not now`.
5. Show that Android Accessibility settings do not open.
6. Trigger setup again.
7. Show disclosure again.
8. Tap affirmative consent.
9. Tap open settings.
10. Enable the app AccessibilityService in Android settings.
11. Return to the app and show protection status.
12. Open YouTube Shorts and show blocker overlay.
13. Use `Exit to phone home`.
14. Open TikTok target surface and show blocker overlay, if implemented.
15. Open Instagram Reels and show blocker overlay, if implemented.
16. Open Facebook Reels and show blocker overlay, if implemented.
17. Tap parent PIN action.
18. Enter parent PIN.
19. Select temporary allow: 5, 10, or 15 minutes.
20. Show temporary allow behavior.
21. Show that non-target surfaces are not blocked where applicable.

If a supported platform is not implemented in the submitted build, omit that
platform from video, disclosure, declaration, and store listing.

## AAB Pipeline

Required release steps:

- Configure Play App Signing / upload key flow.
- Produce release AAB.
- Run release lint.
- Run debug unit tests.
- Run release merged manifest privacy audit.
- Verify versionCode/versionName.
- Verify no debug-only UI is enabled in release.
- Verify release build has no raw Accessibility tree logs.
- Verify no unsupported permissions.
- Upload AAB to internal testing.

Release command target:

```bash
ANDROID_HOME=/home/serputko/Android/Sdk gradle clean \
  :app:assembleRelease \
  :app:bundleRelease \
  :app:lintRelease \
  :app:ktlintCheck \
  :app:assembleDebug \
  :app:testDebugUnitTest \
  :app:lintDebug
```

For Shorts Blocker Kids, local `:app:bundleRelease` is configured and produces:

```text
app/build/outputs/bundle/release/app-release.aab
```

## Internal Testing

Internal testing goals:

- Verify install/update.
- Verify Accessibility disclosure and consent.
- Verify AccessibilityService enablement.
- Verify every implemented detector.
- Verify overlay, exit, PIN, and temporary allow.
- Verify billing tester purchase and restore if billing is included.
- Verify backend entitlement if backend is included.
- Verify no unsupported data leaves device.

Internal test exit:

- No critical crashes.
- No Play policy mismatch.
- No false positives on core non-target surfaces.
- No release-only blocker failures.

## Closed Testing

Closed testing goals:

- Run required Google Play testing duration if account/app status requires it.
- Include testers on different OEMs and Android versions.
- Cover every supported app.
- Cover subscription lifecycle if billing is included.
- Collect tester feedback without analytics SDK unless explicitly approved.

Closed test exit:

- Detector reliability acceptable.
- No major false positives.
- No blocking loops.
- Billing entitlement stable.
- Policy declarations still match behavior.

## Production Rollout

Rollout plan:

1. Submit complete Play Store package.
2. Start staged rollout at low percentage.
3. Monitor Play Console vitals and policy messages.
4. Pause rollout on crash, detector, billing, or policy issue.
5. Hotfix through normal Play release flow.
6. Increase rollout only after stable metrics and manual spot checks.

Do not use analytics SDK unless explicitly approved and policy-reviewed. Use
Play Console vitals and tester reports as the initial feedback channels.

## Required External Artifacts

- Hosted Privacy Policy URL: `https://movashield.de/privacy`.
- Completed Data Safety form.
- Completed Accessibility declaration.
- Demo video URL.
- Target Audience and Content answers.
- Content rating questionnaire.
- Store listing copy and screenshots.
- Test credentials or instructions for Play review.
- AAB uploaded to testing track.
- Billing product configuration if billing is included.
- Backend production readiness evidence if backend is included.

## Policy References

- AccessibilityService API policy:
  https://support.google.com/googleplay/android-developer/answer/10964491
- User Data policy:
  https://support.google.com/googleplay/android-developer/answer/10144311
- Data Safety form guidance:
  https://support.google.com/googleplay/android-developer/answer/10787469
- Play Console requirements:
  https://support.google.com/googleplay/android-developer/answer/10788890
- Families policies:
  https://support.google.com/googleplay/android-developer/answer/9893335
