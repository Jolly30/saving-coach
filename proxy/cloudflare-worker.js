/**
 * Cloudflare Worker for Saving Coach API Proxy
 * Handles /api/chat, /api/coingecko, /api/finnhub
 * Bypasses Myanmar ISP blocks and Google Gemini geo-restrictions.
 */

// OPTIONAL: If Cloudflare dashboard variables are not binding, you can paste your Gemini API key here:
const HARDCODED_GEMINI_KEY = "";

const SYSTEM_PROMPT = `You are Saving Coach, a friendly personal finance AI assistant.
You help users track expenses, set budgets, and achieve savings goals.
When a user describes a purchase, extract the merchant, amount, category, and date.
Respond in the same language the user writes in.`;

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
};

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
    }

    const url = new URL(request.url);
    const pathname = url.pathname;

    try {
      if (pathname === "/api/chat") {
        return await handleChat(request, env);
      } else if (pathname === "/api/coingecko") {
        return await handleCoinGecko(request, env);
      } else if (pathname === "/api/finnhub") {
        return await handleFinnhub(request, env);
      } else if (pathname === "/" || pathname === "/health") {
        return new Response(JSON.stringify({ status: "ok", service: "Saving Coach Proxy" }), {
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      } else {
        return new Response(JSON.stringify({ error: "Not found" }), {
          status: 404,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
    } catch (err) {
      return new Response(JSON.stringify({ error: err.message || "Internal server error" }), {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }
  },
};

// ─────────────────────────────────────────────
// 1. Chat Handler (Gemini with OpenRouter fallback)
// ─────────────────────────────────────────────
async function handleChat(request, env) {
  if (request.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), {
      status: 405,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  const { messages = [], systemPrompt, userGeminiKey, userOpenRouterKey } = await request.json();
  if (!messages.length) {
    return new Response(JSON.stringify({ error: "messages array is required" }), {
      status: 400,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  const prompt = systemPrompt || SYSTEM_PROMPT;
  const lastUserMsg = messages.filter((m) => m.role === "user").slice(-1)[0]?.content || "";
  const isBurmese = /[က-႟]/.test(lastUserMsg);

  const errors = [];
  const url = new URL(request.url);
  const isDebug = url.searchParams.has("debug");
  const systemGeminiKey = (HARDCODED_GEMINI_KEY || env.GEMINI_API_KEY || env.GEMINI_KEY || "").trim();
  const systemOpenRouterKey = (env.OPENROUTER_API_KEY || "").trim();

  const executionSteps = [];

  // 1. User Gemini Key (if provided)
  if (userGeminiKey && userGeminiKey.trim()) {
    executionSteps.push({
      label: "User Gemini Key",
      fn: () => callGemini(messages, prompt, isBurmese, userGeminiKey.trim())
    });
  }

  // 2. User OpenRouter Key (if provided)
  if (userOpenRouterKey && userOpenRouterKey.trim()) {
    executionSteps.push({
      label: "User OpenRouter Key",
      fn: () => callOpenRouter(messages, prompt, userOpenRouterKey.trim())
    });
  }

  // 3. Fallback: System Gemini Key
  if (systemGeminiKey) {
    executionSteps.push({
      label: "System Gemini Key",
      fn: () => callGemini(messages, prompt, isBurmese, systemGeminiKey)
    });
  }

  // 4. Fallback: System OpenRouter Key
  if (systemOpenRouterKey) {
    executionSteps.push({
      label: "System OpenRouter Key",
      fn: () => callOpenRouter(messages, prompt, systemOpenRouterKey)
    });
  }

  for (const step of executionSteps) {
    try {
      const { reply, model } = await step.fn();
      return new Response(JSON.stringify({
        reply,
        provider: `${step.label} [${model}]`,
        ...(isDebug ? { errors, configuredKeys: Object.keys(env) } : {})
      }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    } catch (err) {
      errors.push(`${step.label}: ${err.message}`);
    }
  }

  return new Response(JSON.stringify({ error: "All AI providers failed", details: errors }), {
    status: 503,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

async function callGemini(messages, prompt, isBurmese, apiKey) {
  const geminiModels = ["gemini-2.0-flash", "gemini-1.5-flash"];
  let lastErr = null;

  for (const geminiModel of geminiModels) {
    try {
      const contents = messages.map((m) => ({
        role: m.role === "ai" || m.role === "assistant" || m.role === "model" ? "model" : "user",
        parts: [{ text: m.content }],
      }));

      const res = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${geminiModel}:generateContent?key=${apiKey}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            systemInstruction: { parts: [{ text: prompt }] },
            contents,
            generationConfig: {
              temperature: 0.7,
              maxOutputTokens: isBurmese ? 1000 : 800,
            },
            safetySettings: [
              { category: "HARM_CATEGORY_HARASSMENT", threshold: "BLOCK_NONE" },
              { category: "HARM_CATEGORY_HATE_SPEECH", threshold: "BLOCK_NONE" },
              { category: "HARM_CATEGORY_SEXUALLY_EXPLICIT", threshold: "BLOCK_NONE" },
              { category: "HARM_CATEGORY_DANGEROUS_CONTENT", threshold: "BLOCK_NONE" },
            ],
          }),
        }
      );

      const data = await res.json();
      if (!res.ok) throw new Error(data.error?.message || `Gemini error ${res.status}`);

      const candidateParts = data.candidates?.[0]?.content?.parts || [];
      const userParts = candidateParts.filter((p) => !p.thought && p.text);
      let reply = userParts.length > 0
        ? userParts.map((p) => p.text).join("")
        : (candidateParts.slice(-1)[0]?.text || "No response.");

      reply = cleanThinking(reply);
      return { reply, model: geminiModel };
    } catch (err) {
      lastErr = err;
    }
  }

  throw lastErr || new Error("Gemini models failed");
}

async function callOpenRouter(messages, prompt, apiKey) {
  const models = [
    "google/gemini-2.0-flash-exp:free",
    "meta-llama/llama-3.3-70b-instruct:free",
    "mistralai/mistral-small-24b-instruct-2501:free",
    "qwen/qwen-2.5-72b-instruct:free",
    "nvidia/nemotron-3.5-lightning:free",
  ];

  let lastErr = null;
  for (const model of models) {
    try {
      const body = {
        model: model,
        messages: [
          { role: "system", content: prompt },
          ...messages.map((m) => ({
            role: m.role === "ai" ? "assistant" : "user",
            content: m.content,
          })),
        ],
        temperature: 0.7,
        max_tokens: 1500,
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
      if (!res.ok) throw new Error(data.error?.message || `OpenRouter HTTP ${res.status}`);

      let reply = data.choices?.[0]?.message?.content || "No response.";
      reply = cleanThinking(reply);
      return { reply, model };
    } catch (err) {
      lastErr = err;
    }
  }

  throw lastErr || new Error("OpenRouter models failed");
}

function cleanThinking(text) {
  if (!text) return "";
  // Strip <think>...</think>, [think]...[/think], ```thought...```
  text = text.replace(/<think>[\s\S]*?<\/think>/gi, "")
             .replace(/\[think\][\s\S]*?\[\/think\]/gi, "")
             .replace(/```thought[\s\S]*?```/gi, "")
             .trim();

  // Check for explicit response headers (e.g. Draft - Mental Refinement, Response, Final response, Possible response)
  const match = text.match(/(?:\d+\.\s*)?\*{0,2}(?:Possible response|Draft\s*[-–]\s*Mental Refinement|Mental Refinement|Draft response|Conversational response|Final response|Response|Answer)\*{0,2}:\*{0,2}\s*(?:\*\([^\)]*\)\*\s*)?["“]?([\s\S]+?)["”]?$/i);
  if (match && match[1].trim()) {
    const extracted = match[1].trim().replace(/^["“]|["”]$/g, "").trim();
    if (extracted.length > 10) return extracted;
  }

  // Check for draft quotes like: Something like "..."
  const quoteMatch = text.match(/^(?:Something like|My response should be|Response would be|I should say|Start with|Something along the lines of)\s*["“]([\s\S]+?)["”]/i);
  if (quoteMatch && quoteMatch[1].trim()) {
    const candidate = quoteMatch[1].trim();
    if (!candidate.includes("EXPENSE_DATA") && !/hidden context|rules say/i.test(candidate)) {
      return candidate;
    }
  }

  // Aggressive thinking detection patterns
  const thinkingPatterns = [
    // Input analysis and processing steps
    /(?:Analyze|Analyzing) (?:User|the) Input/i,
    /(?:User|The user) (?:says|said|wants|asked|is asking|mentioned|wrote|typed|logging|just said)/i,
    /(?:This is an?|Another) (?:instruction|request|expense|challenge)/i,
    /The format expected is/i,
    /Identify Required Fields/i,
    /Determine Response Language/i,
    /Formulate Extraction/i,
    /Possible response:/i,
    /The amount is \d+/i,
    /category would be/i,
    /merchant is (?:not )?specified/i,
    /date is today's date from context/i,
    /date from context:?/i,
    /from context: \d{4}-\d{2}-\d{2}/i,
    /So this is .+ for \d+ MMK total/i,
    /\(since it's .+\)/i,

    // Date deduction reasoning
    /If there are \d+ days left/i,
    /because \d+-\d+=\d+/i,
    /days have passed,? so today is/i,
    /today is (?:January|February|March|April|May|June|July|August|September|October|November|December) \d+/i,

    // User intent statements
    /The user is logging an expense/i,
    /This is (?:an?|another) (?:expense|challenge|income|transaction|saving) (?:logging )?request/i,

    // Processing/intent statements
    /(?:Actually,? wait|Actually,? I |Actually,? looking|Actually,? the|Wait,? but|Let me |I need to |I should |I'll |I will )/i,
    /(?:Let me format|Let me parse|Let me analyze|Let me check|Let me think|Let me work|Let me go)/i,
    /(?:I need to (?:output|extract|follow|determine|process|handle|write|check|format))/i,

    // Rules/challenge detection
    /(?:Wait,? but the rules|The rules (?:also )?say|According to (?:the )?rules)/i,
    /The rules say:?/i,
    /Wait,? let me re-read/i,
    /Wait,? let me (?:check|think|look|analyze)/i,
    /Then (?:at the end|the data block)/i,
    /Also,? the strict prohibition:?/i,
    /Write your natural conversational response first/i,
    /Looking at the hidden context:?/i,
    /So if I add \d+/i,
    /So my response should be/i,
    /But I need to be careful not to overstep/i,
    /Do NOT automatically save the expense/i,
    /NEVER mix (?:Burmese|English) words/i,
    /Present Situations \(general knowledge\)/i,
    /Today's approximate ranges/i,
    /Exchange Rate \(USD → MMK\)/i,
    /Something like\s*["“]/i,
    /(?:Challenge action values|CHALLENGE DETECTION|EXPENSE DETECTION)/i,
    /(?:If amount is not specified|If the user)/i,
    /(?:Challenge Title:|challengeTitle:)/i,
    /(?:Non-existent|non existent|does not exist)/i,
    /(?:match from active challenges|match the challenge)/i,

    // Looking at context
    /(?:Looking at the|According to (?:the )?(?:hidden|context|rules|EXPENSE|CHALLENGE))/i,
    /(?:Based on the (?:hidden|context|rules))/i,
    /(?:Following the (?:EXPENSE|CHALLENGE) rules)/i,

    // Thinking starters
    /(?:Here'?s a thinking process|Here'?s (?:what|how))/i,
    /(?:So (?:date|amount|category))/i,
    /(?:Let'?s (?:parse|analyze|think|check))/i,

    // Structure/output thinking
    /(?:The structure should be|The structure is)/i,
    /(?:For this request|For this user)/i,
    /(?:amount:.*category:|category:.*merchant:)/i,
    /(?:Acknowled(?:ge|ing) the)/i,
    /(?:Mention the|Keep (?:it|the|a|short))/i,
    /(?:You (?:should|would|need to) (?:acknowledge|mention|include|output))/i,
    /(?:The user (?:said|wants|is asking|mentioned|wrote))/i,
    /(?:I should (?:acknowledge|mention|include|output|write))/i,
    /(?:Step \d|Phase \d|First,|Second,|Third,)/i,
    /(?:JSON structure|JSON block|JSON data)/i,
    /(?:•\s*(?:amount|category|merchant|date|currency|acknowledge|mention|keep):?)/i,
    /(?:\d+\.\s*(?:amount|category|merchant|date))/i,

    // Analysis thinking
    /(?:Actually,? looking (?:more |at the |closely))/i,
    /(?:Actually,? I think)/i,
    /(?:Actually,? this (?:could|might|seems))/i,
    /(?:Active Challenges \(\d+\)):/i,
    /(?:•\s*\w+:.*MMK.*complete)/i
  ];

  const isThinking = thinkingPatterns.some(p => p.test(text));

  if (isThinking) {
    const expenseDataMatch = text.match(/\[EXPENSE_DATA\][\s\S]*?\[\/EXPENSE_DATA\]/);
    const expenseData = expenseDataMatch ? "\n\n" + expenseDataMatch[0] : "";
    const withoutExpense = text.replace(/\[EXPENSE_DATA\][\s\S]*?\[\/EXPENSE_DATA\]/, "").trim();

    const paragraphs = withoutExpense.split(/\n\s*\n/).map(p => p.trim()).filter(Boolean);
    const userFacing = [];

    for (const p of paragraphs) {
      const lines = p.split("\n").map(l => l.trim()).filter(Boolean);
      const isAllBullets = lines.length > 0 && lines.every(l => /^[•*-]/.test(l));

      const isParagraphThinking = isAllBullets ||
                         thinkingPatterns.some(pt => pt.test(p)) ||
                         /^[•*-]/.test(p) ||
                         /^(?:amount|category|merchant|date|currency):/i.test(p) ||
                         /EXPENSE DETECTION|CHALLENGE DETECTION/i.test(p) ||
                         /The rules say/i.test(p) ||
                         /Wait, let me/i.test(p) ||
                         /re-read the/i.test(p) ||
                         /Then at the end/i.test(p) ||
                         /Then the data block/i.test(p) ||
                         /strict prohibition/i.test(p) ||
                         /Write your natural conversational/i.test(p) ||
                         /Looking at the hidden context/i.test(p) ||
                         /So if I add/i.test(p) ||
                         /So my response should be/i.test(p) ||
                         /careful not to overstep/i.test(p) ||
                         /Do NOT automatically save/i.test(p) ||
                         /NEVER mix/i.test(p) ||
                         /^(?:Something like|Wait,|Also,)/i.test(p) ||
                         /Analyze User Input|Identify Required Fields|Determine Response Language|Formulate Extraction|Possible response/i.test(p) ||
                         /The format expected is|This is an instruction/i.test(p) ||
                         /^(?:1|2|3|4|5)\.\s*\*\*/i.test(p) ||
                         /days left in|days have passed/i.test(p) ||
                         /^let'?s/i.test(p) ||
                         /^so date/i.test(p) ||
                         /^I need to/i.test(p) ||
                         /^First/i.test(p) ||
                         /^Second/i.test(p) ||
                         /^Third/i.test(p) ||
                         /hidden context/i.test(p) ||
                         /JSON block/i.test(p) ||
                         /json structure/i.test(p) ||
                         /prompt_challenge_confirmation/i.test(p) ||
                         /mark_challenge_saving/i.test(p) ||
                         /non-existent/i.test(p) ||
                         /Challenge Title:/i.test(p) ||
                         /challengeTitle:/i.test(p) ||
                         /match from active/i.test(p) ||
                         /does not exist/i.test(p) ||
                         /The structure should be/i.test(p) ||
                         /For this request/i.test(p) ||
                         /Acknowled(?:ge|ing) the/i.test(p) ||
                         /Mention the/i.test(p) ||
                         /Step \d|Phase \d/i.test(p) ||
                         /JSON structure|JSON data/i.test(p) ||
                         /Active Challenges \(\d+\)/i.test(p) ||
                         /•\s*\w+:.*MMK.*complete/i.test(p) ||
                         /Actually,? looking (?:more |at the |closely)/i.test(p) ||
                         /Actually,? I think/i.test(p) ||
                         /Actually,? this (?:could|might|seems)/i.test(p);
      if (!isParagraphThinking) {
        userFacing.push(p);
      }
    }

    // Filter out very short fragments that are likely thinking remnants
    const filteredFacing = userFacing.filter(p => p.length > 10 || /\d/.test(p));

    if (filteredFacing.length > 0) {
      return filteredFacing.join("\n\n") + expenseData;
    } else {
      if (expenseData) {
        return "I've noted this. Please confirm below." + expenseData;
      }
      return "";
    }
  }

  return text;
}

// ─────────────────────────────────────────────
// 2. CoinGecko Handler
// ─────────────────────────────────────────────
async function handleCoinGecko(request, env) {
  if (request.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), {
      status: 405,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  const apiKey = env.COINGECKO_API_KEY;
  if (!apiKey) {
    return new Response(JSON.stringify({ error: "COINGECKO_API_KEY not configured" }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  const body = await request.json();
  const { action, query, ids, vs_currencies = "usd", include_24hr_change = "true" } = body;
  const COINGECKO_BASE = "https://api.coingecko.com/api/v3";

  let targetUrl;
  if (action === "search") {
    if (!query) return new Response(JSON.stringify({ error: "query required" }), { status: 400, headers: corsHeaders });
    targetUrl = `${COINGECKO_BASE}/search?query=${encodeURIComponent(query)}`;
  } else if (action === "price") {
    if (!ids) return new Response(JSON.stringify({ error: "ids required" }), { status: 400, headers: corsHeaders });
    targetUrl = `${COINGECKO_BASE}/simple/price?ids=${encodeURIComponent(ids)}&vs_currencies=${vs_currencies}&include_24hr_change=${include_24hr_change}`;
  } else {
    return new Response(JSON.stringify({ error: "Invalid action" }), { status: 400, headers: corsHeaders });
  }

  const response = await fetch(targetUrl, {
    headers: { "x-cg-demo-api-key": apiKey, Accept: "application/json" },
  });

  const data = await response.text();
  return new Response(data, {
    status: response.status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

// ─────────────────────────────────────────────
// 3. Finnhub Handler
// ─────────────────────────────────────────────
async function handleFinnhub(request, env) {
  if (request.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), {
      status: 405,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  const apiKey = env.FINNHUB_API_KEY;
  if (!apiKey) {
    return new Response(JSON.stringify({ error: "FINNHUB_API_KEY not configured" }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  const body = await request.json();
  const { action, q, symbol, category = "general" } = body;
  const FINNHUB_BASE = "https://finnhub.io/api/v1";

  let targetUrl;
  if (action === "search") {
    if (!q) return new Response(JSON.stringify({ error: "q required" }), { status: 400, headers: corsHeaders });
    targetUrl = `${FINNHUB_BASE}/search?q=${encodeURIComponent(q)}&token=${apiKey}`;
  } else if (action === "quote") {
    if (!symbol) return new Response(JSON.stringify({ error: "symbol required" }), { status: 400, headers: corsHeaders });
    targetUrl = `${FINNHUB_BASE}/quote?symbol=${encodeURIComponent(symbol)}&token=${apiKey}`;
  } else if (action === "news") {
    targetUrl = `${FINNHUB_BASE}/news?category=${category}&token=${apiKey}`;
  } else {
    return new Response(JSON.stringify({ error: "Invalid action" }), { status: 400, headers: corsHeaders });
  }

  const response = await fetch(targetUrl);
  const data = await response.text();
  return new Response(data, {
    status: response.status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}
