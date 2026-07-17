import {
  organizationStructuredData,
  pageLayout,
  secondaryCta,
  sectionEyebrow,
} from "../components.mjs";
import { siteConfig } from "../config.mjs";

export function renderPrivacyPage() {
  const body = `<section class="legal-hero">
  <div class="container narrow">
    ${sectionEyebrow("Privacy")}
    <h1>Privacy Policy</h1>
    <p class="lead">This Privacy Policy explains how ${siteConfig.appName} handles data in the Android app, website, and Google Play subscription verification flow.</p>
    <p class="muted">Last updated: ${siteConfig.lastUpdated}</p>
  </div>
</section>
<section class="legal-section">
  <div class="container narrow legal-copy">
    ${privacyToc()}
    ${identitySection()}
    ${overviewSection()}
    ${dataCollectedSection()}
    ${dataNotCollectedSection()}
    ${accessibilitySection()}
    ${tamperProtectionSection()}
    ${billingSection()}
    ${childrenSection()}
    ${sharingSection()}
    ${retentionSection()}
    ${securitySection()}
    ${rightsSection()}
    ${limitsSection()}
    ${contactSection()}
  </div>
</section>`;

  return pageLayout({
    title: "Privacy Policy",
    description:
      "Privacy Policy for Shorts Blocker Kids, including AccessibilityService, local settings, Google Play Billing, and billing backend data handling.",
    path: "/privacy",
    activePath: "/privacy",
    body,
    jsonLd: organizationStructuredData(),
  });
}

function identitySection() {
  return `<h2 id="who-we-are">Who We Are</h2>
<p><strong>App:</strong> ${siteConfig.appName}</p>
<p><strong>Publisher:</strong> ${siteConfig.publisher}</p>
<p><strong>Website:</strong> <a href="${siteConfig.canonicalDomain}">${siteConfig.canonicalDomain}</a></p>
<p><strong>Privacy and support contact:</strong> <a href="mailto:${siteConfig.contactEmail}">${siteConfig.contactEmail}</a></p>`;
}

function privacyToc() {
  const items = [
    ["Who We Are", "#who-we-are"],
    ["Overview", "#overview"],
    ["Data Stored", "#data-stored"],
    ["Data Not Collected", "#data-not-collected"],
    ["AccessibilityService Use", "#accessibility"],
    ["Device Admin", "#tamper-protection"],
    ["Billing", "#billing"],
    ["Contact", "#contact"],
  ];

  return `<nav class="legal-toc" aria-label="Privacy Policy sections">
  <strong>On this page</strong>
  <div>
    ${items.map(([label, href]) => `<a href="${href}">${label}</a>`).join("")}
  </div>
</nav>`;
}

function overviewSection() {
  return `<h2 id="overview">Overview</h2>
<p>${siteConfig.appName} helps parents reduce children's exposure to supported short-form video surfaces on an Android phone. It is a parental control helper for setting clear boundaries around short-video feeds.</p>
<p>Protection behavior runs locally on the device. The app does not require an account. Production subscription access may use Google Play Billing and a billing backend at <a href="${siteConfig.billingBackendUrl}">${siteConfig.billingBackendUrl}</a> for purchase verification and entitlement status only.</p>`;
}

function dataCollectedSection() {
  return `<h2 id="data-stored">What Data Is Collected Or Stored</h2>
<h3>Local app data on the device</h3>
<p>The app stores local settings needed for protection, including whether protection is enabled, which supported platforms are enabled, temporary allow timing, Accessibility disclosure acceptance, free-test state, billing entitlement status, and parent PIN hash metadata.</p>
<p>The parent PIN is not stored as plain text. The app stores hash and salt metadata so it can verify the PIN locally.</p>
<h3>Billing technical data</h3>
<p>When Google Play Billing and backend verification are used, the app or backend may process only the billing technical data needed to verify subscription access:</p>
<ul>
  <li>random app installation ID;</li>
  <li>app package name and app version;</li>
  <li>Google Play subscription product ID;</li>
  <li>Google Play purchase token;</li>
  <li>entitlement state, verification time, and active-until time where available;</li>
  <li>processed Real-time Developer Notification message IDs for idempotency.</li>
</ul>
<p>The backend stores entitlement records and a hashed purchase token. It does not store payment card data.</p>
<h3>Support email</h3>
<p>If you email support, the message and email address are handled so the publisher can respond to your request.</p>`;
}

function dataNotCollectedSection() {
  return `<h2 id="data-not-collected">What Is Not Collected</h2>
<p>${siteConfig.appName} does not collect, transmit, sell, or share child data or supported-app activity. In particular, the current app does not collect:</p>
<ul>
  <li>watch history;</li>
  <li>video titles, video content, URLs, or browsing history;</li>
  <li>comments, private messages, chats, or account names;</li>
  <li>screen recordings, screenshots, audio, microphone, or camera data;</li>
  <li>location, contacts, calendar, SMS, or call data;</li>
  <li>raw Accessibility tree dumps;</li>
  <li>analytics, advertising identifiers, or child profiles.</li>
</ul>
<p>The current app has no account system, no ads, no analytics SDK, and no cloud sync.</p>`;
}

