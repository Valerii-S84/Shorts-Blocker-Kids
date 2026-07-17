import {
  appStructuredData,
  faqStructuredData,
  pageLayout,
  primaryCta,
  secondaryCta,
  sectionEyebrow,
  statusBadge,
} from "../components.mjs";
import { protectedPlatforms, siteConfig } from "../config.mjs";

const faqItems = [
  {
    question: "What does Shorts Blocker Kids block?",
    answer:
      "It focuses on supported short-video feeds such as Shorts, Reels, TikTok-style feeds, and similar endless-scroll video surfaces. Normal app use can remain possible where the app can separate the feed from the rest of the app.",
  },
  {
    question: "Which apps are supported?",
    answer:
      "Shorts Blocker Kids supports YouTube Shorts, TikTok, Instagram Reels, and Facebook Reels. Support can depend on app versions and Android behavior. The app focuses on supported short-video surfaces, not every video screen.",
  },
  {
    question: "Why does it need Accessibility permission?",
    answer:
      "Android does not provide a standard app setting for detecting Shorts, Reels, or similar feeds inside other apps. Accessibility permission lets Shorts Blocker Kids recognize supported screens locally and show the blocker when protection is enabled.",
  },
  {
    question: "Does the app read private messages?",
    answer:
      "No. The app is not built to read private messages, chats, comments, account names, or personal communication.",
  },
  {
    question: "Does it track my child?",
    answer:
      "No. The app has no account system, ads, analytics, or cloud sync. It does not collect watch history, video titles, URLs, messages, audio, screen recordings, location, contacts, or browsing history.",
  },
  {
    question: "Can my child disable it?",
    answer:
      "Android allows a device user to turn off Accessibility permission. Shorts Blocker Kids uses a parent PIN for in-app settings and temporary allow. Optional Device Admin tamper protection can make uninstall harder while it is active, after a separate parent-facing disclosure.",
  },
  {
    question: "Does it require internet?",
    answer:
      "Blocking runs locally on the phone. Internet access is used for Google Play Billing and subscription verification when those flows are active.",
  },
  {
    question: "How does subscription work?",
    answer:
      "Subscriptions are intended to be managed through Google Play Billing. The website does not collect payment card data and does not provide an external payment flow for the Play-distributed app.",
  },
  {
    question: "How can I contact support?",
    answer: `Email ${siteConfig.contactEmail} for setup, billing, or privacy questions.`,
  },
];

export function renderHomePage() {
  const body = `${heroSection()}
${problemSection()}
${solutionSection()}
${howItWorksSection()}
${supportedAppsSection()}
${trustSection()}
${demoSection()}
${feedbackSection()}
${faqSection()}
${finalCtaSection()}`;

  return pageLayout({
    title: siteConfig.appName,
    description:
      "A parental control helper for reducing children's exposure to endless short-video feeds on supported Android apps.",
    path: "/",
    activePath: "/",
    body,
    jsonLd: [appStructuredData(), faqStructuredData(faqItems)],
  });
}

function heroSection() {
  return `<section class="hero">
  <div class="container hero-grid">
    <div class="hero-copy">
      <div class="hero-brandmark">
        <img src="/assets/app-icon.png" alt="${siteConfig.appName} app icon" width="68" height="68">
        <span>${siteConfig.appName}</span>
      </div>
      ${sectionEyebrow("Parental short-video control for Android")}
      <h1>Help your child avoid endless short-video feeds.</h1>
      <p class="lead">${siteConfig.appName} gives parents a simple way to block Shorts, TikTok-style feeds, Reels, and other supported short-video surfaces on Android. Keep normal app use possible. Put boundaries around the parts that cause the problem.</p>
      <div class="hero-actions">
        ${primaryCta()}
        ${secondaryCta("Read Privacy Policy", "/privacy")}
      </div>
      <div class="hero-note">
        ${statusBadge("Parent PIN")}
        ${statusBadge("No ads")}
        ${statusBadge("No account")}
      </div>
    </div>
    ${appMockup()}
  </div>
</section>`;
}

