# MrScraper MCP for JetBrains IDEs

Connects the AI agents in your JetBrains IDE to MrScraper's hosted
[MCP server](https://docs.mrscraper.com/docs/getting-started/mcp-server) for web
scraping, structured data extraction, and Google search.

One connection covers every JetBrains AI agent that reads the shared MCP
configuration, `~/.ai/mcp/mcp.json`: AI Assistant, Junie, Codex, Claude, and
GitHub Copilot.

## Requirements

- A JetBrains IDE, version 2025.1 or later, with an AI agent plugin installed.
- A [MrScraper](https://app.mrscraper.com) account. The free plan includes
  1,000 tokens a month.

## Install and connect

1. Install **MrScraper MCP** from the JetBrains Marketplace (**Settings |
   Plugins | Marketplace**).
2. Create an API key at <https://app.mrscraper.com/api-tokens>.
3. Click **Connect…** in the notification that appears, or choose
   **Tools | Connect MrScraper MCP…**, and paste the key.
4. If the IDE asks whether to enable the `mrscraper` MCP server, accept it.

Try it with your AI agent:

```text
Fetch https://www.scrapethissite.com/pages/simple/ and summarize the page.
```

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
