# Shorts Blocker Kids Website

Static production website source for `https://shortsblockerkids.de`.

## Commands

```bash
npm run check
npm run build
npm run smoke
```

The build output is written to `website/dist/`.

## Preview Artifacts

Preview screenshots under `website/preview/` are intentionally tracked as
visual review evidence for public website revisions. Generated production build
output stays untracked under `website/dist/`.

## Release State

The current public install CTA is `Coming soon on Google Play` because no live
Google Play or beta testing URL is configured in `src/config.mjs`.

If a live Play URL becomes available, set `playStoreUrl` in `src/config.mjs`
and rebuild. If a closed or open testing URL becomes available before the
public listing, set `betaTestingUrl` instead.

## Metrics Widget

The metrics widget ships without an endpoint and shows neutral private-testing
copy. If a metrics endpoint is added later, it must return verified real data:

```json
{
  "verified": true,
  "privateTestingFamilies": 0,
  "blockedEvents": 0,
  "generatedAt": "2026-05-31T00:00:00Z"
}
```

The website must not use fake install, user, or blocking numbers.

## Static Deployment And Live Checks

Build with the commands above and deploy contents of website/dist to the HTTPS host for shortsblockerkids.de, preserving directory routes /, /privacy, /support and /terms. Generated HTML is not edited manually. Hosting/DNS/certificate/server changes need separate deployment authorization.

Before Play submission, recheck a valid HTTPS certificate and public root/privacy/support/terms pages in a normal browser. Privacy must not require login, geofence or PDF-only access; support shows svalerii535@gmail.com. Check www routing if configured. Match the exact release’s surfaces, SDK/data handling and optional Device Admin disclosure; Console answers remain drafts until verified against the uploaded AAB/current requirements.

Keep Coming soon on Google Play while Play/testing URLs are empty; rebuild and inspect the CTA after separately authorized URL configuration. No primary direct-APK CTA, fake testimonials or fabricated counts. A future metrics endpoint needs explicit scope, verified real data and separation from admin/debug surfaces; none is configured.

[Deployment evidence](../docs/SHORTS_BLOCKER_PRODUCTION_DEPLOYMENT_2026-05-31.md) records historical observations, not current availability. No live checks/deployment were performed in this preparation. Consolidated from SHORTS_BLOCKER_WEBSITE_DEPLOYMENT.md, which remains unchanged pending deletion approval.