function appMockup() {
  return `<div class="device-showcase" aria-label="Shorts Blocker Kids app preview">
  <div class="phone-frame">
    <div class="phone-speaker"></div>
    <div class="mock-screen">
      <div class="mock-app-header">
        <img src="/assets/app-icon.png" alt="" width="34" height="34">
        <div>
          <strong>${siteConfig.appName}</strong>
          <span>Parent controls</span>
        </div>
      </div>
      <section class="mock-card">
        <h2>Short Video Protection</h2>
        <div class="mock-row">
          <span>Short Video Protection</span>
          <strong>ON</strong>
        </div>
        <div class="mock-platform-list">
          <div><span>YouTube Shorts</span><strong>protected</strong></div>
          <div><span>TikTok</span><strong>protected</strong></div>
          <div><span>Instagram Reels</span><strong>protected</strong></div>
          <div><span>Facebook Reels</span><strong>protected</strong></div>
        </div>
        <div class="mock-row">
          <span>PIN</span>
          <strong>created</strong>
        </div>
        <div class="mock-row">
          <span>Protection status</span>
          <strong>active</strong>
        </div>
        <button type="button" class="mock-primary">Open Accessibility Settings</button>
        <button type="button" class="mock-secondary">Privacy Policy</button>
      </section>
      <section class="mock-blocker">
        <strong>Short video blocked</strong>
        <span>Parent PIN is required to allow short videos or change protection.</span>
        <div class="mock-blocker-actions">
          <button type="button">Exit to phone home</button>
          <button type="button">Enter PIN</button>
        </div>
      </section>
    </div>
  </div>
</div>`;
}

function problemSection() {
  return `<section class="page-section">
  <div class="container section-grid">
    <div>
      ${sectionEyebrow("The problem")}
      <h2>Shorts, Reels, and TikTok-style feeds are built to keep children scrolling.</h2>
    </div>
    <div class="feature-grid three">
      <article class="feature-card">
        <h3>Endless feeds</h3>
        <p>Shorts, Reels, TikTok-style feeds, and similar surfaces make it easy for a quick check to become a much longer session.</p>
      </article>
      <article class="feature-card">
        <h3>Hard stopping points</h3>
        <p>Children often need a clear boundary before the next swipe, not another reminder after the scroll has already started.</p>
      </article>
      <article class="feature-card">
        <h3>Simple parent control</h3>
        <p>Parents need a focused tool that sets limits without turning the whole phone into a complicated monitoring system.</p>
      </article>
    </div>
  </div>
</section>`;
}

function solutionSection() {
  const items = [
    ["Block supported feeds", "Show a blocking screen when Shorts, Reels, TikTok-style feeds, or similar supported surfaces open."],
    ["Keep parent control", "Choose protected apps and manage protection from the parent flow."],
    ["Protect settings with a PIN", "Use a parent PIN for settings changes and temporary access decisions."],
    ["Temporarily allow access", "Allow supported short-video surfaces for a limited time when there is a good reason, then protection resumes."],
  ];

  return `<section class="page-section section-alt" id="solution">
  <div class="container">
    ${sectionEyebrow("The solution")}
    <h2>Clear boundaries without blocking the whole phone.</h2>
    <div class="feature-grid four">
      ${items
        .map(
          ([title, text]) => `<article class="feature-card">
        <h3>${title}</h3>
        <p>${text}</p>
      </article>`,
        )
        .join("")}
    </div>
  </div>
</section>`;
}

function howItWorksSection() {
  const steps = [
    ["Install the app", "Google Play launch is being prepared. The public install path will stay with Google Play."],
    ["Create the parent PIN", "Set the PIN used for protected settings and temporary allow."],
    ["Choose protected apps", "Turn on protection for YouTube Shorts, TikTok, Instagram Reels, and Facebook Reels."],
    ["Enable Accessibility permission", "Review the permission disclosure and enable Shorts Blocker Kids Protection in Android settings."],
    ["Optional tamper protection", "Enable Device Admin tamper protection after its separate parent-facing disclosure."],
    ["Protection starts", "When a supported short-video surface opens, the blocker appears and asks for the parent PIN."],
  ];

  return `<section class="page-section" id="how-it-works">
  <div class="container">
    ${sectionEyebrow("How it works")}
    <h2>Set it up once, then let the boundary do its job.</h2>
    <div class="steps">
      ${steps
        .map(
          ([title, text], index) => `<article class="step">
        <span>${index + 1}</span>
        <h3>${title}</h3>
        <p>${text}</p>
      </article>`,
        )
        .join("")}
    </div>
  </div>
</section>`;
}

