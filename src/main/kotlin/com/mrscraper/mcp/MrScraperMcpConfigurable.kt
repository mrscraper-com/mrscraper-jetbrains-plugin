package com.mrscraper.mcp

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.panel
import java.io.IOException
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel

/** Settings | Tools | MrScraper MCP: connection status, connect or update the key, and disconnect. */
class MrScraperMcpConfigurable : Configurable {
    private lateinit var statusLabel: JLabel
    private lateinit var disconnectButton: JButton

    override fun getDisplayName(): String = "MrScraper MCP"

    override fun createComponent(): JComponent {
        val service = MrScraperMcpService.getInstance()
        val content = panel {
            row("Status:") {
                statusLabel = label("").component
            }
            row("MCP configuration:") {
                label(service.configPath.toString())
            }
            row {
                button("Connect or Update API Key…") {
                    ConnectDialog(null).show()
                    updateStatus()
                }
                disconnectButton = button("Disconnect") { disconnect() }.component
            }
            row {
                browserLink("Create an API key", MrScraperMcp.API_TOKENS_URL)
                browserLink("Documentation", MrScraperMcp.DOCS_URL)
            }
        }
        updateStatus()
        return content
    }

    private fun updateStatus() {
        val suffix = MrScraperMcpService.getInstance().connectedKeySuffix()
        statusLabel.text = if (suffix != null) "Connected (API key ending in $suffix)" else "Not connected"
        disconnectButton.isEnabled = suffix != null
    }

    private fun disconnect() {
        try {
            MrScraperMcpService.getInstance().disconnect()
        } catch (e: McpConfigFile.InvalidConfigException) {
            Messages.showErrorDialog(e.message, "MrScraper MCP")
        } catch (e: IOException) {
            Messages.showErrorDialog("Couldn't update the MCP configuration: ${e.message}", "MrScraper MCP")
        }
        updateStatus()
    }

    override fun isModified(): Boolean = false

    override fun apply() = Unit
}
