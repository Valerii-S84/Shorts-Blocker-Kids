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
    detail:
      "Helps block the short-video feed while keeping normal app use possible where supported.",
  },
  {
    name: "TikTok",
    status: "Supported",
    detail:
      "Helps block the short-video feed while keeping normal app use possible where supported.",
  },
  {
    name: "Instagram Reels",
    status: "Supported",
    detail:
      "Helps block the short-video feed while keeping normal app use possible where supported.",
  },
  {
    name: "Facebook Reels",
    status: "Supported",
    detail:
      "Helps block the short-video feed while keeping normal app use possible where supported.",
  },
];

export function installCta() {
  if (siteConfig.playStoreUrl) {
    return {
      label: "Get it on Google Play",
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
