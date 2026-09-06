const SYSTEM_PROMPT = `You are an empathetic, proactive personal financial coach named Saving Coach.
When summarizing financial data:
1. Do not merely list raw figures; interpret what they mean for the user's daily life.
2. Calculate and highlight a "Daily Safe-to-Spend" amount based on remaining days.
3. Call out the single biggest spending leak with zero judgment.
4. Provide exactly ONE practical, low-effort step the user can take this week.
5. Keep the tone encouraging, concise, and focused on behavioral change.
When a user describes a purchase, extract the merchant, amount, category, and date.
Respond in the same language the user writes in.`;

const providers = [
  {
    name: "Gemini",
    envKey: "GEMINI_API_KEY",
    call: async (messages, systemPrompt, apiKey) => {
      const lastUserMessage = messages.filter((m) => m.role === "user").slice(-1)[0]?.content || "";
      const isBurmese = /[က-႟]/.test(lastUserMessage);

      const contents = messages.map((m) => ({
        role: m.role === "ai" ? "model" : "user",
        parts: [{ text: m.content }],
      }));

      const geminiModels = [
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite",
        "gemini-3.6-flash",
        "gemini-2.5-pro",
        "gemini-2.0-flash",
        "gemini-1.5-flash",
      ];
      let lastErr = null;

      for (const geminiModel of geminiModels) {
        try {
          const res = await fetch(
            `https://generativelanguage.googleapis.com/v1beta/models/${geminiModel}:generateContent?key=${apiKey}`,
            {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({
                systemInstruction: { parts: [{ text: systemPrompt }] },
                contents,
                generationConfig: {
                  temperature: 0.7,
                  maxOutputTokens: 1500,
                },
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
          if (!res.ok) throw { status: res.status, message: data.error?.message || `Gemini error ${res.status}` };

          // Filter out thinking parts
          const candidateParts = data.candidates?.[0]?.content?.parts || [];
          const userParts = candidateParts.filter((p) => !p.thought && p.text);
          let reply = userParts.length > 0
            ? userParts.map((p) => p.text).join("")
            : (candidateParts.slice(-1)[0]?.text || "No response.");

          reply = cleanThinking(reply);
          return { reply };
        } catch (err) {
          lastErr = err;
        }
      }

      throw lastErr || { status: 500, message: "Gemini models failed" };
    },
  },
  {
    name: "OpenRouter",
    envKey: "OPENROUTER_API_KEY",
    call: async (messages, systemPrompt, apiKey) => {
      const lastUserMessage = messages.filter((m) => m.role === "user").slice(-1)[0]?.content || "";
      const isBurmese = /[က-႟]/.test(lastUserMessage);
      const maxTokens = 1500;

      const models = [
        "deepseek/deepseek-chat",
        "google/gemini-2.5-flash",
        "meta-llama/llama-3.3-70b-instruct",
        "qwen/qwen-2.5-72b-instruct",
        "mistralai/mistral-small-24b-instruct-2501",
        "google/gemini-2.0-flash-exp:free",
        "nvidia/nemotron-3.5-lightning:free",
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
            max_tokens: maxTokens,
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
          let reply = data.choices?.[0]?.message?.content || "No response.";
          reply = cleanThinking(reply);
          if (isBurmese) {
            const hasCorruptedScripts = /[\uAC00-\uD7AF\u1100-\u11FF\u3130-\u318F\u0E00-\u0E7F]/.test(reply);
            const hasHybridTokens = /[က-႟]+-[a-zA-Z]+|[a-zA-Z]+-[က-႟]+/.test(reply);
            if (hasCorruptedScripts || hasHybridTokens) {
              throw new Error(`Model ${model} produced corrupted multilingual Burmese`);
            }
          }
          return { reply };
        } catch (err) {
          console.warn(`✗ OpenRouter model ${model} failed: ${err.message}`);
          lastError = err;
        }
      }
      throw lastError || new Error("All OpenRouter models failed");
    },
  },
];

function cleanThinking(text) {
  if (!text) return "";
  // Strip <think>...</think>, [think]...[/think], ```thought...```
  text = text.replace(/<think>[\s\S]*?<\/think>/gi, "")
             .replace(/\[think\][\s\S]*?\[\/think\]/gi, "")
             .replace(/```thought[\s\S]*?```/gi, "")
             .trim();

  // Reject foreign corrupted scripts (Korean/Hangul) or hybrid BPE tokens
  if (/[\uAC00-\uD7AF\u1100-\u11FF\u3130-\u318F]/.test(text) || /[က-႟]+-[a-zA-Z]+|[a-zA-Z]+-[က-႟]+/.test(text)) {
    const expenseDataMatch = text.match(/\[EXPENSE_DATA\][\s\S]*?\[\/EXPENSE_DATA\]/);
    return expenseDataMatch ? "\n\n" + expenseDataMatch[0] : "";
  }

  // Check for explicit response headers (e.g. Draft - Mental Refinement, Response, Final response)
  const match = text.match(/(?:\d+\.\s*)?\*{0,2}(?:Draft\s*[-–]\s*Mental Refinement|Mental Refinement|Draft response|Conversational response|Final response|Response|Answer)\*{0,2}:\*{0,2}\s*(?:\*\([^\)]*\)\*\s*)?["“]?([\s\S]+?)["”]?$/i);
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
    // Date deduction reasoning
    /If there are \d+ days left/i,
    /because \d+-\d+=\d+/i,
    /days have passed,? so today is/i,
    /today is (?:January|February|March|April|May|June|July|August|September|October|November|December) \d+/i,

    // User intent statements
    /The user (?:is |said |wants |asked |is asking |mentioned |wrote |typed |logging |just said )/i,
    /The user is logging an expense/i,
    /This is (?:an?|another) (?:expense|challenge|income|transaction|saving) (?:logging )?request/i,

    // Processing/intent statements
    /(?:Actually,? wait|Actually,? looking closely|Actually,? let me|Wait,? but (?:the rules|I need))/i,
    /(?:Let me (?:format|parse|analyze|extract|check the rules|think about))/i,
    /(?:I need to (?:output|extract|follow the rules|determine the category|format the JSON))/i,

    // Rules/challenge detection
    /(?:Wait,? but the rules|The rules (?:also )?say|According to (?:the )?rules)/i,
    /The rules say:?/i,
    /Wait,? let me re-read/i,
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
    /(?:Challenge Title:|challengeTitle:)/i,

    // Looking at context
    /(?:Looking at the (?:hidden context|rules|prompt|instruction))/i,
    /(?:Based on the (?:hidden context|rules|prompt|instruction))/i,
    /(?:Following the (?:EXPENSE|CHALLENGE) rules)/i,

    // Structure/output thinking
    /(?:The structure should be|The structure is)/i,
    /(?:For this request|For this user)/i,
    /(?:JSON structure|JSON block|JSON data)/i,
    /(?:•\s*(?:amount|category|merchant|date|currency|acknowledge|mention|keep):?)/i,
    /(?:\d+\.\s*(?:amount|category|merchant|date))/i,

    // Prompt regurgitation & multi-challenge / mixed thinking
    /So for this case.*/i,
    /with (?:two|three|multiple|several|\d+) (?:challenges|expenses).*/i,
    /For the (?:expense|challenge) part.*/i,
    /And for (?:mixed|multiple).*/i,
    /"?Example for (?:Mixed|Multiple|Challenge|Expense).*/i,
    /^User:\s*"?/i,
    /^Assistant:\s*"?/i,
    /I need:\s*$/i,
    /would be .* category/i,

    // Analysis thinking
    /(?:Active Challenges \(\d+\)):/i,
    /(?:•\s*\w+:.*MMK.*complete)/i,

    // Leaked prompt directives
    /(?:What remaining funds and days left mean|Specific daily spending guardrail|Remaining Budget\s*\/|Address the single biggest spending category)/i
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
      const hasExtractionBullets = lines.some(l => /^[•*\\-\u2022\u2023\u25E6\u2043\u2219]?\s*(?:amount|category|item|merchant|date|currency)\s*:/i.test(l));
      const hasPromptDirectiveBullets = lines.some(l => /^[•*\\-\u2022\u2023\u25E6\u2043\u2219]?\s*(?:Acknowledge the|Mention the|Keep it|Do NOT automatically)\b/i.test(l));

      const isParagraphThinking = hasExtractionBullets ||
                         hasPromptDirectiveBullets ||
                         thinkingPatterns.some(pt => pt.test(p)) ||
                         /^For\s+["“]/i.test(p) ||
                         /^\d+\.\s*["“].+?["”]/i.test(p) ||
                         /EXPENSE DETECTION|CHALLENGE DETECTION/i.test(p) ||
                         /The rules say/i.test(p) ||
                         /Wait, let me/i.test(p) ||
                         /re-read the/i.test(p) ||
                         /Then at the end/i.test(p) ||
                         /Then the data block/i.test(p) ||
                         /strict prohibition/i.test(p) ||
                         /Write your natural conversational/i.test(p) ||
                         /Looking at the hidden context/i.test(p) ||
                         /careful not to overstep/i.test(p) ||
                         /Do NOT automatically save/i.test(p) ||
                         /NEVER mix/i.test(p) ||
                         /Breaking it down/i.test(p) ||
                         /Breaking down/i.test(p) ||
                         /mentioning two expenses/i.test(p) ||
                         (/mentioning/i.test(p) && /expenses/i.test(p)) ||
                         /^\s*["“].+?["”]\s*=\s*["”].+?["”]/ .test(p) ||
                         /^\s*[•*-]\s*["“].+?["”]\s*=/ .test(p) ||
                         /The user'?s message:?/i.test(p) ||
                         /\d+\.\s*.+? for \d+ MMK/i.test(p) ||
                         /^(?:Something like|Wait,)/i.test(p) ||
                         /Analyze User Input|Identify Required Fields|Determine Response Language|Formulate Extraction|Possible response/i.test(p) ||
                         /The format expected is|This is an instruction/i.test(p) ||
                         /^\d+\.\s*\*\*Draft\s*[-–]\s*Mental Refinement:\*\*/i.test(p) ||
                         /hidden context/i.test(p) ||
                         /JSON block/i.test(p) ||
                         /json structure/i.test(p) ||
                         /prompt_challenge_confirmation/i.test(p) ||
                         /mark_challenge_saving/i.test(p) ||
                         /Active Challenges \(/i.test(p) ||
                         /•\s*\w+:.*MMK.*complete/i.test(p) ||
                         /So for this case/i.test(p) ||
                         /two challenges/i.test(p) ||
                         /multiple challenges/i.test(p) ||
                         /For the expense part/i.test(p) ||
                         /For the challenge part/i.test(p) ||
                         /mixed expense/i.test(p) ||
                         /Example for/i.test(p) ||
                         /^User:/i.test(p) ||
                         /^Assistant:/i.test(p) ||
                         /^\s*[[{}\]]\s*$/.test(p);
      if (!isParagraphThinking) {
        userFacing.push(p);
      }
    }

    // Filter out very short fragments that are likely thinking remnants
    const filteredFacing = userFacing.filter(p => p.length > 10 || /\d/.test(p));

    const isBurmese = /[က-႟]/.test(text);
    if (filteredFacing.length > 0) {
      let combined = filteredFacing.join("\n\n").trim();
      combined = combined.replace(/(?:\r?\n)+#{1,6}\s+[^\n]+$/g, "").trim();
      const substantive = combined.split("\n").filter(l => l.trim() && !l.trim().startsWith("#"));
      if (substantive.length === 0) {
        const fallbackAck = isBurmese ? "မှတ်သားထားပါတယ်။ အောက်ပါ Card တွင် အတည်ပြုပေးပါ။" : "I've noted this. Please confirm below.";
        return expenseData ? fallbackAck + expenseData : "";
      }
      return combined + expenseData;
    } else {
      if (expenseData) {
        const fallbackAck = isBurmese ? "မှတ်သားထားပါတယ်။ အောက်ပါ Card တွင် အတည်ပြုပေးပါ။" : "I've noted this. Please confirm below.";
        return fallbackAck + expenseData;
      }
      return "";
    }
  }

  let result = text.trim();
  result = result.replace(/(?:\r?\n)+#{1,6}\s+[^\n]+$/g, "").trim();
  const substantive = result.split("\n").filter(l => l.trim() && !l.trim().startsWith("#"));
  if (substantive.length === 0) {
    return "";
  }
  return result;
}

module.exports = async function handler(req, res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") return res.status(200).end();
  if (req.method !== "POST") return res.status(405).json({ error: "Method not allowed" });

  const { messages = [], systemPrompt, userGeminiKey, userOpenRouterKey } = req.body;
  if (!messages.length) return res.status(400).json({ error: "messages array is required" });

  const prompt = systemPrompt || SYSTEM_PROMPT;
  const errors = [];

  const geminiProvider = providers.find(p => p.name === "Gemini");
  const openRouterProvider = providers.find(p => p.name === "OpenRouter");

  const executionChain = [];

  // 1. User Gemini Key (highest priority if provided)
  if (userGeminiKey && userGeminiKey.trim()) {
    executionChain.push({
      label: "User Gemini Key",
      provider: geminiProvider,
      apiKey: userGeminiKey.trim()
    });
  }

  // 2. User OpenRouter Key (if provided)
  if (userOpenRouterKey && userOpenRouterKey.trim()) {
    executionChain.push({
      label: "User OpenRouter Key",
      provider: openRouterProvider,
      apiKey: userOpenRouterKey.trim()
    });
  }

  // 3. Fallback: System Gemini Key
  const systemGeminiKey = (process.env.GEMINI_API_KEY || "").trim();
  if (systemGeminiKey) {
    executionChain.push({
      label: "System Gemini Key",
      provider: geminiProvider,
      apiKey: systemGeminiKey
    });
  }

  // 4. Fallback: System OpenRouter Key
  const systemOpenRouterKey = (process.env.OPENROUTER_API_KEY || "").trim();
  if (systemOpenRouterKey) {
    executionChain.push({
      label: "System OpenRouter Key",
      provider: openRouterProvider,
      apiKey: systemOpenRouterKey
    });
  }

  for (const step of executionChain) {
    if (!step.provider || !step.apiKey) continue;

    try {
      console.log(`Trying ${step.label} (${step.provider.name})...`);
      const result = await step.provider.call(messages, prompt, step.apiKey);
      console.log(`✓ ${step.label} succeeded`);
      return res.status(200).json(result);
    } catch (err) {
      const status = err.status || 500;
      const msg = err.message || "Unknown error";
      console.error(`✗ ${step.label} failed (${status}): ${msg}`);
      errors.push(`${step.label}: ${msg}`);
    }
  }

  return res.status(503).json({
    error: "All providers failed",
    details: errors,
  });
};
