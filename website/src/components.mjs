import { installCta, siteConfig } from "./config.mjs";

const navItems = [
  { label: "Home", href: "/" },
  { label: "Privacy", href: "/privacy" },
  { label: "Support", href: "/support" },
  { label: "Terms", href: "/terms" },
];

export function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

export function pageUrl(path) {
  if (path === "/") {
    return siteConfig.canonicalDomain;
  }
  return `${siteConfig.canonicalDomain}${path}`;
}

export function pageLayout({ title, description, path, activePath, body, jsonLd }) {
  const canonical = pageUrl(path);
  const pageTitle =
    title === siteConfig.appName ? title : `${title} | ${siteConfig.appName}`;
  const structuredData = jsonLd ? jsonScript(jsonLd) : "";

  return `<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${escapeHtml(pageTitle)}</title>
    <meta name="description" content="${escapeHtml(description)}">
    <link rel="canonical" href="${escapeHtml(canonical)}">
    <meta name="robots" content="index,follow">
    <meta property="og:type" content="website">
    <meta property="og:title" content="${escapeHtml(pageTitle)}">
    <meta property="og:description" content="${escapeHtml(description)}">
    <meta property="og:url" content="${escapeHtml(canonical)}">
    <meta property="og:site_name" content="${escapeHtml(siteConfig.appName)}">
    <meta property="og:image" content="${escapeHtml(siteConfig.canonicalDomain)}/assets/app-icon.png">
    <meta name="twitter:card" content="summary">
    <meta name="twitter:title" content="${escapeHtml(pageTitle)}">
    <meta name="twitter:description" content="${escapeHtml(description)}">
    <meta name="theme-color" content="#1f6f54">
    <link rel="icon" type="image/png" href="/favicon.png">
    <link rel="apple-touch-icon" href="/assets/app-icon.png">
    <link rel="manifest" href="/site.webmanifest">
    <link rel="stylesheet" href="/assets/styles.css">
    ${structuredData}
  </head>
  <body>
    ${siteHeader(activePath)}
    <main>
      ${body}
    </main>
    ${siteFooter()}
    <script src="/assets/site.js" defer></script>
  </body>
</html>
`;
}

export function siteHeader(activePath) {
  const links = navItems
    .map((item) => {
      const isActive = item.href === activePath;
      return `<a href="${item.href}" ${isActive ? 'aria-current="page"' : ""}>${item.label}</a>`;
    })
    .join("");

  return `<header class="site-header">
  <div class="container header-inner">
    <a class="brand" href="/" aria-label="${escapeHtml(siteConfig.appName)} home">
      <img src="/assets/app-icon.png" alt="" width="40" height="40">
      <span>${escapeHtml(siteConfig.appName)}</span>
    </a>
    <nav class="site-nav" aria-label="Primary navigation">
      ${links}
    </nav>
  </div>
</header>`;
}

export function siteFooter() {
  return `<footer class="site-footer">
  <div class="container footer-grid">
    <div>
      <a class="brand footer-brand" href="/" aria-label="${escapeHtml(siteConfig.appName)} home">
        <img src="/assets/app-icon.png" alt="" width="36" height="36">
        <span>${escapeHtml(siteConfig.appName)}</span>
      </a>
      <p class="muted">Publisher: ${escapeHtml(siteConfig.publisher)}</p>
      <p class="muted">Contact: <a href="mailto:${escapeHtml(siteConfig.contactEmail)}">${escapeHtml(siteConfig.contactEmail)}</a></p>
    </div>
    <div>
      <h2>Website</h2>
      <a href="/privacy">Privacy</a>
      <a href="/support">Support</a>
      <a href="/terms">Terms</a>
    </div>
    <div>
      <h2>Install</h2>
      ${primaryCta("footer")}
      <p class="muted small">Google Play is the intended production install path. No direct APK download is offered here.</p>
    </div>
  </div>
</footer>`;
}

export function primaryCta(context = "main") {
  const cta = installCta();
  const className = context === "footer" ? "button button-secondary" : "button button-primary";
  if (cta.disabled) {
    return `<span class="${className} is-disabled" aria-disabled="true">${escapeHtml(cta.label)}</span>`;
  }
  return `<a class="${className}" href="${escapeHtml(cta.href)}" rel="noopener">${escapeHtml(cta.label)}</a>`;
}

export function secondaryCta(label, href) {
  return `<a class="button button-ghost" href="${escapeHtml(href)}">${escapeHtml(label)}</a>`;
}

export function sectionEyebrow(text) {
  return `<p class="eyebrow">${escapeHtml(text)}</p>`;
}

export function statusBadge(text) {
  return `<span class="status-badge">${escapeHtml(text)}</span>`;
}

export function appStructuredData() {
  return {
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    name: siteConfig.appName,
    applicationCategory: "ParentingApplication",
    operatingSystem: "Android",
    publisher: {
      "@type": "Person",
      name: siteConfig.publisher,
    },
    url: siteConfig.canonicalDomain,
    privacyPolicy: `${siteConfig.canonicalDomain}/privacy`,
  };
}

export function organizationStructuredData() {
  return {
    "@context": "https://schema.org",
    "@type": "Organization",
    name: siteConfig.publisher,
    url: siteConfig.canonicalDomain,
    email: siteConfig.contactEmail,
  };
}

export function faqStructuredData(items) {
  return {
    "@context": "https://schema.org",
    "@type": "FAQPage",
    mainEntity: items.map((item) => ({
      "@type": "Question",
      name: item.question,
      acceptedAnswer: {
        "@type": "Answer",
        text: item.answer,
      },
    })),
  };
}

export function jsonScript(data) {
  const json = JSON.stringify(data).replaceAll("<", "\\u003c");
  return `<script type="application/ld+json">${json}</script>`;
}
