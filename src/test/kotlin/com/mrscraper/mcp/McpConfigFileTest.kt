package com.mrscraper.mcp

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

class McpConfigFileTest {
    @TempDir
    lateinit var home: Path

    private val config: Path get() = home.resolve(".ai/mcp/mcp.json")

    private fun writeConfig(text: String) {
        Files.createDirectories(config.parent)
        Files.writeString(config, text)
    }

    private fun parsed() = JsonParser.parseString(Files.readString(config)).asJsonObject

    @Test
    fun `connect creates the file and folders with the hosted server and bearer key`() {
        McpConfigFile.connect(config, "key-1234")

        val entry = parsed().getAsJsonObject("mcpServers").getAsJsonObject("mrscraper")
        assertEquals("https://mcp.mrscraper.com/mcp", entry.get("url").asString)
        assertEquals("Bearer key-1234", entry.getAsJsonObject("headers").get("Authorization").asString)
        assertEquals("key-1234", McpConfigFile.configuredApiKey(config))
    }

    @Test
    fun `connect keeps other servers, top-level keys, and integer values unchanged`() {
        writeConfig(
            """
            {
              "mcpServers": {
                "local": { "command": "node", "args": ["server.js"], "env": { "PORT": "8000" }, "timeout": 30 }
              },
              "otherSetting": { "retries": 3, "ratio": 0.5, "enabled": true }
            }
            """.trimIndent(),
        )

        McpConfigFile.connect(config, "key-1234")

        val text = Files.readString(config)
        assertTrue(text.contains("\"timeout\": 30") && !text.contains("30.0"), text)
        assertTrue(text.contains("\"retries\": 3") && !text.contains("3.0"), text)
        val root = parsed()
        val local = root.getAsJsonObject("mcpServers").getAsJsonObject("local")
        assertEquals("node", local.get("command").asString)
        assertEquals("8000", local.getAsJsonObject("env").get("PORT").asString)
        assertEquals(0.5, root.getAsJsonObject("otherSetting").get("ratio").asDouble)
        assertTrue(root.getAsJsonObject("mcpServers").has("mrscraper"))
    }

    @Test
    fun `connect again replaces the key`() {
        McpConfigFile.connect(config, "old-key")
        McpConfigFile.connect(config, "new-key")

        assertEquals("new-key", McpConfigFile.configuredApiKey(config))
        assertEquals(1, parsed().getAsJsonObject("mcpServers").size())
    }

    @Test
    fun `disconnect removes only the hosted entry`() {
        writeConfig("""{ "mcpServers": { "local": { "command": "node" } } }""")
        McpConfigFile.connect(config, "key-1234")

        assertTrue(McpConfigFile.disconnect(config))

        val servers = parsed().getAsJsonObject("mcpServers")
        assertFalse(servers.has("mrscraper"))
        assertTrue(servers.has("local"))
        assertNull(McpConfigFile.configuredApiKey(config))
    }

    @Test
    fun `disconnect leaves a mrscraper entry the user set up differently`() {
        val original = """{ "mcpServers": { "mrscraper": { "command": "npx", "args": ["-y", "@mrscraper/mcp"] } } }"""
        writeConfig(original)

        assertFalse(McpConfigFile.disconnect(config))
        assertEquals(original, Files.readString(config))
        assertNull(McpConfigFile.configuredApiKey(config))
    }

    @Test
    fun `disconnect without a file does nothing`() {
        assertFalse(McpConfigFile.disconnect(config))
        assertFalse(Files.exists(config))
    }

    @Test
    fun `invalid JSON is reported and never overwritten`() {
        val broken = """{ "mcpServers": { "local": """
        writeConfig(broken)

        assertThrows(McpConfigFile.InvalidConfigException::class.java) { McpConfigFile.connect(config, "key-1234") }
        assertThrows(McpConfigFile.InvalidConfigException::class.java) { McpConfigFile.disconnect(config) }
        assertEquals(broken, Files.readString(config))
    }

    @Test
    fun `a file that is not a JSON object is never overwritten`() {
        writeConfig("""[ "not", "an", "object" ]""")

        assertThrows(McpConfigFile.InvalidConfigException::class.java) { McpConfigFile.connect(config, "key-1234") }
        assertEquals("""[ "not", "an", "object" ]""", Files.readString(config))
    }

    @Test
    fun `mcpServers that is not an object is never overwritten`() {
        val original = """{ "mcpServers": [] }"""
        writeConfig(original)

        assertThrows(McpConfigFile.InvalidConfigException::class.java) { McpConfigFile.connect(config, "key-1234") }
        assertEquals(original, Files.readString(config))
    }

    @Test
    fun `an empty file is treated as an empty configuration`() {
        writeConfig("")

        McpConfigFile.connect(config, "key-1234")

        assertEquals("key-1234", McpConfigFile.configuredApiKey(config))
    }

    @Test
    fun `the written file is readable only by its owner`() {
        assumeTrue("posix" in FileSystems.getDefault().supportedFileAttributeViews())

        McpConfigFile.connect(config, "key-1234")

        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(config)))
    }

    @Test
    fun `a symbolic link is kept and its target is updated`() {
        assumeTrue("posix" in FileSystems.getDefault().supportedFileAttributeViews())
        val target = home.resolve("dotfiles/mcp.json")
        Files.createDirectories(target.parent)
        Files.writeString(target, """{ "mcpServers": { "local": { "command": "node" } } }""")
        Files.createDirectories(config.parent)
        Files.createSymbolicLink(config, target)

        McpConfigFile.connect(config, "key-1234")

        assertTrue(Files.isSymbolicLink(config))
        assertEquals("key-1234", McpConfigFile.configuredApiKey(target))
        assertTrue(Files.readString(target).contains("\"local\""))
    }

    @Test
    fun `the key is read from a lowercase authorization header`() {
        writeConfig(
            """{ "mcpServers": { "mrscraper": { "url": "https://mcp.mrscraper.com/mcp/", "headers": { "authorization": "Bearer abc" } } } }""",
        )

        assertEquals("abc", McpConfigFile.configuredApiKey(config))
    }

    @Test
    fun `a blank key is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { McpConfigFile.connect(config, "  ") }
        assertFalse(Files.exists(config))
    }
}
