package com.mrscraper.mcp

import com.intellij.openapi.util.registry.Registry
import java.nio.file.Path
import java.nio.file.Paths

internal object MrScraperMcp {
    const val PLUGIN_ID = "com.mrscraper.mcp"
    const val NOTIFICATION_GROUP = "MrScraper MCP"
    const val API_TOKENS_URL = "https://app.mrscraper.com/api-tokens"
    const val DOCS_URL = "https://docs.mrscraper.com/docs/getting-started/mcp-server"
    const val VERIFY_URL = "https://api.app.mrscraper.com/api/v1/subscription-accounts"
}

/**
 * Location of the MCP configuration that JetBrains AI agents share: `~/.ai/mcp/mcp.json` unless
 * the `llm.mcp.client.global.mcp.json.path` registry key points elsewhere.
 */
internal object McpConfigLocation {
    private const val REGISTRY_KEY = "llm.mcp.client.global.mcp.json.path"
    private const val DEFAULT_RELATIVE_PATH = ".ai/mcp/mcp.json"

    fun resolve(): Path {
        val home = Paths.get(System.getProperty("user.home"))
        val configured = runCatching { Registry.stringValue(REGISTRY_KEY) }.getOrNull()?.trim().orEmpty()
        if (configured.isEmpty()) return home.resolve(DEFAULT_RELATIVE_PATH)
        val path = Paths.get(configured)
        return if (path.isAbsolute) path else home.resolve(configured)
    }
}
