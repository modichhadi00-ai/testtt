/**
 * Sets the DeepSeek API key in Firebase Functions config.
 * Run from project root with the key in env (never commit the key).
 *
 * PowerShell:  $env:DEEPSEEK_API_KEY="sk-your-key"; node scripts/set-deepseek-key.js
 * CMD:         set DEEPSEEK_API_KEY=sk-your-key && node scripts/set-deepseek-key.js
 */
const { execSync } = require("child_process");
const path = require("path");

const key = process.env.DEEPSEEK_API_KEY;
if (!key || !key.startsWith("sk-")) {
  console.error("Set DEEPSEEK_API_KEY to your DeepSeek API key (starts with sk-).");
  console.error("Example (PowerShell): $env:DEEPSEEK_API_KEY=\"sk-xxx\"; node scripts/set-deepseek-key.js");
  process.exit(1);
}

const projectRoot = path.resolve(__dirname, "..");
try {
  execSync(
    ["firebase", "functions:config:set", `deepseek.api_key=${key}`],
    { cwd: projectRoot, stdio: "inherit" }
  );
  console.log("Done. Deploy with: firebase deploy --only functions");
} catch (e) {
  process.exit(e.status || 1);
}
