import {
  appStructuredData,
  faqStructuredData,
  pageLayout,
  primaryCta,
  secondaryCta,
  sectionEyebrow,
  statusBadge,
} from "../components.mjs";
import { protectedPlatforms, siteConfig, unsupportedPlatforms } from "../config.mjs";

const faqItems = [
  {
    question: "Does the app read private messages?",
    answer:
      "No. Shorts Blocker Kids is not built to read private messages, chats, comments, account names, or personal communication. The Accessibility Service is used for local short-video surface detection.",
  },
  {
    question: "Why does it need Accessibility permission?",
    answer:
      "Android does not provide a normal app API that tells Shorts Blocker Kids when Shorts, Reels, or similar surfaces are open inside supported apps. Accessibility permission lets the app inspect supported app UI locally and show the blocking overlay when protection is enabled.",
  },
  {
    question: "Does it track my child?",
    answer:
      "No. The app has no account system, ads, analytics, cloud sync, or hidden monitoring. It does not collect watch history, video titles, URLs, messages, audio, screen recordings, location, contacts, or browsing history.",
  },
  {
    question: "Can my child disable it?",
    answer:
      "Android lets a device user turn off Accessibility permission or uninstall apps. Shorts Blocker Kids uses a parent PIN for in-app settings and temporary allow, but it does not use aggressive anti-uninstall behavior.",
  },
  {
    question: "Does it block all videos?",
    answer:
      "No. The goal is to block supported short-form video surfaces, not every video in every app. Normal YouTube videos and unsupported apps are not claimed as blocked.",
  },
  {
    question: "Does it require internet?",
    answer:
      "The blocking behavior runs locally. Internet access is used for Google Play Billing and production subscription verification when those flows are active.",
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
${supportedAppsSection()}
${howItWorksSection()}
${trustSection()}
${metricsSection()}
${videoSection()}
${testimonialsSection()}
${faqSection()}
${finalCtaSection()}`;

  return pageLayout({
    title: siteConfig.appName,
    description:
      "A trust-focused parental control helper for reducing children's exposure to short-form video feeds on supported Android apps.",
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
      ${sectionEyebrow("Parental short-video control for Android")}
      <h1>${siteConfig.appName}</h1>
      <p class="lead">Help reduce a child's exposure to short-form video feeds such as Shorts, Reels, TikTok, and similar endless-scroll surfaces, without pretending to be a surveillance tool.</p>
      <div class="hero-actions">
        ${primaryCta()}
        ${secondaryCta("Read Privacy Policy", "/privacy")}
      </div>
      <div class="hero-note">
        ${statusBadge("No account")}
        ${statusBadge("No ads")}
        ${statusBadge("No hidden monitoring")}
      </div>
    </div>
    <div class="device-showcase" aria-label="Shorts Blocker Kids app mockup">
      <div class="phone-frame">
        <div class="phone-speaker"></div>
        <div class="mock-screen">
          <div class="mock-topline">
            <img src="/assets/app-icon.png" alt="" width="34" height="34">
            <span>Protection</span>
          </div>
          <div class="mock-status active">Short Video Protection: ON</div>
          <div class="mock-row">
            <span>YouTube Shorts</span>
            <strong>supported</strong>
          </div>
          <div class="mock-row">
            <span>TikTok main</span>
            <strong>needs QA</strong>
          </div>
          <div class="mock-row">
            <span>PIN</span>
            <strong>parent controlled</strong>
          </div>
          <div class="mock-overlay">
            <span>Short video surface blocked</span>
            <button type="button">Enter PIN</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</section>`;
}

function problemSection() {
  return `<section class="page-section">
  <div class="container section-grid">
    <div>
      ${sectionEyebrow("The problem")}
      <h2>Short-video feeds can turn a quick check into a long scroll.</h2>
    </div>
    <div class="feature-grid three">
      <article class="feature-card">
        <h3>Endless surfaces</h3>
        <p>Shorts, Reels, and similar feeds present rapid clips in a swipeable format that can keep children inside one surface for longer than planned.</p>
      </article>
      <article class="feature-card">
        <h3>Parents need boundaries</h3>
        <p>Many families want practical controls for when these surfaces are available, while still allowing ordinary app use where appropriate.</p>
      </article>
      <article class="feature-card">
        <h3>Trust matters</h3>
        <p>A parental helper should explain what it does, avoid hidden monitoring, and keep sensitive child data out of unnecessary systems.</p>
      </article>
    </div>
  </div>
</section>`;
}

function solutionSection() {
  const items = [
    ["Blocks short-form surfaces", "Shows a blocking screen when a supported short-video surface is detected and protection is active."],
    ["Parent-controlled settings", "Protected apps, protection status, and temporary allow decisions stay under the parent flow."],
    ["PIN protection", "The parent PIN is stored as hash metadata, not as plain text."],
    ["Temporary allow", "A parent can allow access for a short period when needed, then protection resumes."],
    ["Privacy-first design", "No account, no ads, no analytics, no child profile selling, and no watch history storage."],
  ];

  return `<section class="page-section section-alt" id="solution">
  <div class="container">
    ${sectionEyebrow("The solution")}
    <h2>A focused blocker for the parts of apps that need boundaries.</h2>
    <div class="feature-grid">
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

function supportedAppsSection() {
  const supportedRows = protectedPlatforms
    .map(
      (platform) => `<tr>
      <th scope="row">${platform.name}</th>
      <td>${platform.status}</td>
      <td>${platform.detail}</td>
    </tr>`,
    )
    .join("");

  return `<section class="page-section" id="supported-apps">
  <div class="container">
    ${sectionEyebrow("Supported apps")}
    <h2>Support claims stay tied to current app evidence.</h2>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th scope="col">Surface</th>
            <th scope="col">Current status</th>
            <th scope="col">Notes</th>
          </tr>
        </thead>
        <tbody>${supportedRows}</tbody>
      </table>
    </div>
    <p class="muted">Not currently claimed: ${unsupportedPlatforms.join("; ")}.</p>
  </div>
</section>`;
}

function howItWorksSection() {
  const steps = [
    ["Enable Accessibility permission", "Review the in-app disclosure, then enable Shorts Blocker Kids Protection in Android settings."],
    ["Choose apps to protect", "Keep the supported surfaces enabled or turn off a surface from the parent settings flow."],
    ["Set the parent PIN", "Use the PIN to protect settings and temporary allow decisions."],
    ["Block short-video surfaces", "When protection is active, the app detects supported short-video surfaces locally and shows the blocker."],
    ["Temporarily allow when needed", "A parent can allow access for a limited time without turning protection off permanently."],
  ];

  return `<section class="page-section section-alt" id="how-it-works">
  <div class="container">
    ${sectionEyebrow("How it works")}
    <h2>Clear setup, clear permission, clear limits.</h2>
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

function trustSection() {
  return `<section class="page-section" id="trust">
  <div class="container trust-grid">
    <div>
      ${sectionEyebrow("Trust and privacy")}
      <h2>Built as a parental helper, not spyware.</h2>
      <p class="lead small-lead">AccessibilityService is used to detect supported short-video surfaces on the device and show a blocking overlay. It is not used to sell child profiles, read private messages, record the screen, or create hidden monitoring.</p>
      <div class="hero-actions">
        ${secondaryCta("Privacy Policy", "/privacy")}
        ${secondaryCta("Support", "/support")}
      </div>
    </div>
    <div class="trust-list">
      <div><strong>No child profile selling</strong><span>There is no advertising profile, account system, or analytics SDK in the current app.</span></div>
      <div><strong>No unnecessary tracking</strong><span>Protection settings and PIN hash metadata stay locally on the phone.</span></div>
      <div><strong>Billing separated</strong><span>The billing backend is for Google Play subscription verification only.</span></div>
    </div>
  </div>
</section>`;
}

function metricsSection() {
  return `<section class="page-section section-alt" aria-labelledby="metrics-title">
  <div class="container metrics-band" data-metrics-widget data-endpoint="${siteConfig.metricsEndpoint}">
    <div>
      ${sectionEyebrow("Usage signal")}
      <h2 id="metrics-title">Real-data-ready metrics</h2>
      <p class="muted">This section only shows live values when a verified metrics endpoint is configured later. Until then it stays neutral.</p>
    </div>
    <div class="metric-widget" aria-live="polite">
      <strong data-metrics-status>Private testing in progress</strong>
      <span data-metrics-detail>No public install or usage numbers are published yet.</span>
    </div>
  </div>
</section>`;
}

function videoSection() {
  return `<section class="page-section" id="video">
  <div class="container video-grid">
    <div>
      ${sectionEyebrow("Demo video")}
      <h2>Product walkthrough space is reserved.</h2>
      <p>A public demo video will be added after the Play review walkthrough is recorded and approved for publication. No demo claim is made before the video exists.</p>
    </div>
    <div class="video-placeholder" role="img" aria-label="Demo video placeholder">
      <div class="play-mark"></div>
      <span>Demo video coming later</span>
    </div>
  </div>
</section>`;
}

function testimonialsSection() {
  return `<section class="page-section section-alt" id="feedback">
  <div class="container feedback-empty">
    ${sectionEyebrow("Parent feedback")}
    <h2>Feedback will be added after public testing.</h2>
    <p>No testimonials or user quotes are published until they come from real parents and are approved for use.</p>
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
      ${sectionEyebrow("Ready for Play review and public trust")}
      <h2>Install path stays with Google Play.</h2>
      <p>Use the privacy and support pages for Play Console review and parent trust while the public Google Play listing is being prepared.</p>
    </div>
    <div class="hero-actions">
      ${primaryCta()}
      ${secondaryCta("Contact support", "/support")}
      ${secondaryCta("Privacy", "/privacy")}
    </div>
  </div>
</section>`;
}
