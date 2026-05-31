import {
  organizationStructuredData,
  pageLayout,
  sectionEyebrow,
} from "../components.mjs";
import { siteConfig } from "../config.mjs";

export function renderTermsPage() {
  const body = `<section class="legal-hero">
  <div class="container narrow">
    ${sectionEyebrow("Terms")}
    <h1>Terms</h1>
    <p class="lead">Basic terms for using ${siteConfig.appName} and this website. These terms are practical product terms, not a substitute for legal advice.</p>
    <p class="muted">Last updated: ${siteConfig.lastUpdated}</p>
  </div>
</section>
<section class="legal-section">
  <div class="container narrow legal-copy">
    <h2>Use Of The App</h2>
    <p>${siteConfig.appName} is a parental control helper for reducing access to supported short-form video surfaces on Android. It must be configured by a parent or guardian with appropriate authority over the device.</p>

    <h2>No Hidden Monitoring</h2>
    <p>The app is not intended for secret surveillance, spyware, or hidden monitoring. Parents should use it transparently and in compliance with applicable rules.</p>

    <h2>Service Limitations</h2>
    <p>The app does not claim to block every video, every app, every short-video surface, or every attempt to disable protection. Blocking depends on the app being installed, Protection being ON, parent setup being complete, and Android Accessibility Service remaining enabled.</p>

    <h2>Subscriptions</h2>
    <p>Subscriptions for the Play-distributed app are intended to be handled through Google Play Billing. Google Play controls purchase, cancellation, refund, and subscription management flows where applicable.</p>

    <h2>Website</h2>
    <p>This website provides product, privacy, and support information. It does not provide a direct APK download as the primary install path and does not expose billing backend admin or debug information.</p>

    <h2>Changes</h2>
    <p>The app and website may change as support, Play review, and public testing progress. Privacy-impacting changes should be reflected in the Privacy Policy.</p>

    <h2>Contact</h2>
    <p>Contact <a href="mailto:${siteConfig.contactEmail}">${siteConfig.contactEmail}</a> for support or terms questions.</p>
  </div>
</section>`;

  return pageLayout({
    title: "Terms",
    description:
      "Basic product terms for Shorts Blocker Kids and the shortsblockerkids.de website.",
    path: "/terms",
    activePath: "/terms",
    body,
    jsonLd: organizationStructuredData(),
  });
}
