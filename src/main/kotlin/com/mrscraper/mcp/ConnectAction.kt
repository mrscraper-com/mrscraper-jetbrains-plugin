package com.mrscraper.mcp

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

/** Tools | Connect MrScraper MCP… */
class ConnectAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        ConnectDialog(e.project).show()
    }
}
