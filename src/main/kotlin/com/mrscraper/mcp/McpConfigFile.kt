package com.mrscraper.mcp

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions

/**
 * Reads and updates the MCP configuration file that JetBrains AI agents share.
 *
 * Only the `mcpServers.mrscraper` entry is managed. Every other key and server is kept as parsed,
 * including numbers. A file that is not a JSON object is never overwritten.
 *
 * Uses no IntelliJ Platform API so it can be tested without an IDE.
 */
internal object McpConfigFile {
    const val SERVERS_KEY = "mcpServers"
    const val SERVER_NAME = "mrscraper"
    const val SERVER_URL = "https://mcp.mrscraper.com/mcp"

    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    class InvalidConfigException(message: String) : Exception(message)

    /** The server entry for [apiKey]: the hosted MrScraper MCP server with the key as a bearer token. */
    fun entryFor(apiKey: String): JsonObject = JsonObject().apply {
        addProperty("url", SERVER_URL)
        add("headers", JsonObject().apply { addProperty("Authorization", "Bearer $apiKey") })
    }

    /** Adds or replaces the MrScraper server entry. Creates the file and its folders when missing. */
    fun connect(path: Path, apiKey: String) {
        require(apiKey.isNotBlank()) { "The API key is empty." }
        val root = read(path)
        val servers = when (val existing = root.get(SERVERS_KEY)) {
            null -> JsonObject().also { root.add(SERVERS_KEY, it) }
            else -> existing.takeIf { it.isJsonObject }?.asJsonObject
                ?: throw InvalidConfigException("\"$SERVERS_KEY\" in $path is not a JSON object. Fix the file, then try again.")
        }
        servers.add(SERVER_NAME, entryFor(apiKey))
        write(path, root)
    }

    /**
     * Removes the MrScraper entry when it points at the hosted server. An entry the user set up
     * differently, such as a local server, is left alone. Returns whether the file changed.
     */
    fun disconnect(path: Path): Boolean {
        if (!Files.exists(path)) return false
        val root = read(path)
        val servers = root.get(SERVERS_KEY)?.takeIf { it.isJsonObject }?.asJsonObject ?: return false
        if (!isHostedEntry(servers.get(SERVER_NAME))) return false
        servers.remove(SERVER_NAME)
        write(path, root)
        return true
    }

    /** The API key in the hosted MrScraper entry, or null when the file has no such entry. */
    fun configuredApiKey(path: Path): String? {
        if (!Files.exists(path)) return null
        val servers = read(path).get(SERVERS_KEY)?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
        val entry = servers.get(SERVER_NAME)?.takeIf { isHostedEntry(it) }?.asJsonObject ?: return null
        val headers = entry.get("headers")?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
        val authorization = headers.entrySet()
            .firstOrNull { it.key.equals("Authorization", ignoreCase = true) }
            ?.value?.takeIf { it.isJsonPrimitive }?.asString ?: return null
        return authorization.removePrefix("Bearer ").trim().takeIf { it.isNotEmpty() }
    }

    internal fun isHostedEntry(entry: JsonElement?): Boolean {
        if (entry == null || !entry.isJsonObject) return false
        val url = entry.asJsonObject.get("url")?.takeIf { it.isJsonPrimitive }?.asString ?: return false
        return url.trimEnd('/') == SERVER_URL
    }

    internal fun read(path: Path): JsonObject {
        if (!Files.exists(path)) return JsonObject()
        val text = String(Files.readAllBytes(path), StandardCharsets.UTF_8)
        if (text.isBlank()) return JsonObject()
        val element = try {
            JsonParser.parseString(text)
        } catch (e: JsonParseException) {
            throw InvalidConfigException("$path is not valid JSON. Fix or remove it, then try again.")
        }
        if (!element.isJsonObject) {
            throw InvalidConfigException("$path does not contain a JSON object. Fix or remove it, then try again.")
        }
        return element.asJsonObject
    }

    /** Writes atomically, follows a symbolic link to its target, and limits access to the owner. */
    internal fun write(path: Path, root: JsonObject) {
        val target = if (Files.isSymbolicLink(path)) path.toRealPath() else path.toAbsolutePath()
        val directory = target.parent
        Files.createDirectories(directory)
        val temporary = Files.createTempFile(directory, ".mcp", ".json.tmp")
        try {
            Files.write(temporary, (gson.toJson(root) + "\n").toByteArray(StandardCharsets.UTF_8))
            restrictToOwner(temporary)
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun restrictToOwner(file: Path) {
        if ("posix" in FileSystems.getDefault().supportedFileAttributeViews()) {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"))
        }
    }
}
