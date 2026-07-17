# Shorts Blocker Kids Production Deployment Evidence - 2026-05-31

Status: Public-safe summary. July 17, 2026 public HTTPS smoke verifies root,
Privacy, Support, Terms, `www` Privacy, and billing backend health. Google Play
credentials, RTDN, real purchase flows, and full real-device QA remain pending.

## Scope

This public repository keeps only DNS/URL health evidence and release blockers.
Production SSH aliases, raw server addresses, internal filesystem paths, reverse
proxy internals, backup paths, and colocated-app details belong in a private
operations runbook and are intentionally omitted here.

## Public Hostnames Verified

July 17, 2026 external HTTPS recheck:

```text
https://shortsblockerkids.de/                 200
https://shortsblockerkids.de/privacy          200
https://shortsblockerkids.de/support          200
https://shortsblockerkids.de/terms            200
https://www.shortsblockerkids.de/privacy      200
https://billing.shortsblockerkids.de/health   200 {"status":"ok"}
```

## Public Release Evidence

- Website root, Privacy, Support, and Terms are reachable over public HTTPS.
- The verified Privacy Policy URL for Play Console is
  `https://shortsblockerkids.de/privacy`.
- The verified Support URL for Play Console is
  `https://shortsblockerkids.de/support`.
- The production billing backend health endpoint is reachable at
  `https://billing.shortsblockerkids.de/health`.
- The public website CTA must remain `Coming soon on Google Play` until a live
  Play listing or testing opt-in URL is available.

## Billing Backend State

The public health endpoint returns healthy, but full production billing review
remains blocked until Google Play credentials, authenticated RTDN, and real
license-tester purchase flows are configured and verified outside this public
repository.

## Remaining Blockers

- Google Play service-account credentials are not recorded in this repository.
- Pub/Sub authenticated RTDN config is not recorded as verified.
- Real Google Play purchase/license-tester flow was not verified.
- Full real-device QA was not verified in this deployment evidence.
