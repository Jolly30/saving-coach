const SYSTEM_PROMPT = `You are Saving Coach, a friendly personal finance AI assistant.
You help users track expenses, set budgets, and achieve savings goals.
When a user describes a purchase, extract the merchant, amount, category, and date.
Respond in the same language the user writes in.`;

module.exports = async function handler(req, res) {
  // CORS headers
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") {
    return res.status(200).end();
  }

  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method not allowed" });
  }

  const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
  if (!GEMINI_API_KEY) {
    return res.status(500).json({ error: "GEMINI_API_KEY not configured" });
  }

  const { messages = [], systemPrompt } = req.body;

  if (!messages.length) {
    return res.status(400).json({ error: "messages array is required" });
  }

  // Build Gemini-compatible contents array
  const contents = messages.map((msg) => ({
    role: msg.role === "ai" ? "model" : "user",
    parts: [{ text: msg.content }],
  }));

  const prompt = systemPrompt || SYSTEM_PROMPT;

  try {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${GEMINI_API_KEY}`;

    const geminiRes = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        systemInstruction: { parts: [{ text: prompt }] },
        contents,
        generationConfig: {
          temperature: 0.7,
          maxOutputTokens: 2048,
        },
      }),
    });

    const data = await geminiRes.json();

    if (!geminiRes.ok) {
      console.error("Gemini API error:", data);
      return res.status(geminiRes.status).json({
        error: data.error?.message || "Gemini API request failed",
      });
    }

    const reply =
      data.candidates?.[0]?.content?.parts?.[0]?.text || "No response generated.";

    return res.status(200).json({ reply });
  } catch (err) {
    console.error("Proxy error:", err.message);
    return res.status(500).json({ error: "Proxy server error" });
  }
};
