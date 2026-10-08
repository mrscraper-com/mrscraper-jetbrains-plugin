package com.mrscraper.mcp

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Path

/** Connects and disconnects the MrScraper MCP server in the shared configuration. */
@Service(Service.Level.APP)
class MrScraperMcpService {
    private val log = logger<MrScraperMcpService>()
    private val credentialAttributes = CredentialAttributes(generateServiceName("MrScraper MCP", "API key"))

    val configPath: Path
        get() = McpConfigLocation.resolve()

    /** Writes the server entry and keeps the key in the IDE password storage. Throws on failure. */
    fun connect(apiKey: String) {
        val path = configPath
        McpConfigFile.connect(path, apiKey)
        PasswordSafe.instance.setPassword(credentialAttributes, apiKey)
        MrScraperMcpSettings.getInstance().state.connected = true
        refresh(path)
    }

    /** Removes the server entry and forgets the key. Throws on failure. */
    fun disconnect() {
        removeEntry()
        forgetApiKey()
        MrScraperMcpSettings.getInstance().state.connected = false
    }

    /** Removes only the server entry, keeping the stored key so [restore] can add it back. */
    fun removeEntry() {
        val path = configPath
        if (McpConfigFile.disconnect(path)) refresh(path)
    }

    fun forgetApiKey() {
        PasswordSafe.instance.setPassword(credentialAttributes, null)
    }

    /** Adds the entry back from the stored key, for example after the plugin is enabled again. */
    fun restore() {
        val apiKey = PasswordSafe.instance.getPassword(credentialAttributes)?.takeIf { it.isNotBlank() } ?: return
        runCatching { McpConfigFile.connect(configPath, apiKey) }
            .onSuccess { refresh(configPath) }
            .onFailure { log.warn("Could not restore the MrScraper MCP server entry", it) }
    }

    /** The last four characters of the configured API key, or null when MrScraper isn't configured. */
    fun connectedKeySuffix(): String? =
        runCatching { McpConfigFile.configuredApiKey(configPath) }.getOrNull()?.takeLast(4)

    /** Lets the IDE notice the change so AI agents reload their MCP servers without a restart. */
    private fun refresh(path: Path) {
        LocalFileSystem.getInstance().refreshIoFiles(listOf(path.toFile()))
    }

    companion object {
        fun getInstance(): MrScraperMcpService = service()
    }
}
