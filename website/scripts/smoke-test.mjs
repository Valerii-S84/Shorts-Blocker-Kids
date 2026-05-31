import http from "node:http";
import { promises as fs } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const distDir = path.join(rootDir, "dist");
const requiredRoutes = ["/", "/privacy", "/support", "/terms"];
const requiredFiles = [
  "index.html",
  "privacy/index.html",
  "support/index.html",
  "terms/index.html",
  "assets/styles.css",
  "assets/site.js",
  "assets/app-icon.png",
  "favicon.png",
  "site.webmanifest",
  "robots.txt",
  "sitemap.xml",
];
const forbiddenPatterns = [
  /10,?000\+?\s+users/i,
  /fake\s+review/i,
  /fake\s+testimonial/i,
  /download\s+apk/i,
  /movashield\.de/i,
  /Get it on Google Play/i,
];

await assertFilesExist();
await assertHtmlInvariants();
await assertRoutesServe();
console.log("Website smoke test passed.");

async function assertFilesExist() {
  await Promise.all(
    requiredFiles.map(async (relativePath) => {
      const fullPath = path.join(distDir, relativePath);
      const stats = await fs.stat(fullPath);
      if (!stats.isFile()) {
        throw new Error(`Expected file: ${relativePath}`);
      }
    }),
  );
}

async function assertHtmlInvariants() {
  const htmlFiles = requiredFiles.filter((file) => file.endsWith(".html"));
  for (const relativePath of htmlFiles) {
    const content = await fs.readFile(path.join(distDir, relativePath), "utf8");
    assertIncludes(content, "https://shortsblockerkids.de", relativePath);
    assertIncludes(content, "svalerii535@gmail.com", relativePath);
    assertIncludes(content, "Valerii Serputko", relativePath);
    assertIncludes(content, "Coming soon on Google Play", relativePath);
    forbiddenPatterns.forEach((pattern) => {
      if (pattern.test(content)) {
        throw new Error(`Forbidden pattern ${pattern} in ${relativePath}`);
      }
    });
  }
}

async function assertRoutesServe() {
  const server = http.createServer(async (request, response) => {
    try {
      const filePath = routeToFilePath(request.url ?? "/");
      const content = await fs.readFile(filePath);
      response.writeHead(200, {
        "Content-Type": contentType(filePath),
      });
      response.end(content);
    } catch {
      response.writeHead(404);
      response.end("Not found");
    }
  });

  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const address = server.address();
  const port = typeof address === "object" && address ? address.port : 0;

  try {
    for (const route of requiredRoutes) {
      const response = await fetch(`http://127.0.0.1:${port}${route}`);
      if (!response.ok) {
        throw new Error(`${route} returned HTTP ${response.status}`);
      }
      const body = await response.text();
      assertIncludes(body, "Shorts Blocker Kids", route);
    }
  } finally {
    server.close();
  }
}

function routeToFilePath(route) {
  const cleanRoute = route.split("?")[0].replace(/\/+$/, "") || "/";
  if (cleanRoute === "/") {
    return path.join(distDir, "index.html");
  }
  return path.join(distDir, cleanRoute.slice(1), "index.html");
}

function assertIncludes(content, expected, fileName) {
  if (!content.includes(expected)) {
    throw new Error(`${fileName} does not include ${expected}`);
  }
}

function contentType(filePath) {
  if (filePath.endsWith(".css")) {
    return "text/css; charset=utf-8";
  }
  if (filePath.endsWith(".js")) {
    return "application/javascript; charset=utf-8";
  }
  if (filePath.endsWith(".png")) {
    return "image/png";
  }
  return "text/html; charset=utf-8";
}
