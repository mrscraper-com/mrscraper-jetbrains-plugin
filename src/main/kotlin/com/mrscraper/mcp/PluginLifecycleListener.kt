package com.mrscraper.mcp

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.diagnostic.logger

/**
 * Removes the MrScraper server entry when the plugin is disabled or uninstalled, and adds it back
 * when a disabled plugin is enabled again. Uninstalling also forgets the stored key.
 */
class PluginLifecycleListener : DynamicPluginListener {
    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        if (pluginDescriptor.pluginId.idString != MrScraperMcp.PLUGIN_ID) return
        if (MrScraperMcpSettings.getInstance().state.connected) MrScraperMcpService.getInstance().restore()
    }

    override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
        if (isUpdate || pluginDescriptor.pluginId.idString != MrScraperMcp.PLUGIN_ID) return
        val service = MrScraperMcpService.getInstance()
        runCatching { service.removeEntry() }
            .onFailure { logger<PluginLifecycleListener>().warn("Could not remove the MrScraper MCP server entry", it) }
        if (!PluginManagerCore.isDisabled(pluginDescriptor.pluginId)) {
            service.forgetApiKey()
            MrScraperMcpSettings.getInstance().state.connected = false
        }
    }
}
