export const siteConfig = {
  appName: "Shorts Blocker Kids",
  canonicalDomain: "https://shortsblockerkids.de",
  billingBackendUrl: "https://billing.shortsblockerkids.de",
  publisher: "Valerii Serputko",
  contactEmail: "svalerii535@gmail.com",
  lastUpdated: "May 31, 2026",
  playStoreUrl: "",
  betaTestingUrl: "",
  releaseState: "coming-soon",
  metricsEndpoint: "",
  demoVideoUrl: "",
};

export const protectedPlatforms = [
  {
    name: "YouTube Shorts",
    status: "Supported",
    detail: "Supported by the current app and backed by YouTube real-device smoke evidence.",
  },
  {
    name: "TikTok",
    status: "Code-supported, needs real-device validation",
    detail: "Main Android package com.zhiliaoapp.musically only. Regional TikTok packages are not claimed.",
  },
  {
    name: "Instagram Reels",
    status: "Code-supported, needs real-device validation",
    detail: "Implemented in app detectors and still pending final real-device QA before broad rollout claims.",
  },
  {
    name: "Facebook Reels",
    status: "Code-supported, needs real-device validation",
    detail: "Implemented for com.facebook.katana. Facebook Lite is not claimed as supported.",
  },
];

export const unsupportedPlatforms = [
  "TikTok regional package com.ss.android.ugc.trill",
  "Facebook Lite com.facebook.lite",
  "Instagram Lite",
  "YouTube TV",
  "YouTube Music",
];

export function installCta() {
  if (siteConfig.playStoreUrl) {
    return {
      label: "Open Google Play listing",
      href: siteConfig.playStoreUrl,
      disabled: false,
    };
  }

  if (siteConfig.betaTestingUrl) {
    return {
      label: "Join testing",
      href: siteConfig.betaTestingUrl,
      disabled: false,
    };
  }

  return {
    label: "Coming soon on Google Play",
    href: "",
    disabled: true,
  };
}
