import { renderHomePage } from "./home.mjs";
import { renderPrivacyPage } from "./privacy.mjs";
import { renderSupportPage } from "./support.mjs";
import { renderTermsPage } from "./terms.mjs";

export const pages = [
  {
    route: "/",
    outputPath: "index.html",
    render: renderHomePage,
  },
  {
    route: "/privacy",
    outputPath: "privacy/index.html",
    render: renderPrivacyPage,
  },
  {
    route: "/support",
    outputPath: "support/index.html",
    render: renderSupportPage,
  },
  {
    route: "/terms",
    outputPath: "terms/index.html",
    render: renderTermsPage,
  },
];