function accessibilitySection() {
  return `<h2 id="accessibility">AccessibilityService Use</h2>
<p>${siteConfig.appName} uses Android AccessibilityService to detect when YouTube Shorts, TikTok short-video feed, Instagram Reels, or Facebook Reels is open and to show a blocking overlay when protection is enabled.</p>
<p>Android does not provide a normal app API for detecting these surfaces inside other apps. AccessibilityService allows local inspection of supported app UI state so the blocker can work.</p>
<p>Accessibility processing runs locally on the device. The service is not used to read private messages, collect child activity, record the screen, capture audio, or send Accessibility content to a server.</p>`;
}

function tamperProtectionSection() {
  return `<h2 id="tamper-protection">Device Admin Tamper Protection</h2>
<p>Optional Device Admin tamper protection can make ${siteConfig.appName} harder to uninstall while it is active. It is enabled only after a separate parent-facing disclosure and the Android Device Admin confirmation screen.</p>
<p>Device Admin tamper protection does not replace AccessibilityService, block Android Settings, wipe the device, lock the screen, reset passwords, change hidden system settings, or manage other apps. If someone tries to disable Device Admin status, Android shows a warning first.</p>`;
}

function billingSection() {
  return `<h2 id="billing">Payments And Billing Backend</h2>
<p>Subscriptions are handled through Google Play Billing. Google Play may process payment and subscription data under Google's own terms and privacy rules.</p>
<p>The app does not store payment card data, billing addresses, bank details, or payment account credentials.</p>
<p>Production release builds are configured to use the billing backend origin <a href="${siteConfig.billingBackendUrl}">${siteConfig.billingBackendUrl}</a> for Google Play purchase verification and entitlement status. The billing backend is separate from the public website and must not receive child data, supported app activity, video titles, URLs, messages, screen recordings, audio, location, contacts, browsing history, or raw Accessibility tree dumps.</p>`;
}

function childrenSection() {
  return `<h2>Children And Family Data</h2>
<p>The app is meant to be configured by a parent or guardian on a child's Android phone. It does not create child accounts, sell child profiles, or collect child identity information.</p>
<p>Parents should use the app transparently and in a way that fits their family rules and local requirements. The app is not a substitute for supervision or platform-level parental controls.</p>`;
}

function sharingSection() {
  return `<h2>Data Sharing</h2>
<p>The app does not sell personal data. It does not share child data, watch history, video titles, URLs, messages, comments, account names, screen recordings, audio, location, contacts, browsing history, or Accessibility tree dumps with third parties.</p>
<p>Google Play Billing processes payments and subscription status as part of the Google Play purchase flow. The billing backend uses Google Play Developer API verification for entitlement decisions.</p>`;
}

function retentionSection() {
  return `<h2>Retention</h2>
<p>Local settings and PIN hash metadata remain on the device until the parent clears app data or uninstalls the app.</p>
<p>Billing entitlement records are kept only as needed for subscription verification, fraud prevention, Real-time Developer Notification idempotency, support, accounting, and legal retention needs.</p>
<p>Support emails may be retained for as long as needed to handle the request and maintain support history.</p>`;
}

function securitySection() {
  return `<h2>Security</h2>
<p>The app keeps protection settings local and stores the parent PIN as hash metadata instead of plain text. Billing backend communication is intended to use HTTPS in production.</p>
<p>No system can be guaranteed perfectly secure, but the app is designed to avoid unnecessary data collection and to keep billing infrastructure separate from child protection behavior.</p>`;
}

function rightsSection() {
  return `<h2>Your Choices And Rights</h2>
<p>You can remove local app data by clearing the app data in Android settings or uninstalling the app. If Device Admin tamper protection is active, turn it off first in Android settings. You can contact the publisher about privacy, support, or billing entitlement records at <a href="mailto:${siteConfig.contactEmail}">${siteConfig.contactEmail}</a>.</p>
<p>Google Play purchases, cancellations, and refunds are handled through Google Play. Use Google Play subscription management for subscription changes where applicable.</p>`;
}

function limitsSection() {
  return `<h2>Important Limitations</h2>
<p>Blocking works only when Protection is ON, the parent setup is complete, and Android Accessibility Service is active. Optional Device Admin tamper protection can make uninstall harder while active, but if Accessibility permission is turned off, blocking stops.</p>
<p>The app focuses on supported short-video surfaces, not every video or every app.</p>`;
}

function contactSection() {
  return `<h2 id="contact">Contact</h2>
<div class="contact-block">
  <p><strong>Privacy and support contact:</strong> <a href="mailto:${siteConfig.contactEmail}">${siteConfig.contactEmail}</a></p>
  <p><strong>Publisher:</strong> ${siteConfig.publisher}</p>
  <p>${secondaryCta("Support page", "/support")}</p>
</div>`;
}
