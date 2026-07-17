package de.bame.bamelitebans.service

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.event.EventTask
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import de.bame.bamelitebans.config.ConfigService
import de.bame.bamelitebans.util.ColorParser
import de.bame.bamelitebans.util.JsonUtil
import org.slf4j.Logger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture

class AiModerationService(
    private val proxy: ProxyServer,
    private val configService: ConfigService,
    private val logger: Logger
) {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build()

    // Lokale Regex-/Heuristik-Pattern für sofortiges Auto-Punish (0.1ms) - fängt auch Leerzeichen zwischen Buchstaben (z.B. s i e g h e i l) sauber ab
    private val patterns = mapOf(
        "werbung" to Regex("""(?i)\b(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+(?:de|net|com|org|eu|gg|to|co|info|me)\b|\b(?:\d{1,3}\.){3}\d{1,3}(?::\d{1,5})?\b|(?:https?://|discord\.gg/|t\.me/)"""),
        "rassismus" to Regex("""(?i)\b(?:n[\s*._-]*[i1!j][\s*._-]*[gq]+[\s*._-]*[e3a@#*!]+[\s*._-]*r|n[\s*._-]*[e3][\s*._-]*g[\s*._-]*[e3][\s*._-]*r|k[\s*._-]*k[\s*._-]*k)\b"""),
        "nationalsozialismus" to Regex("""(?i)\b(?:s[\s*._-]*[i1!j][\s*._-]*[e3][\s*._-]*g[\s*._-]*h[\s*._-]*[e3][\s*._-]*[i1!j][\s*._-]*l|h[\s*._-]*[i1!j][\s*._-]*t[\s*._-]*l[\s*._-]*[e3][\s*._-]*r|n[\s*._-]*s[\s*._-]*d[\s*._-]*[a@][\s*._-]*p|1488|n[\s*._-]*[a@][\s*._-]*z[\s*._-]*[i1!j])\b"""),
        "antisemitismus" to Regex("""(?i)\b(?:v[\s*._-]*[e3][\s*._-]*r[\s*._-]*g[\s*._-]*[a@4][\s*._-]*s[\s*._-]*[e3][\s*._-]*n|h[\s*._-]*[o0][\s*._-]*l[\s*._-]*[o0][\s*._-]*c[\s*._-]*[a@4][\s*._-]*u[\s*._-]*s[\s*._-]*t|z[\s*._-]*y[\s*._-]*k[\s*._-]*l[\s*._-]*[o0][\s*._-]*n|d[\s*._-]*[r1][\s*._-]*[i1!j][\s*._-]*t[\s*._-]*t[\s*._-]*[e3][\s*._-]*s[\s*._-]*r[\s*._-]*[e3][\s*._-]*[i1!j][\s*._-]*c[\s*._-]*h)\b"""),
        "beleidigung" to Regex("""(?i)\b(?:h[\s*._-]*[uüv][\s*._-]*r[\s*._-]*[e3][\s*._-]*n[\s*._-]*s[\s*._-]*[o0][\s*._-]*h[\s*._-]*n|b[\s*._-]*[a@][\s*._-]*s[\s*._-]*t[\s*._-]*[a@][\s*._-]*r[\s*._-]*d|f[\s*._-]*[o0][\s*._-]*t[\s*._-]*z[\s*._-]*[e3]|m[\s*._-]*[i1!j][\s*._-]*s[\s*._-]*s[\s*._-]*g[\s*._-]*[e3][\s*._-]*b[\s*._-]*[uü][\s*._-]*r[\s*._-]*t|w[\s*._-]*[i1!j][\s*._-]*c[\s*._-]*h[\s*._-]*s[\s*._-]*[e3][\s*._-]*r|a[\s*._-]*r[\s*._-]*s[\s*._-]*c[\s*._-]*h[\s*._-]*l[\s*._-]*[o0][\s*._-]*c[\s*._-]*h|k[\s*._-]*[a@][\s*._-]*n[\s*._-]*[a@][\s*._-]*k[\s*._-]*[e3]|n[\s*._-]*[uü][\s*._-]*t[\s*._-]*t[\s*._-]*[e3])\b"""),
        "sexistisches_verhalten" to Regex("""(?i)\b(?:s[\s*._-]*[e3][\s*._-]*x[\s*._-]*[i1!j][\s*._-]*s[\s*._-]*t|f[\s*._-]*r[\s*._-]*[a@][\s*._-]*u[\s*._-]*[e3][\s*._-]*n[\s*._-]*[a@][\s*._-]*r[\s*._-]*z[\s*._-]*t|k[\s*._-]*[üu][\s*._-]*c[\s*._-]*h[\s*._-]*[e3]|g[\s*._-]*[e3][\s*._-]*s[\s*._-]*c[\s*._-]*h[\s*._-]*[i1!j][\s*._-]*r[\s*._-]*r[\s*._-]*s[\s*._-]*p[\s*._-]*[üu][\s*._-]*l[\s*._-]*[e3][\s*._-]*r|s[\s*._-]*c[\s*._-]*h[\s*._-]*l[\s*._-]*[a@][\s*._-]*m[\s*._-]*p[\s*._-]*[e3])\b"""),
        "server_hetze" to Regex("""(?i)\b(?:d[\s*._-]*r[\s*._-]*[e3][\s*._-]*c[\s*._-]*k[\s*._-]*s[\s*._-]*s[\s*._-]*[e3][\s*._-]*r[\s*._-]*v[\s*._-]*[e3][\s*._-]*r|s[\s*._-]*c[\s*._-]*h[\s*._-]*[e3][\s*._-]*[i1!j][\s*._-]*[ßs][\s*._-]*s[\s*._-]*[e3][\s*._-]*r[\s*._-]*v[\s*._-]*[e3][\s*._-]*r|m[\s*._-]*[üu][\s*._-]*l[\s*._-]*l[\s*._-]*s[\s*._-]*[e3][\s*._-]*r[\s*._-]*v[\s*._-]*[e3][\s*._-]*r|s[\s*._-]*[e3][\s*._-]*r[\s*._-]*v[\s*._-]*[e3][\s*._-]*r[\s*._-]*[i1!j][\s*._-]*s[\s*._-]*t[\s*._-]*t[\s*._-]*[o0][\s*._-]*t)\b"""),
        "nsfw" to Regex("""(?i)\b(?:p[\s*._-]*[o0][\s*._-]*r[\s*._-]*n[\s*._-]*[o0]?|[o0]nlyf[a@]ns|n[uü]d[e3]s|x?h[a@]mst[e3]r|c[a@]mg[i1]rl)\b"""),
        "provokation" to Regex("""(?i)\b(?:e[\s*._-]*z+|l[\s*._-]*2[\s*._-]*p+|n[\s*._-]*[o0][\s*._-]*[o0][\s*._-]*b|t[\s*._-]*r[\s*._-]*[a@][\s*._-]*s[\s*._-]*h|b[\s*._-]*[o0][\s*._-]*t|h[\s*._-]*[e3][\s*._-]*u[\s*._-]*l[\s*._-]*d[\s*._-]*[o0][\s*._-]*c[\s*._-]*h|l[\s*._-]*[o0][\s*._-]*s[\s*._-]*[e3][\s*._-]*r|m[\s*._-]*[üu][\s*._-]*l[\s*._-]*l[\s*._-]*s[\s*._-]*p[\s*._-]*[i1!j][\s*._-]*[e3][\s*._-]*l[\s*._-]*[e3][\s*._-]*r|s[\s*._-]*[o0][\s*._-]*s[\s*._-]*c[\s*._-]*h[\s*._-]*l[\s*._-]*[e3][\s*._-]*c[\s*._-]*h[\s*._-]*t)\b""")
    )

    private data class SpamTracker(var text: String, var count: Int, var firstTimestamp: Long)
    private val spamHistory = java.util.concurrent.ConcurrentHashMap<java.util.UUID, SpamTracker>()

    @Subscribe(order = com.velocitypowered.api.event.PostOrder.FIRST)
    fun onPlayerChat(event: PlayerChatEvent): EventTask {
        return EventTask.async {
            processChatAsync(event)
        }
    }

    private fun processChatAsync(event: PlayerChatEvent) {
        if (!configService.automod.isEnabled) return
        val player = event.player
        if (player.hasPermission("bamelitebans.bypass.automod")) return

        val message = event.message

        // 0. Check Spam-Schutz
        if (configService.automod.isSpamEnabled) {
            if (checkAndHandleSpam(event, player, message)) {
                return
            }
        }

        val localMatch = checkLocalHeuristics(message)

        if (localMatch != null) {
            event.result = PlayerChatEvent.ChatResult.message(" *** ")
            if (configService.automod.isMuteCategory(localMatch) && !configService.automod.areMutesEnabled) {
                player.sendMessage(ColorParser.parse(configService.automod.mutesBlockedMessage))
            } else {
                executeAutoPunish(player, localMatch, message)
            }
            return
        }

        // Falls API-Key vorhanden, prüfe zusätzlich asynchron über KI-API
        val apiKey = configService.automod.apiKey
        if (apiKey.isNotBlank()) {
            CompletableFuture.runAsync {
                checkAiApi(player, message, apiKey)
            }
        }
    }

    private fun checkLocalHeuristics(message: String): String? {
        // 1. Check Werbung mit Allowed-Domain Filter
        if (patterns["werbung"]!!.containsMatchIn(message)) {
            val allowed = configService.automod.allowedAdvertisingDomains
            val cleanMessage = message.lowercase()
            val isAllowed = allowed.any { domain ->
                domain.isNotBlank() && cleanMessage.contains(domain.lowercase().trim())
            }
            if (!isAllowed) {
                return "werbung"
            }
        }

        // 2. Check restliche Kategorien
        for ((category, regex) in patterns) {
            if (category == "werbung") continue
            if (regex.containsMatchIn(message)) {
                return category
            }
        }
        return null
    }

    private fun checkAiApi(player: Player, message: String, apiKey: String) {
        try {
            val apiUrl = configService.automod.apiUrl.ifBlank { "https://api.openai.com/v1/chat/completions" }
            val model = configService.automod.model.ifBlank { "gpt-4o-mini" }
            val safeMsg = JsonUtil.escape(message)

            val jsonPayload = """
                {
                  "model": "$model",
                  "messages": [
                    {
                      "role": "system",
                      "content": "Klassifiziere die folgende Minecraft-Chatnachricht in genau EINES dieser Wörter falls es ein klarer Regelverstoß ist: [beleidigung, provokation, rassismus, antisemitismus, nationalsozialismus, nsfw, werbung, sexistisches_verhalten, server_hetze]. Wenn kein Regelverstoß vorliegt, antworte nur mit: sauber. Antworte mit exakt einem Wort im Kleinbuchstaben."
                    },
                    {
                      "role": "user",
                      "content": "$safeMsg"
                    }
                  ],
                  "temperature": 0.0,
                  "max_tokens": 10
                }
            """.trimIndent()

            val request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $apiKey")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val body = response.body().lowercase()
                val category = when {
                    body.contains("nationalsozialismus") -> "nationalsozialismus"
                    body.contains("rassismus") -> "rassismus"
                    body.contains("antisemitismus") -> "antisemitismus"
                    body.contains("beleidigung") -> "beleidigung"
                    body.contains("sexistisches_verhalten") -> "sexistisches_verhalten"
                    body.contains("server_hetze") -> "server_hetze"
                    body.contains("nsfw") -> "nsfw"
                    body.contains("werbung") -> "werbung"
                    body.contains("provokation") -> "provokation"
                    else -> null
                }
                if (category != null) {
                    executeAutoPunish(player, category, message)
                }
            }
        } catch (e: Exception) {
            logger.debug("[AiModeration] Fehler bei API-Abfrage: ${e.message}")
        }
    }

    private fun checkAndHandleSpam(event: PlayerChatEvent, player: Player, message: String): Boolean {
        val now = System.currentTimeMillis()
        val cleanMsg = message.trim().lowercase()
        val cooldownMs = configService.automod.spamCooldownMinutes * 60_000L
        val tracker = spamHistory[player.uniqueId]

        if (tracker == null || now - tracker.firstTimestamp > cooldownMs || tracker.text != cleanMsg) {
            spamHistory[player.uniqueId] = SpamTracker(cleanMsg, 1, now)
            return false
        }

        tracker.count++
        val maxDuplicates = configService.automod.spamMaxDuplicates

        if (tracker.count > maxDuplicates) {
            event.result = PlayerChatEvent.ChatResult.message(" *** ")
            player.sendMessage(ColorParser.parse(configService.automod.spamPlayerMessage))

            val staffMsg = ColorParser.parse(configService.automod.spamStaffMessage.replace("%player%", player.username))
            proxy.allPlayers.filter { it.hasPermission("bamelitebans.notify.automod") || it.hasPermission("bamelitebans.command.reload") }
                .forEach { it.sendMessage(staffMsg) }
            proxy.consoleCommandSource.sendMessage(staffMsg)

            val muteAfter = configService.automod.spamMuteAfterAttempts
            if (muteAfter > 0 && tracker.count >= muteAfter) {
                executeAutoPunish(player, "spam", message)
            }
            return true
        }
        return false
    }

    private fun executeAutoPunish(player: Player, category: String, message: String) {
        if (configService.automod.isMuteCategory(category) && !configService.automod.areMutesEnabled) {
            player.sendMessage(ColorParser.parse(configService.automod.mutesBlockedMessage))
            return
        }

        val rawCommand = configService.automod.getCommand(category) ?: return
        val commandToRun = rawCommand
            .replace("%player%", player.username)
            .replace("%message%", sanitizeForCommand(message))
            .removePrefix("/")
            .trim()
            
        if (commandToRun.isEmpty()) return

        logger.info("[AiModeration] Führe Auto-Punish aus: /$commandToRun (Kategorie: $category, Spieler: ${player.username})")

        proxy.commandManager.executeAsync(proxy.consoleCommandSource, commandToRun).thenAccept {
            if (configService.automod.notifyTeam) {
                val notifyMsg = configService.automod.notifyMessage
                    .replace("%player%", player.username)
                    .replace("%category%", category.uppercase())
                    .replace("%message%", ColorParser.escape(message))

                val parsed = ColorParser.parse(notifyMsg)
                proxy.allPlayers.filter { it.hasPermission("bamelitebans.notify.automod") || it.hasPermission("bamelitebans.command.reload") }
                    .forEach { it.sendMessage(parsed) }
                proxy.consoleCommandSource.sendMessage(parsed)
            }
        }
    }

    private fun sanitizeForCommand(raw: String): String {
        return raw
            .replace(Regex("[\\r\\n]"), " ")
            .replace(Regex("[\"'`;|&$]"), "")
            .take(100)
            .trim()
    }
}

