package com.mrscraper.mcp

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import java.io.IOException
import javax.swing.JComponent

/** Asks for a MrScraper API key, checks it with MrScraper, and connects the MCP server. */
class ConnectDialog(private val project: Project?) : DialogWrapper(project, true) {
    private val apiKeyField = JBPasswordField()

    init {
        title = "Connect MrScraper MCP"
        setOKButtonText("Connect")
        setCancelButtonText("Not Now")
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row {
            text(
                "Gives the AI agents in your IDE, such as AI Assistant, Junie, and GitHub Copilot, " +
                    "MrScraper's tools for fetching web pages, extracting structured data, and searching Google.",
                maxLineLength = 70,
            )
        }
        row("API key:") {
            cell(apiKeyField).columns(COLUMNS_LARGE)
        }
        row {
            browserLink("Create an API key", MrScraperMcp.API_TOKENS_URL)
        }
        row {
            comment(
                "The key is checked with MrScraper, saved in the IDE password storage, and added to " +
                    "${MrScraperMcpService.getInstance().configPath}, the MCP configuration your AI agents read.",
                maxLineLength = 70,
            )
        }
    }

    override fun getPreferredFocusedComponent(): JComponent = apiKeyField

    override fun doValidate(): ValidationInfo? =
        if (apiKeyField.password.isEmpty()) ValidationInfo("Enter your MrScraper API key.", apiKeyField) else null

    override fun doOKAction() {
        val apiKey = String(apiKeyField.password).trim()
        val outcome = ProgressManager.getInstance().runProcessWithProgressSynchronously(
            ThrowableComputable<Outcome, RuntimeException> { connect(apiKey) },
            "Connecting MrScraper MCP",
            false,
            project,
        )
        when (outcome) {
            Outcome.Connected -> {
                super.doOKAction()
                notifyConnected(project)
            }
            is Outcome.Failed -> setErrorText(outcome.message, apiKeyField)
        }
    }

    private sealed interface Outcome {
        data object Connected : Outcome
        data class Failed(val message: String) : Outcome
    }

    private fun connect(apiKey: String): Outcome = when (val check = ApiKeyCheck.check(apiKey)) {
        ApiKeyCheck.Result.Rejected -> Outcome.Failed("MrScraper rejected this API key. Check it and try again.")
        is ApiKeyCheck.Result.Failed -> Outcome.Failed("Couldn't reach MrScraper: ${check.reason}")
        ApiKeyCheck.Result.Valid -> try {
            MrScraperMcpService.getInstance().connect(apiKey)
            Outcome.Connected
        } catch (e: McpConfigFile.InvalidConfigException) {
            Outcome.Failed(e.message ?: "The MCP configuration file is not valid JSON.")
        } catch (e: IOException) {
            Outcome.Failed("Couldn't update the MCP configuration: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun notifyConnected(project: Project?) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(MrScraperMcp.NOTIFICATION_GROUP)
            .createNotification(
                "MrScraper MCP connected",
                "Your AI agents can now use MrScraper. If the IDE asks whether to enable the mrscraper MCP server, accept it.",
                NotificationType.INFORMATION,
            )
            .notify(project)
    }
}
