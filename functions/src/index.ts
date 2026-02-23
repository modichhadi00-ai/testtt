import * as dotenv from "dotenv";
dotenv.config();

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";

interface ChatMessage {
  role: "system" | "user" | "assistant";
  content: string;
}

interface ChatRequest {
  messages: ChatMessage[];
  stream?: boolean;
  model?: string;
  max_tokens?: number;
}

/**
 * Verifies Firebase ID token from Authorization header.
 * Returns decoded token or throws.
 */
async function verifyAuth(context: functions.https.CallableContext): Promise<admin.auth.DecodedIdToken> {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Missing or invalid authentication."
    );
  }
  return context.auth.token as unknown as admin.auth.DecodedIdToken;
}

/**
 * Get DeepSeek API key from Firebase config or environment.
 */
function getDeepSeekApiKey(): string {
  const key = process.env.DEEPSEEK_API_KEY ?? functions.config().deepseek?.api_key;
  if (!key) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "DeepSeek API key not configured. Set DEEPSEEK_API_KEY or firebase functions:config:set deepseek.api_key=YOUR_KEY"
    );
  }
  return key;
}

/**
 * Callable Cloud Function: chat (non-streaming).
 * Request: { messages: [...], stream?: false, model?: "deepseek-chat", max_tokens?: number }
 * Response: { content: string, usage?: {...} }
 */
export const chat = functions.https.onCall(async (data: ChatRequest, context): Promise<unknown> => {
  await verifyAuth(context);

  const { messages, stream = false, model = "deepseek-chat", max_tokens = 4096 } = data ?? {};
  if (!Array.isArray(messages) || messages.length === 0) {
    throw new functions.https.HttpsError("invalid-argument", "messages array is required");
  }

  const apiKey = getDeepSeekApiKey();
  const body = {
    model,
    messages,
    stream: false,
    max_tokens,
  };

  const res = await fetch(DEEPSEEK_API_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${apiKey}`,
    },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    const errText = await res.text();
    throw new functions.https.HttpsError(
      "internal",
      `DeepSeek API error: ${res.status} - ${errText}`
    );
  }

  const json = (await res.json()) as {
    choices?: Array<{ message?: { content?: string }; finish_reason?: string }>;
    usage?: { prompt_tokens?: number; completion_tokens?: number; total_tokens?: number };
  };
  const content = json.choices?.[0]?.message?.content ?? "";
  return { content, usage: json.usage };
});

/**
 * HTTPS function for streaming chat (SSE).
 * Client sends POST with Authorization: Bearer <Firebase ID token>
 * Body: { messages: [...], stream: true, model?: string, max_tokens?: number }
 * Response: Server-Sent Events stream.
 */
export const chatStream = functions.https.onRequest(async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).send("Method Not Allowed");
    return;
  }

  const authHeader = req.headers.authorization;
  if (!authHeader?.startsWith("Bearer ")) {
    res.status(401).json({ error: "Missing or invalid Authorization header" });
    return;
  }
  const idToken = authHeader.slice(7);

  let decodedToken: admin.auth.DecodedIdToken;
  try {
    decodedToken = await admin.auth().verifyIdToken(idToken);
  } catch {
    res.status(401).json({ error: "Invalid token" });
    return;
  }

  let body: ChatRequest;
  try {
    body = typeof req.body === "string" ? JSON.parse(req.body) : req.body;
  } catch {
    res.status(400).json({ error: "Invalid JSON body" });
    return;
  }

  const { messages, model = "deepseek-chat", max_tokens = 4096 } = body ?? {};
  if (!Array.isArray(messages) || messages.length === 0) {
    res.status(400).json({ error: "messages array is required" });
    return;
  }

  const apiKey = getDeepSeekApiKey();
  const payload = {
    model,
    messages,
    stream: true,
    max_tokens,
  };

  const deepSeekRes = await fetch(DEEPSEEK_API_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${apiKey}`,
    },
    body: JSON.stringify(payload),
  });

  if (!deepSeekRes.ok) {
    const errText = await deepSeekRes.text();
    res.status(deepSeekRes.status).json({ error: `DeepSeek API: ${errText}` });
    return;
  }

  res.setHeader("Content-Type", "text/event-stream");
  res.setHeader("Cache-Control", "no-cache");
  res.setHeader("Connection", "keep-alive");
  res.flushHeaders();

  const reader = deepSeekRes.body?.getReader();
  if (!reader) {
    res.end();
    return;
  }

  const decoder = new TextDecoder();
  let buffer = "";

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split("\n");
      buffer = lines.pop() ?? "";
      for (const line of lines) {
        if (line.startsWith("data: ")) {
          const data = line.slice(6);
          if (data === "[DONE]") {
            res.write("data: [DONE]\n\n");
            res.end();
            return;
          }
          res.write(line + "\n");
        }
      }
      res.flush?.();
    }
    if (buffer.trim()) {
      if (buffer.startsWith("data: ")) res.write(buffer + "\n");
    }
  } finally {
    reader.releaseLock();
  }
  res.end();
});
