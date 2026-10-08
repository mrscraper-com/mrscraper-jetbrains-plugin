package com.mrscraper.mcp

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(name = "MrScraperMcpSettings", storages = [Storage("mrscraperMcp.xml")])
class MrScraperMcpSettings : SimplePersistentStateComponent<MrScraperMcpSettings.SettingsState>(SettingsState()) {

    class SettingsState : BaseState() {
        /** The user connected through this plugin and has not disconnected since. */
        var connected by property(false)

        /** The user chose not to see the startup suggestion to connect again. */
        var promptDismissed by property(false)
    }

    companion object {
        fun getInstance(): MrScraperMcpSettings = service()
    }
}
