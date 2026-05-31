import { spawnSync } from "node:child_process";
import { promises as fs } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const checkedExtensions = new Set([".js", ".mjs"]);
const sourceDirs = ["scripts", "src"];
const files = [];

for (const sourceDir of sourceDirs) {
  await collectFiles(path.join(rootDir, sourceDir));
}

for (const file of files) {
  const result = spawnSync(process.execPath, ["--check", file], {
    stdio: "inherit",
  });
  if (result.status !== 0) {
    process.exit(result.status ?? 1);
  }
}

console.log(`Checked ${files.length} JavaScript files.`);

async function collectFiles(dir) {
  const entries = await fs.readdir(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      await collectFiles(fullPath);
      continue;
    }
    if (checkedExtensions.has(path.extname(entry.name))) {
      files.push(fullPath);
    }
  }
}
