import { spawn, spawnSync } from "node:child_process";
import { promises as fs } from "node:fs";
import http from "node:http";
import net from "node:net";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const distDir = path.join(rootDir, "dist");
const previewDir = path.join(rootDir, "preview");
const screenshotSuffix = parseScreenshotSuffix();

const screenshots = [
  { path: "/", fileName: screenshotName("home-desktop.png"), width: 1440, height: 1200, mobile: false },
  { path: "/", fileName: screenshotName("home-tablet.png"), width: 820, height: 1200, mobile: true },
  { path: "/", fileName: screenshotName("home-mobile.png"), width: 390, height: 1200, mobile: true },
  { path: "/privacy", fileName: screenshotName("privacy-desktop.png"), width: 1440, height: 1200, mobile: false },
  { path: "/support", fileName: screenshotName("support-desktop.png"), width: 1440, height: 1200, mobile: false },
];

let chromeError = "";

async function main() {
  await assertDistExists();
  await fs.mkdir(previewDir, { recursive: true });

  const server = await startServer();
  const debugPort = await findFreePort();
  const chromePath = await findChrome();
  const profileDir = path.join(os.tmpdir(), `shorts-blocker-kids-chrome-${process.pid}`);
  await fs.mkdir(profileDir, { recursive: true });

  const chrome = spawn(chromePath, chromeArgs(debugPort, profileDir), {
    stdio: ["ignore", "ignore", "pipe"],
  });

  chrome.stderr.setEncoding("utf8");
  chrome.stderr.on("data", (chunk) => {
    chromeError += chunk;
  });

  try {
    await waitForChrome(debugPort);
    const client = await connectToPage(debugPort);
    await client.send("Page.enable");
    await client.send("Runtime.enable");

    for (const screenshot of screenshots) {
      await captureScreenshot(client, server.origin, screenshot);
      console.log(`Captured preview/${screenshot.fileName}`);
    }

    client.close();
  } finally {
    await stopChrome(chrome);
    server.close();
    await fs.rm(profileDir, { recursive: true, force: true }).catch(() => {});
  }
}

async function assertDistExists() {
  const indexPath = path.join(distDir, "index.html");
  try {
    const stats = await fs.stat(indexPath);
    if (!stats.isFile()) {
      throw new Error();
    }
  } catch {
    throw new Error("Run npm run build before capturing screenshots.");
  }
}

function screenshotName(fileName) {
  if (!screenshotSuffix) {
    return fileName;
  }
  return fileName.replace(/\.png$/, `${screenshotSuffix}.png`);
}

function parseScreenshotSuffix() {
  const suffixArg = process.argv.find((arg) => arg.startsWith("--suffix="));
  if (suffixArg) {
    return suffixArg.slice("--suffix=".length);
  }
  return process.env.SCREENSHOT_SUFFIX ?? "";
}

async function startServer() {
  const server = http.createServer(async (request, response) => {
    try {
      const filePath = routeToFilePath(request.url ?? "/");
      const content = await fs.readFile(filePath);
      response.writeHead(200, { "Content-Type": contentType(filePath) });
      response.end(content);
    } catch {
      response.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" });
      response.end("Not found");
    }
  });

  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const address = server.address();
  const port = typeof address === "object" && address ? address.port : 0;
  return {
    origin: `http://127.0.0.1:${port}`,
    close: () => server.close(),
  };
}

function routeToFilePath(route) {
  const cleanRoute = route.split("?")[0].replace(/\/+$/, "") || "/";
  if (cleanRoute === "/") {
    return path.join(distDir, "index.html");
  }
  if (path.extname(cleanRoute)) {
    return path.join(distDir, cleanRoute.slice(1));
  }
  return path.join(distDir, cleanRoute.slice(1), "index.html");
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
  if (filePath.endsWith(".webmanifest")) {
    return "application/manifest+json; charset=utf-8";
  }
  return "text/html; charset=utf-8";
}

async function findFreePort() {
  const server = net.createServer();
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const address = server.address();
  const port = typeof address === "object" && address ? address.port : 0;
  await new Promise((resolve) => server.close(resolve));
  return port;
}

async function findChrome() {
  const configuredPath = process.env.CHROME_BIN;
  const candidates = [
    configuredPath,
    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
    "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
    "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
    "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
    "/mnt/c/Program Files/Google/Chrome/Application/chrome.exe",
    "/mnt/c/Program Files (x86)/Google/Chrome/Application/chrome.exe",
    "/mnt/c/Program Files/Microsoft/Edge/Application/msedge.exe",
    "/mnt/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
    "google-chrome",
    "google-chrome-stable",
    "chromium",
    "chromium-browser",
  ].filter(Boolean);

  for (const candidate of candidates) {
    if (isFileCandidate(candidate)) {
      try {
        await fs.access(candidate);
        return candidate;
      } catch {
        continue;
      }
    }

    if (commandExists(candidate)) {
      return candidate;
    }
  }

  throw new Error("No Chrome or Edge executable found. Set CHROME_BIN to capture screenshots.");
}

