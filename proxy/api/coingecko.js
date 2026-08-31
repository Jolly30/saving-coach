/**
 * CoinGecko API Proxy
 *
 * Hides the API key server-side. The app calls this proxy instead of CoinGecko directly.
 *
 * Endpoints (POST):
 *   /api/coingecko?action=search&query=bitcoin
 *   /api/coingecko?action=price&ids=bitcoin,ethereum&vs_currencies=usd&include_24hr_change=true
 */

const COINGECKO_BASE = "https://api.coingecko.com/api/v3";

module.exports = async function handler(req, res) {
  // CORS headers
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") return res.status(200).end();
  if (req.method !== "POST") return res.status(405).json({ error: "Method not allowed" });

  const apiKey = process.env.COINGECKO_API_KEY;
  if (!apiKey) {
    return res.status(500).json({ error: "COINGECKO_API_KEY not configured" });
  }

  const { action, query, ids, vs_currencies = "usd", include_24hr_change = "true" } = req.body;

  try {
    let url;

    switch (action) {
      case "search":
        if (!query) return res.status(400).json({ error: "query is required for search" });
        url = `${COINGECKO_BASE}/search?query=${encodeURIComponent(query)}`;
        break;

      case "price":
        if (!ids) return res.status(400).json({ error: "ids is required for price" });
        url = `${COINGECKO_BASE}/simple/price?ids=${encodeURIComponent(ids)}&vs_currencies=${vs_currencies}&include_24hr_change=${include_24hr_change}`;
        break;

      default:
        return res.status(400).json({ error: "Invalid action. Use 'search' or 'price'" });
    }

    const response = await fetch(url, {
      headers: {
        "x-cg-demo-api-key": apiKey,
        "Accept": "application/json"
      }
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error(`CoinGecko API error (${response.status}):`, errorText);
      return res.status(response.status).json({
        error: `CoinGecko API error: ${response.status}`,
        details: errorText
      });
    }

    const data = await response.json();
    return res.status(200).json(data);

  } catch (error) {
    console.error("CoinGecko proxy error:", error);
    return res.status(500).json({
      error: "Proxy error",
      message: error.message
    });
  }
};
