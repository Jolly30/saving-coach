/**
 * Finnhub API Proxy
 *
 * Hides the API key server-side. The app calls this proxy instead of Finnhub directly.
 *
 * Endpoints (POST):
 *   /api/finnhub?action=search&q=AAPL
 *   /api/finnhub?action=quote&symbol=AAPL
 *   /api/finnhub?action=news&category=general
 */

const FINNHUB_BASE = "https://finnhub.io/api/v1";

module.exports = async function handler(req, res) {
  // CORS headers
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") return res.status(200).end();
  if (req.method !== "POST") return res.status(405).json({ error: "Method not allowed" });

  const apiKey = process.env.FINNHUB_API_KEY;
  if (!apiKey) {
    return res.status(500).json({ error: "FINNHUB_API_KEY not configured" });
  }

  const { action, q, symbol, category = "general" } = req.body;

  try {
    let url;

    switch (action) {
      case "search":
        if (!q) return res.status(400).json({ error: "q (query) is required for search" });
        url = `${FINNHUB_BASE}/search?q=${encodeURIComponent(q)}&token=${apiKey}`;
        break;

      case "quote":
        if (!symbol) return res.status(400).json({ error: "symbol is required for quote" });
        url = `${FINNHUB_BASE}/quote?symbol=${encodeURIComponent(symbol)}&token=${apiKey}`;
        break;

      case "news":
        url = `${FINNHUB_BASE}/news?category=${category}&token=${apiKey}`;
        break;

      default:
        return res.status(400).json({ error: "Invalid action. Use 'search', 'quote', or 'news'" });
    }

    const response = await fetch(url);

    if (!response.ok) {
      const errorText = await response.text();
      console.error(`Finnhub API error (${response.status}):`, errorText);
      return res.status(response.status).json({
        error: `Finnhub API error: ${response.status}`,
        details: errorText
      });
    }

    const data = await response.json();
    return res.status(200).json(data);

  } catch (error) {
    console.error("Finnhub proxy error:", error);
    return res.status(500).json({
      error: "Proxy error",
      message: error.message
    });
  }
};
