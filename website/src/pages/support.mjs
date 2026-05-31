import {
  organizationStructuredData,
  pageLayout,
  secondaryCta,
  sectionEyebrow,
} from "../components.mjs";
import { siteConfig } from "../config.mjs";

export function renderSupportPage() {
  const body = `<section class="legal-hero">
  <div class="container narrow">
    ${sectionEyebrow("Support")}
    <h1>Support</h1>
    <p class="lead">Help with setup, Accessibility permission, billing, common issues, and privacy questions for ${siteConfig.appName}.</p>
    <p><a class="button button-primary" href="mailto:${siteConfig.contactEmail}">Email support</a></p>
  </div>
</section>
<section class="page-section">
  <div class="container support-grid">
    ${contactCard()}
    ${setupCard()}
    ${accessibilityCard()}
    ${billingCard()}
    ${commonIssuesCard()}
    ${privacyCard()}
  </div>
</section>`;

  return pageLayout({
    title: "Support",
    description:
      "Support page for Shorts Blocker Kids setup, Accessibility permission, billing, and privacy questions.",
    path: "/support",
    activePath: "/support",
    body,
    jsonLd: organizationStructuredData(),
  });
}

function contactCard() {
  return `<article class="feature-card support-card">
  <h2>Contact</h2>
  <p>Email: <a href="mailto:${siteConfig.contactEmail}">${siteConfig.contactEmail}</a></p>
  <p>Include your device model, Android version, app version if visible, what you were trying to do, and what happened.</p>
</article>`;
}

function setupCard() {
  return `<article class="feature-card support-card">
  <h2>Setup Help</h2>
  <ol>
    <li>Install ${siteConfig.appName} from the intended Google Play path when it is available.</li>
    <li>Open the app and create the parent PIN.</li>
    <li>Review the Accessibility permission explanation.</li>
    <li>Enable Shorts Blocker Kids Protection in Android Accessibility settings.</li>
    <li>Return to the app, choose protected apps, and confirm Protection is ON.</li>
  </ol>
</article>`;
}

function accessibilityCard() {
  return `<article class="feature-card support-card">
  <h2>Accessibility Permission</h2>
  <p>The app needs Android Accessibility permission so it can detect supported short-video surfaces locally and show a blocking overlay.</p>
  <p>If blocking stops, check that the service is still enabled, battery settings are not restricting the app, and Protection is ON.</p>
</article>`;
}

function billingCard() {
  return `<article class="feature-card support-card">
  <h2>Billing Help</h2>
  <p>Subscriptions are intended to be handled through Google Play Billing. The website does not collect payment card details and does not provide a separate payment flow for the Play-distributed app.</p>
  <p>For cancellations, subscription changes, or refund requests, use Google Play subscription and refund flows.</p>
</article>`;
}

function commonIssuesCard() {
  return `<article class="feature-card support-card">
  <h2>Common Issues</h2>
  <h3>Blocking does not appear</h3>
  <p>Check that Protection is ON, Accessibility permission is enabled, parent setup is complete, and the app you are using is supported.</p>
  <h3>Normal videos are blocked</h3>
  <p>Contact support with the app name, screen description, and device details. Do not send private messages, account details, or child data.</p>
  <h3>PIN is not accepted</h3>
  <p>Use the correct parent PIN. Repeated failures can temporarily lock PIN entry.</p>
</article>`;
}

function privacyCard() {
  return `<article class="feature-card support-card">
  <h2>Privacy Contact</h2>
  <p>For privacy questions or billing entitlement data requests, email <a href="mailto:${siteConfig.contactEmail}">${siteConfig.contactEmail}</a>.</p>
  <p>${secondaryCta("Read Privacy Policy", "/privacy")}</p>
</article>`;
}
