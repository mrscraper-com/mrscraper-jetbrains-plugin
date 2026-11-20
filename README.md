# MrScraper MCP for JetBrains IDEs

Connects the AI agents in your JetBrains IDE to MrScraper's hosted
[MCP server](https://docs.mrscraper.com/docs/getting-started/mcp-server) for web
scraping and web search.

One connection covers every JetBrains AI agent that reads the shared MCP
configuration, `~/.ai/mcp/mcp.json`: AI Assistant, Junie, Codex, Claude, and
GitHub Copilot.

## Requirements

- A JetBrains IDE, version 2025.1 or later, with an AI agent plugin installed.
- A [MrScraper](https://app.mrscraper.com) account.

## Install and connect

1. Install **MrScraper MCP** from the JetBrains Marketplace (**Settings |
   Plugins | Marketplace**).
2. Create an API key at <https://app.mrscraper.com/api-tokens>.
3. Click **Connect…** in the notification that appears, or choose
   **Tools | Connect MrScraper MCP…**, and paste the key.
4. If the IDE asks whether to enable the `mrscraper` MCP server, accept it.

Try it with your AI agent:

```text
Fetch https://www.scrapethissite.com and summarize the page.
```

## Tools

The hosted MCP server gives your AI agent seven tools:

| Tool | What it does |
| --- | --- |
| `status` | Return the raw account status API response without summarizing, redacting body fields, or adding calculated fields. |
| `scrapeHtml` | Retrieve the raw HTML of a page. Browser rendering is optional. |
| `scrapeMarkdown` | Retrieve a page as Markdown. Including HTML also enables browser rendering and returns both outputs as JSON. |
| `getScreenshot` | Capture a base64 screenshot of the full page or top viewport. Browser rendering is always enabled. |
| `scrapeBrand` | Extract a website's colors, typography, logos, and social links as JSON. |
| `scrapeJson` | Extract structured JSON from a page using a natural-language prompt. Append Json Schema: and a JSON schema to shape the output. |
| `webSearch` | Search the web and return ranked results with titles, URLs, descriptions, and sources. |

The six scraping and search tools use tokens from your MrScraper plan.

## What the plugin changes

- Checks the key with `GET https://api.app.mrscraper.com/api/v1/subscription-accounts`.
- Saves the key in the IDE password storage.
- Adds this entry to `~/.ai/mcp/mcp.json` (or the path set by the
  `llm.mcp.client.global.mcp.json.path` registry key) and leaves every other
  server unchanged:

  ```json
  {
    "mcpServers": {
      "mrscraper": {
        "url": "https://mcp.mrscraper.com/mcp",
        "headers": { "Authorization": "Bearer <your API key>" }
      }
    }
  }
  ```

  The file is written atomically and made readable only by you. A file that
  isn't valid JSON is never overwritten; the plugin reports the problem
  instead.

Manage the connection in **Settings | Tools | MrScraper MCP**. **Disconnect**
removes the entry and forgets the key. Disabling the plugin removes the entry,
and enabling it again restores it; uninstalling removes the entry and the key.

The plugin collects no telemetry. When an AI agent uses MrScraper, the target
URLs and tool inputs go to MrScraper; see the
[privacy policy](https://mrscraper.com/privacy-policy).

## Build

Requires JDK 21.

```bash
./gradlew test buildPlugin verifyPlugin
```

The plugin ZIP is written to `build/distributions/`.

## Support

- Documentation: <https://docs.mrscraper.com/docs/getting-started/mcp-server>
- Email: <support@mrscraper.com>

## License

[MIT](LICENSE)
