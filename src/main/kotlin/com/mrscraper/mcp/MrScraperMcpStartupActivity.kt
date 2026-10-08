package com.mrscraper.mcp

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.util.concurrent.atomic.AtomicBoolean

/** Suggests connecting once per IDE session until the user connects or declines. */
class MrScraperMcpStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val settings = MrScraperMcpSettings.getInstance()
        if (settings.state.promptDismissed) return
        if (MrScraperMcpService.getInstance().connectedKeySuffix() != null) return
        if (!suggestionShown.compareAndSet(false, true)) return

        NotificationGroupManager.getInstance()
            .getNotificationGroup(MrScraperMcp.NOTIFICATION_GROUP)
            .createNotification(
                "Connect MrScraper MCP",
                "Give the AI agents in your IDE MrScraper's web scraping tools by connecting your MrScraper API key.",
                NotificationType.INFORMATION,
            )
            .addAction(NotificationAction.createSimpleExpiring("Connect…") { ConnectDialog(project).show() })
            .addAction(NotificationAction.createSimpleExpiring("Don't show again") { settings.state.promptDismissed = true })
            .notify(project)
    }

    private companion object {
        val suggestionShown = AtomicBoolean(false)
    }
}
