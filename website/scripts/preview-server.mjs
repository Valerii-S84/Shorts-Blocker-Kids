import http from "node:http";
import { createReadStream, promises as fs } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const distDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../dist");
const types = {
  ".html": "text/html; charset=utf-8", ".css": "text/css; charset=utf-8",
  ".js": "application/javascript; charset=utf-8", ".png": "image/png",
  ".jpg": "image/jpeg", ".mp4": "video/mp4",
  ".webmanifest": "application/manifest+json; charset=utf-8",
  ".xml": "application/xml; charset=utf-8", ".txt": "text/plain; charset=utf-8",
};

export async function startPreviewServer(port = 0) {
  const server = http.createServer(async (request, response) => {
    if (!["GET", "HEAD"].includes(request.method)) {
      response.writeHead(405, { Allow: "GET, HEAD" }).end();
      return;
    }
    try {
      const pathname = decodeURIComponent(new URL(request.url, "http://localhost").pathname);
      let file = path.resolve(distDir, `.${pathname}`);
      if (file !== distDir && !file.startsWith(`${distDir}${path.sep}`)) throw new Error("Outside preview");
      if ((await fs.stat(file)).isDirectory()) file = path.join(file, "index.html");
      file = await fs.realpath(file);
      if (!file.startsWith(`${distDir}${path.sep}`)) throw new Error("Outside preview");
      const { size } = await fs.stat(file);
      const headers = { "Content-Type": types[path.extname(file)] ?? "application/octet-stream",
        "Accept-Ranges": "bytes", "Cache-Control": "no-cache" };
      let start = 0;
      let end = size - 1;
      const range = request.headers.range;
      if (range) {
        const match = /^bytes=(\d*)-(\d*)$/.exec(range);
        if (match && (match[1] || match[2])) {
          start = match[1] ? Number(match[1]) : Math.max(0, size - Number(match[2]));
          end = match[1] && match[2] ? Math.min(size - 1, Number(match[2])) : size - 1;
        }
        if (!match || (!match[1] && !match[2]) || !Number.isSafeInteger(start) ||
            !Number.isSafeInteger(end) || start > end || start >= size) {
          response.writeHead(416, { "Content-Range": `bytes */${size}` }).end();
          return;
        }
        headers["Content-Range"] = `bytes ${start}-${end}/${size}`;
      }
      headers["Content-Length"] = Math.max(0, end - start + 1);
      response.writeHead(range ? 206 : 200, headers);
      if (request.method === "HEAD" || size === 0) response.end();
      else createReadStream(file, { start, end }).on("error", () => response.destroy()).pipe(response);
    } catch {
      response.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" }).end("Not found");
    }
  });
  await new Promise((resolve, reject) => {
    server.once("error", reject);
    server.listen(port, "127.0.0.1", resolve);
  });
  return { origin: `http://127.0.0.1:${server.address().port}`, close: () => server.close() };
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const port = Number(process.argv[2] ?? 4173);
  if (!Number.isInteger(port) || port < 0 || port > 65535) throw new Error("Invalid preview port");
  const server = await startPreviewServer(port);
  console.log(`Local preview: ${server.origin}/#demo`);
}
