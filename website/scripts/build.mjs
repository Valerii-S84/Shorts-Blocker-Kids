import { promises as fs } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { pages } from "../src/pages/index.mjs";
import { siteConfig } from "../src/config.mjs";

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const distDir = path.join(rootDir, "dist");
const publicDir = path.join(rootDir, "public");

await fs.rm(distDir, { recursive: true, force: true });
await fs.mkdir(distDir, { recursive: true });

await copyDirectory(publicDir, distDir);
await copyFile("src/styles.css", "assets/styles.css");
await copyFile("src/scripts/site.js", "assets/site.js");
await writePages();
await writeTextFile("robots.txt", robotsTxt());
await writeTextFile("sitemap.xml", sitemapXml());

async function writePages() {
  await Promise.all(
    pages.map((page) => writeTextFile(page.outputPath, page.render())),
  );
}

async function copyFile(source, destination) {
  const sourcePath = path.join(rootDir, source);
  const destinationPath = path.join(distDir, destination);
  await fs.mkdir(path.dirname(destinationPath), { recursive: true });
  await fs.copyFile(sourcePath, destinationPath);
}

async function copyDirectory(sourceDir, destinationDir) {
  const entries = await fs.readdir(sourceDir, { withFileTypes: true });
  await fs.mkdir(destinationDir, { recursive: true });
  await Promise.all(
    entries.map(async (entry) => {
      const sourcePath = path.join(sourceDir, entry.name);
      const destinationPath = path.join(destinationDir, entry.name);
      if (entry.isDirectory()) {
        await copyDirectory(sourcePath, destinationPath);
        return;
      }
      await fs.copyFile(sourcePath, destinationPath);
    }),
  );
}

async function writeTextFile(relativePath, content) {
  const outputPath = path.join(distDir, relativePath);
  await fs.mkdir(path.dirname(outputPath), { recursive: true });
  await fs.writeFile(outputPath, content, "utf8");
}

function robotsTxt() {
  return `User-agent: *
Allow: /
Sitemap: ${siteConfig.canonicalDomain}/sitemap.xml
`;
}

function sitemapXml() {
  const urls = pages
    .map((page) => {
      const url = page.route === "/" ? siteConfig.canonicalDomain : `${siteConfig.canonicalDomain}${page.route}`;
      return `  <url>
    <loc>${url}</loc>
  </url>`;
    })
    .join("\n");

  return `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${urls}
</urlset>
`;
}