function supportedAppsSection() {
  const supportedCards = protectedPlatforms
    .map(
      (platform) => `<article class="platform-card">
      <div>
        <h3>${platform.name}</h3>
        <span class="platform-status">${platform.status}</span>
      </div>
      <p>${platform.detail}</p>
    </article>`,
    )
    .join("");

  return `<section class="page-section section-alt" id="supported-apps">
  <div class="container split-heading">
    <div>
      ${sectionEyebrow("Supported platforms")}
      <h2>Designed for popular short-video surfaces.</h2>
    </div>
    <p class="section-intro">Works with supported short-video feeds on Android while keeping normal app use possible where supported.</p>
  </div>
  <div class="container platform-grid">${supportedCards}</div>
</section>`;
}

function trustSection() {
  return `<section class="page-section" id="trust">
  <div class="container trust-grid">
    <div>
      ${sectionEyebrow("Privacy and trust")}
      <h2>Built for parents who want boundaries, not extra noise.</h2>
      <p class="lead small-lead">Blocking runs locally on the phone for supported short-video surfaces. The app is designed to avoid accounts, ads, analytics, and unnecessary child data collection.</p>
      <div class="hero-actions">
        ${secondaryCta("Privacy Policy", "/privacy")}
        ${secondaryCta("Support", "/support")}
      </div>
    </div>
    <div class="trust-list">
      <div><strong>Runs locally for blocking</strong><span>Supported screens are recognized on the device so the blocker can appear when protection is enabled.</span></div>
      <div><strong>No ads or account</strong><span>The current app has no advertising SDK, analytics SDK, account system, or cloud sync.</span></div>
      <div><strong>Parent PIN</strong><span>Settings and temporary allow decisions are protected by the parent PIN flow.</span></div>
      <div><strong>Google Play billing</strong><span>Subscriptions are intended to be handled through Google Play Billing.</span></div>
    </div>
  </div>
</section>`;
}

function demoSection() {
  return `<section class="page-section" id="demo">
  <div class="container video-grid">
    <div>
      ${sectionEyebrow("Demo")}
      <h2>Demo video coming soon.</h2>
      <p>A short walkthrough will show setup, the parent PIN, Accessibility permission, and the blocking screen.</p>
    </div>
    <div class="video-placeholder" role="img" aria-label="Demo video coming soon">
      <div class="play-mark"></div>
      <span>Demo video coming soon</span>
    </div>
  </div>
</section>`;
}

function feedbackSection() {
  return `<section class="page-section section-alt" id="feedback">
  <div class="container feedback-empty">
    ${sectionEyebrow("Parent feedback")}
    <h2>Parent feedback will be added after public testing.</h2>
    <p>Only real, approved parent feedback will appear here.</p>
  </div>
</section>`;
}

function faqSection() {
  return `<section class="page-section" id="faq">
  <div class="container">
    ${sectionEyebrow("FAQ")}
    <h2>Common questions</h2>
    <div class="faq-list">
      ${faqItems
        .map(
          (item) => `<details>
        <summary>${item.question}</summary>
        <p>${item.answer}</p>
      </details>`,
        )
        .join("")}
    </div>
  </div>
</section>`;
}

function finalCtaSection() {
  return `<section class="final-cta">
  <div class="container final-cta-inner">
    <div>
      ${sectionEyebrow("Google Play launch is being prepared")}
      <h2>Give short-video feeds a clear boundary.</h2>
      <p>Follow the Google Play launch status or contact support with setup, privacy, or billing questions.</p>
    </div>
    <div class="hero-actions">
      ${primaryCta()}
      ${secondaryCta("Contact support", "/support")}
      ${secondaryCta("Privacy", "/privacy")}
    </div>
  </div>
</section>`;
}
