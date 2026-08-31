const SYSTEM_PROMPT = `You are Saving Coach, a friendly personal finance AI assistant.
You help users track expenses, set budgets, and achieve savings goals.
When a user describes a purchase, extract the merchant, amount, category, and date.
Respond in the same language the user writes in.`;

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
        `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=${apiKey}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            systemInstruction: { parts: [{ text: systemPrompt }] },
            contents,
            generationConfig: { temperature: 0.7, maxOutputTokens: 800 },
            safetySettings: [
              { category: "HARM_CATEGORY_HARASSMENT", threshold: "BLOCK_NONE" },
              { category: "HARM_CATEGORY_HATE_SPEECH", threshold: "BLOCK_NONE" },
              { category: "HARM_CATEGORY_SEXUALLY_EXPLICIT", threshold: "BLOCK_NONE" },
              { category: "HARM_CATEGORY_DANGEROUS_CONTENT", threshold: "BLOCK_NONE" }
            ],
          }),
        }
      );

      const data = await res.json();
      if (!res.ok) throw { status: res.status, message: data.error?.message || "Gemini error" };
      return { reply: data.candidates?.[0]?.content?.parts?.[0]?.text || "No response." };
    },
  },
  {
    name: "OpenRouter",
    envKey: "OPENROUTER_API_KEY",
    call: async (messages, systemPrompt, apiKey) => {
      const models = [
        "google/gemma-4-31b-it:free",
        "nvidia/nemotron-3.5-lightning:free",
        "minimax/minimax-m3:free"
      ];
      
      let lastError = null;
      for (const model of models) {
        try {
          console.log(`Trying OpenRouter model: ${model}...`);
          const body = {
            model: model,
            messages: [
              { role: "system", content: systemPrompt },
              ...messages.map((m) => ({
                role: m.role === "ai" ? "assistant" : "user",
                content: m.content,
              })),
            ],
            temperature: 0.7,
            max_tokens: 400,
          };

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
          if (!res.ok) {
            throw new Error(data.error?.message || `HTTP ${res.status}`);
          }
          console.log(`✓ OpenRouter model ${model} succeeded`);
          return { reply: data.choices?.[0]?.message?.content || "No response." };
        } catch (err) {
          console.warn(`✗ OpenRouter model ${model} failed: ${err.message}`);
          lastError = err;
        }
      }
      throw lastError || new Error("All OpenRouter models failed");
    },
  },
];

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
      return res.status(200).json(result);
    } catch (err) {
      const status = err.status || 500;
      const msg = err.message || "Unknown error";
      console.error(`✗ ${provider.name} failed (${status}): ${msg}`);
      errors.push(`${provider.name}: ${msg}`);
    }
  }

  return res.status(503).json({
    error: "All providers failed",
    details: errors,
  });
};
