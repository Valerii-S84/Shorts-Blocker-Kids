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

The website does not connect to the billing backend by default and must not use
fake install, user, or blocking numbers.
