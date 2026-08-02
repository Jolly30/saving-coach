const SYSTEM_PROMPT = `You are Saving Coach, a friendly personal finance AI assistant.
You help users track expenses, set budgets, and achieve savings goals.
When a user describes a purchase, extract the merchant, amount, category, and date.
Respond in the same language the user writes in.`;

// ─── Provider definitions ───────────────────────────────────────────
// Each provider: { name, envKey, call(messages, systemPrompt, apiKey) }
// call() returns { reply } or throws with { status, message }

const providers = [
  {
    name: "Gemini",
    envKey: "GEMINI_API_KEY",
    call: async (messages, systemPrompt, apiKey) => {
      const contents = messages.map((m) => ({
        role: m.role === "ai" ? "model" : "user",
        parts: [{ text: m.content }],
      }));

      const res = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${apiKey}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            systemInstruction: { parts: [{ text: systemPrompt }] },
            contents,
            generationConfig: { temperature: 0.7, maxOutputTokens: 2048 },
          }),
        }
      );

      const data = await res.json();
      if (!res.ok) {
        throw { status: res.status, message: data.error?.message || "Gemini error" };
      }
      return { reply: data.candidates?.[0]?.content?.parts?.[0]?.text || "No response." };
    },
  },
  {
    name: "OpenRouter",
    envKey: "OPENROUTER_API_KEY",
    call: async (messages, systemPrompt, apiKey) => {
      const body = buildOpenAICompat(messages, systemPrompt, "deepseek/deepseek-chat-v3-0324");
      const res = await fetch("https://openrouter.ai/api/v1/chat/completions", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${apiKey}`,
          "HTTP-Referer": "https://saving-coach.app",
        },
        body: JSON.stringify(body),
      });
      const data = await res.json();
      if (!res.ok) throw { status: res.status, message: data.error?.message || "OpenRouter error" };
      return { reply: data.choices?.[0]?.message?.content || "No response." };
    },
  },
];

// ─── Shared helper for OpenAI-compatible providers ──────────────────
function buildOpenAICompat(messages, systemPrompt, model) {
  return {
    model,
    messages: [
      { role: "system", content: systemPrompt },
      ...messages.map((m) => ({
        role: m.role === "ai" ? "assistant" : "user",
        content: m.content,
      })),
    ],
    temperature: 0.7,
    max_tokens: 2048,
  };
}

// ─── Should we skip this provider? ──────────────────────────────────
function shouldSkip(status) {
  // 401/403 = bad key, 429 = rate limited, 500+ = server down — all retry-worthy
  // 400 = bad request (our fault), 404 = wrong endpoint — don't retry
  return status >= 400 && status < 500 && status !== 429 && status !== 401 && status !== 403;
}

// ─── Handler ────────────────────────────────────────────────────────
module.exports = async function handler(req, res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") return res.status(200).end();
  if (req.method !== "POST") return res.status(405).json({ error: "Method not allowed" });

  const { messages = [], systemPrompt } = req.body;
  if (!messages.length) return res.status(400).json({ error: "messages array is required" });

  const prompt = systemPrompt || SYSTEM_PROMPT;
  const errors = [];

  for (const provider of providers) {
    const apiKey = process.env[provider.envKey];
    if (!apiKey) continue;

    try {
      console.log(`Trying ${provider.name}...`);
      const result = await provider.call(messages, prompt, apiKey);
      console.log(`✓ ${provider.name} succeeded`);
      return res.status(200).json({ ...result, provider: provider.name });
    } catch (err) {
      const status = err.status || 500;
      const msg = err.message || "Unknown error";
      console.error(`✗ ${provider.name} failed (${status}): ${msg}`);
      errors.push(`${provider.name}: ${msg}`);

      if (shouldSkip(status)) {
        break; // Abort fallback for non-retryable errors like 400 Bad Request
      }
    }
  }

  if (errors.length === 0) {
    return res.status(500).json({ error: "No API keys configured. Set at least one: GEMINI_API_KEY, OPENROUTER_API_KEY" });
  }

  return res.status(503).json({
    error: "All providers failed",
    details: errors,
  });
};