function isFileCandidate(candidate) {
  return (
    path.isAbsolute(candidate) ||
    candidate.includes("/") ||
    candidate.includes("\\")
  );
}

function commandExists(commandName) {
  const lookup = process.platform === "win32" ? "where" : "command";
  const args = process.platform === "win32" ? [commandName] : ["-v", commandName];
  const result = spawnSync(lookup, args, {
    shell: process.platform !== "win32",
    stdio: "ignore",
  });
  return result.status === 0;
}

function chromeArgs(debugPort, profileDir) {
  return [
    "--headless=new",
    "--disable-gpu",
    "--disable-extensions",
    "--hide-scrollbars",
    "--no-first-run",
    "--run-all-compositor-stages-before-draw",
    `--remote-debugging-port=${debugPort}`,
    `--user-data-dir=${toChromePath(profileDir)}`,
    "about:blank",
  ];
}

async function stopChrome(chromeProcess) {
  if (chromeProcess.exitCode !== null) {
    return;
  }

  await new Promise((resolve) => {
    const timeout = setTimeout(resolve, 2000);
    chromeProcess.once("exit", () => {
      clearTimeout(timeout);
      resolve();
    });
    chromeProcess.kill();
  });
}

function toChromePath(filePath) {
  if (process.platform !== "linux" || !filePath.startsWith("/mnt/")) {
    return filePath;
  }

  const result = spawnSync("wslpath", ["-w", filePath], {
    encoding: "utf8",
  });
  if (result.status !== 0) {
    return filePath;
  }
  return result.stdout.trim();
}

async function waitForChrome(debugPort) {
  const startedAt = Date.now();
  while (Date.now() - startedAt < 10000) {
    try {
      const response = await fetch(`http://127.0.0.1:${debugPort}/json/version`);
      if (response.ok) {
        return;
      }
    } catch {
      await delay(100);
    }
  }

  throw new Error(`Chrome did not start. ${chromeError.trim()}`);
}

async function connectToPage(debugPort) {
  const response = await fetch(`http://127.0.0.1:${debugPort}/json/list`);
  const pages = await response.json();
  const page = pages.find((item) => item.type === "page");
  if (!page?.webSocketDebuggerUrl) {
    throw new Error("Chrome page target was not available.");
  }

  const socket = new WebSocket(page.webSocketDebuggerUrl);
  await new Promise((resolve, reject) => {
    socket.addEventListener("open", resolve, { once: true });
    socket.addEventListener("error", reject, { once: true });
  });
  return new CdpClient(socket);
}

async function captureScreenshot(client, origin, screenshot) {
  await client.send("Emulation.setDeviceMetricsOverride", {
    width: screenshot.width,
    height: screenshot.height,
    deviceScaleFactor: 1,
    mobile: screenshot.mobile,
  });

  const loadEvent = client.waitFor("Page.loadEventFired", 10000);
  await client.send("Page.navigate", { url: `${origin}${screenshot.path}` });
  await loadEvent;
  await client.send("Runtime.evaluate", {
    expression: "document.fonts ? document.fonts.ready.then(() => true) : true",
    awaitPromise: true,
  });
  await delay(250);

  const result = await client.send("Page.captureScreenshot", {
    format: "png",
    fromSurface: true,
    captureBeyondViewport: false,
  });
  const outputPath = path.join(previewDir, screenshot.fileName);
  await fs.writeFile(outputPath, Buffer.from(result.data, "base64"));
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

class CdpClient {
  constructor(socket) {
    this.socket = socket;
    this.nextId = 1;
    this.pending = new Map();
    this.listeners = new Map();

    socket.addEventListener("message", (event) => {
      const message = JSON.parse(event.data);
      if (message.id) {
        const pending = this.pending.get(message.id);
        if (!pending) {
          return;
        }
        this.pending.delete(message.id);
        if (message.error) {
          pending.reject(new Error(message.error.message));
          return;
        }
        pending.resolve(message.result ?? {});
        return;
      }

      const listeners = this.listeners.get(message.method) ?? [];
      listeners.forEach((listener) => listener(message.params ?? {}));
    });
  }

  send(method, params = {}) {
    const id = this.nextId;
    this.nextId += 1;
    const payload = JSON.stringify({ id, method, params });
    this.socket.send(payload);

    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject });
    });
  }

  waitFor(method, timeoutMs) {
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        this.removeListener(method, listener);
        reject(new Error(`Timed out waiting for ${method}`));
      }, timeoutMs);

      const listener = (params) => {
        clearTimeout(timeout);
        this.removeListener(method, listener);
        resolve(params);
      };

      const listeners = this.listeners.get(method) ?? [];
      listeners.push(listener);
      this.listeners.set(method, listeners);
    });
  }

  removeListener(method, listener) {
    const listeners = this.listeners.get(method) ?? [];
    this.listeners.set(
      method,
      listeners.filter((item) => item !== listener),
    );
  }

  close() {
    this.socket.close();
  }
}

await main();
